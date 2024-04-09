package org.alc.blackjack.impl

import mu.KotlinLogging
import org.alc.blackjack.model.*
import org.alc.card.impl.DefaultGameShoeImpl
import org.alc.card.impl.RandomShuffler
import org.alc.card.model.Card
import org.alc.card.model.GameShoe
import org.alc.card.model.Rank
import java.security.SecureRandom
import java.util.*

private val logger = KotlinLogging.logger {}

internal class PlayerHands {
    var totalHands: Int = 0
    val hands: ArrayList<HandImpl> = ArrayList()
}

class TableImpl(
    override val minBet: Int = 1,
    override val maxBet: Int = Int.MAX_VALUE,
    override val rule: TableRule = TableRule.DEFAULT,
    override val nbDecks: Int = 8,
    gameShoe: GameShoe? = null,
    random: Random? = null
) : Table {

    private val playerHands = LinkedHashMap<Player, PlayerHands>()
    private var lastCardMarker: Int = 0

    private var _random = random ?: SecureRandom()
    private var _gameShoe = gameShoe ?: DefaultGameShoeImpl(RandomShuffler(_random))

    private fun drawCard(visible: Boolean = true): Card {
        val card = _gameShoe.dealCard()!!
        if (visible) notifyCardDealt(card)
        return card
    }

    private fun getLastCardMarker(): Int {
        val nbRemainingCards = _gameShoe.nbRemainingCards()
        val min = nbRemainingCards / 4
        val max = nbRemainingCards / 3
        return _random.nextInt(max - min) + min
    }

    private fun initGameShoe() {
        _gameShoe.reset()
        _gameShoe.addDecks(nbDecks, shuffled = true)
        lastCardMarker = getLastCardMarker()
        // throw first card
        _gameShoe.dealCard()
        playerHands.keys.forEach { it.deckShuffled() }
    }

    override fun addPlayer(player: Player): Boolean {
        if (playerHands.contains(player)) {
            return false
        }
        playerHands[player] = PlayerHands()
        player.enteredTable(this)
        return true
    }

    override fun removePlayer(player: Player): Boolean {
        playerHands.remove(player) ?: return false
        player.leftTable(this)
        return true
    }

    override fun nbPlayers(): Int = playerHands.size

    private fun getInitialBet(player: Player): Int? {
        val maxRetry = 2
        var retry = 0
        while (retry <= maxRetry) {
            val initialBet = player.initialBet()
            if (initialBet <= 0) return null
            if (initialBet < minBet || initialBet > maxBet)
                logger.error("invalid bet: $initialBet, not between $minBet and $maxBet")
            else if (initialBet > player.balance())
                logger.error("Invalid bet: $initialBet is less than account balance: ${player.balance()}")
            else
                return initialBet
            retry += 1
        }
        logger.error("Too many invalid bets. Kicking player out of table")
        return null
    }

    private fun initPlayerHands() {
        playerHands.forEach { (player, ph) ->
            ph.hands.clear()
            when (val initialBet = getInitialBet(player)) {
                null -> logger.info("Player is leaving table")
                else -> {
                    player.withdraw(initialBet.toDouble())
                    val h = HandImpl(initialBet)
                    ph.totalHands = 1
                    ph.hands.add(h)
                }
            }
        }

        playerHands.filterValues { it.hands.isEmpty() }.keys.forEach { playerHands.remove(it) }

        if (playerHands.isEmpty()) {
            logger.info("No more players left. Wrapping up")
        }
    }

    private fun dealOneCardToEachPlayer() {
        playerHands.forEach { (player, ph) ->
            ph.hands.forEach { hand -> dealCardToPlayer(player, hand) }
        }
    }

    private fun dealCardToPlayer(player: Player, hand: HandImpl) {
        val card = drawCard()
        hand.addCard(card)
        player.received(card)
        logger.info("Player got $card")
    }

    private fun notifyCardDealt(card: Card) {
        playerHands.keys.forEach { it.cardDealt(card) }
    }

    private fun offerInsuranceOrEqualPayment() {
        playerHands.forEach { (player, ph) ->
            ph.hands.forEach { hand ->
                if (hand.isBlackJack()) {
                    if (!rule.alwaysPay21) hand.equalPayment = player.equalPayment()
                } else if (player.insurance(hand)) {
                    val insuranceAmount = hand.initialBet * 0.5
                    if (insuranceAmount > player.balance()) {
                        logger.info(
                            "Can't insure hand: required = $insuranceAmount, available = ${player.balance()}"
                        )
                    } else {
                        player.withdraw(insuranceAmount)
                        hand.insure(insuranceAmount)
                    }
                }
            }
        }
    }

    private fun recordPush(player: Player, hand: Hand) {
        logger.info("Push")
        player.deposit(hand.netBet().toDouble())
        if (hand.insurance() > 0.0)
            player.recordLoss(hand.insurance())
        else
            player.recordPush()
    }

    private fun recordWin(player: Player, hand: Hand, factor: Double) {
        val rawAmountWon = hand.totalBet() * factor
        logger.info("Player won $rawAmountWon")
        player.deposit(hand.netBet() + rawAmountWon)
        player.recordWin(rawAmountWon - hand.insurance())
    }

    private fun recordLoss(player: Player, hand: Hand) {
        val netLoss = if (hand.surrendered())
            if (hand.totalBet() == hand.initialBet)
                hand.initialBet / 2.0
            else
                hand.initialBet.toDouble()
        else
            hand.netBet() + hand.insurance()
        if (netLoss > 0.0) {
            logger.info("Player lost $netLoss")
        } else {
            logger.info("Player lost a FREE hand")
        }
        player.recordLoss(netLoss)
    }

    private fun handleDealerBlackJack(dealerHand: Hand) {
        playerHands.forEach { (player, ph) ->
            player.finalDealerHand(dealerHand)
            ph.hands.forEach { hand ->
                player.finalHand(hand)
                if (hand.isBlackJack()) {
                    if (rule.alwaysPay21)
                        recordWin(player, hand, rule.blackjackPayFactor)
                    else if (!hand.equalPayment)
                        recordPush(player, hand)
                    else
                        recordWin(player, hand, 1.0)
                } else {
                    val insurance = hand.insurance()
                    if (insurance > 0.0) {
                        player.deposit(insurance)
                        hand.insure(0.0)
                        recordPush(player, hand)
                    } else {
                        recordLoss(player, hand)
                    }
                }
            }
        }
    }

    private fun drawOneCardAndGetScore(hand: HandImpl, player: Player?): Int {
        val name = player?.let { "Player" } ?: "Dealer"
        val card = drawCard()
        logger.info("$name draw $card")
        hand.addCard(card)
        player?.received(card) ?: playerHands.keys.forEach { it.dealerReceived(card) }

        val score = hand.score()
        if (score > 21)
            logger.info("$name busted with score: $score")
        else
            logger.info("$name score is $score")
        return score
    }

    private fun playerStands(@Suppress("UNUSED_PARAMETER") player: Player, hand: Hand) {
        logger.info("Player stand with hand = ${hand.score()}")
    }

    private fun playerDoubles(player: Player, ph: PlayerHands, hand: HandImpl, pos: Int, dealerUpCard: Card) {
        logger.info("Player double for one card")
        if (hand.canBeFreelyDoubled(rule)) {
            hand.doubleBet(free = true)
        } else {
            player.withdraw(hand.initialBet.toDouble())
            hand.doubleBet()
        }
        drawOneCardAndGetScore(hand, player)
        logger.info("Player total is ${hand.score()}")

        if (rule.allowDoubleRescue && hand.score() <= 21) {
            playHand(player, ph, hand, pos, dealerUpCard)
        }
    }

    private fun playerSplits(player: Player, ph: PlayerHands, hand: HandImpl, pos: Int): HandImpl {
        logger.info("Player split on ${hand[0].value}")
        val isAce: Boolean = hand.getCard(0).rank == Rank.ACE
        val canBeSplit = ph.totalHands < rule.maxSplit - 1 && (!isAce || rule.allowMultipleSplitAces)
        val canBeHit = !isAce || rule.allowHitSplitAces
        val isFreeSplit = rule.allowFreeSplit && hand.score() != 20

        ph.hands.removeAt(pos)
        if (!isFreeSplit) player.withdraw(hand.initialBet.toDouble())
        val h1 = HandImpl(
            initialBet = hand.initialBet,
            canBeSplit = canBeSplit,
            canBeHit = canBeHit,
            isFromSplit = true,
            isFree = hand.isFree
        )
        val h2 = HandImpl(
            initialBet = hand.initialBet,
            canBeSplit = canBeSplit,
            canBeHit = canBeHit,
            isFromSplit = true,
            isFree = isFreeSplit
        )
        h1.addCard(hand.getCard(0))
        h2.addCard(hand.getCard(1))
        ph.hands.add(pos, h2)
        ph.hands.add(pos, h1)
        ph.totalHands += 1
        return h1
    }

    private tailrec fun getPlayerDecision(
        player: Player,
        hand: HandImpl,
        dealerUpCard: Card,
        retryCount: Int = 0
    ): Decision {
        val decision = player.nextMove(hand, dealerUpCard)
        when (decision) {
            Decision.STAND -> return Decision.STAND
            Decision.HIT -> if (hand.canBeHit()) return Decision.HIT
            Decision.SURRENDER -> if (canSurrender(hand)) return decision
            Decision.DOUBLE -> if (canDouble(player, hand)) return decision
            Decision.SPLIT -> if (canSplit(player, hand)) return decision
        }
        logger.warn("Illegal decision: $decision hand=$hand")
        if (retryCount >= 2) {
            logger.warn("Too many illegal decisions.  Reverting to STAND")
            return Decision.STAND
        }
        return getPlayerDecision(player, hand, dealerUpCard, retryCount + 1)
    }

    private tailrec fun playHand(player: Player, ph: PlayerHands, hand: HandImpl, pos: Int, dealerUpCard: Card) {
        if (hand.nbCards() < 2) {
            val score = drawOneCardAndGetScore(hand, player)
            if (score >= 21) return
        }
        if (!hand.canBeHit) return

        when (getPlayerDecision(player, hand, dealerUpCard)) {
            Decision.STAND -> playerStands(player, hand)
            Decision.DOUBLE -> playerDoubles(player, ph, hand, pos, dealerUpCard)

            Decision.SPLIT -> {
                val h = playerSplits(player, ph, hand, pos)
                playHand(player, ph, h, pos, dealerUpCard)
            }

            Decision.HIT -> {
                if (drawOneCardAndGetScore(hand, player) < 21)
                    playHand(player, ph, hand, pos, dealerUpCard)
            }

            Decision.SURRENDER -> {
                logger.info("Player surrender with score = ${hand.score()}")
                hand.surrender()
            }
        }
    }

    private fun playHands(dealerUpCard: Card) {
        playerHands.forEach { (player, ph) ->
            var i = 0
            val hands = ph.hands
            while (i < hands.size) {
                val hand = hands[i]
                if (hand.isBlackJack()) {
                    player.finalHand(hand)
                    if (hand.equalPayment) {
                        logger.info("Player accepted equal payment on Blackjack")
                        recordWin(player, hand, 1.0)
                    } else {
                        logger.info("BlackJack pays ${rule.blackjackPayFactor} the bet")
                        recordWin(player, hand, rule.blackjackPayFactor)
                    }
                    hands.removeAt(i)
                } else {
                    playHand(player, ph, hand, i, dealerUpCard)
                    val h = hands[i]
                    player.finalHand(h)
                    if (h.score() == 21 && rule.alwaysPay21) {
                        recordWin(player, h, 1.0)
                        hands.removeAt(i)
                    }
                    else if (h.isBusted()) {
                        recordLoss(player, h)
                        hands.removeAt(i)
                    } else if (h.surrendered()) {
                        if (hand.totalBet() == hand.initialBet) {
                            player.deposit(hand.totalBet() / 2.0)
                            recordLoss(player, h)
                        } else {
                            player.deposit((hand.totalBet() - hand.initialBet).toDouble())
                            recordLoss(player, h)
                        }
                        hands.removeAt(i)
                    } else {
                        i += 1
                    }
                }
            }
        }
    }

    private fun dealerPlay(hand: HandImpl) {
        var done = false
        while (!done) {
            val score = hand.score()
            if (score > 17) done = true
            else if (score < 17 || (rule.dealerHitsOnSoft17 && hand.isSoft())) drawOneCardAndGetScore(hand, null)
            else done = true
        }
    }

    private fun payWinningHands(dealerHand: Hand) {
        val dealerScore = dealerHand.score()
        playerHands.forEach { (player, ph) ->
            ph.hands.forEach { hand ->
                val score = hand.score()
                when {
                    dealerScore > 22 || (dealerScore == 22 && !rule.pushOn22) -> recordWin(player, hand, 1.0)
                    score > dealerScore -> recordWin(player, hand, 1.0)
                    score == dealerScore || dealerScore == 22 -> recordPush(player, hand)
                    else -> recordLoss(player, hand)
                }
            }
        }
    }

    private fun showHiddenCard(hiddenDealerCard: Card, visibleDealerCard: Card): HandImpl {
        logger.info("Dealer's hidden card is turned: $hiddenDealerCard")
        notifyCardDealt(hiddenDealerCard)
        playerHands.keys.forEach { it.dealerCardVisible(hiddenDealerCard) }

        val dealerHand = HandImpl(
            initialBet = 0,
            canBeSplit = false
        )

        dealerHand.addCard(visibleDealerCard)
        dealerHand.addCard(hiddenDealerCard)
        return dealerHand
    }

    override fun newRound(): Boolean {
        initPlayerHands()
        if (playerHands.isEmpty()) {
            return false
        }

        if (_gameShoe.nbRemainingCards() <= lastCardMarker) initGameShoe()
        dealOneCardToEachPlayer()
        val visibleDealerCard = drawCard()
        playerHands.keys.forEach { it.dealerReceived(visibleDealerCard) }
        logger.info("Dealer's visible card is $visibleDealerCard")
        dealOneCardToEachPlayer()
        val hiddenDealerCard = drawCard(visible = false)

        // handle potential dealer black jack
        if (visibleDealerCard.rank == Rank.ACE) {
            offerInsuranceOrEqualPayment()
            val hiddenCardValue = hiddenDealerCard.value
            if (hiddenCardValue == 10) {
                val dealerHand = showHiddenCard(hiddenDealerCard, visibleDealerCard)
                handleDealerBlackJack(dealerHand)
                return true
            }
        } else if (visibleDealerCard.value == 10 && hiddenDealerCard.value == 11) {
            val dealerHand = showHiddenCard(hiddenDealerCard, visibleDealerCard)
            handleDealerBlackJack(dealerHand)
            return true
        }

        playHands(visibleDealerCard)
        val dealerHand = showHiddenCard(hiddenDealerCard, visibleDealerCard)
        if (playerHands.values.any { it.hands.isNotEmpty() }) dealerPlay(dealerHand)
        playerHands.keys.forEach { it.finalDealerHand(dealerHand) }
        payWinningHands(dealerHand)
        return true
    }

    override fun createAccount(initialAmount: Double) = AccountImpl(initialAmount)
}

