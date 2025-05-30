package org.alc.blackjack.impl

import mu.*
import org.alc.blackjack.model.*
import org.alc.card.impl.*
import org.alc.card.model.*
import java.security.*
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

    private fun recordPush(player: Player, hand: Hand, dealerHand: Hand) {
        logger.info("Push")
        player.deposit(hand.netBet().toDouble())
        if (hand.insurance() > 0.0) {
            player.recordResult(Outcome.INSURANCE_LOSS, hand.insurance(), hand, dealerHand)
        } else {
            player.recordResult(Outcome.PUSH, 0.0, hand, dealerHand)
        }
    }

    private fun recordWin(player: Player, hand: Hand, amountWon: Double, outcome: Outcome, dealerHand: Hand?) {
        logger.info("Player won $amountWon")
        val netWon = amountWon - hand.insurance()
        player.deposit(hand.netBet() + amountWon)
        player.recordResult(outcome, netWon, hand, dealerHand)
    }

    private fun recordLoss(player: Player, hand: Hand, dealerHand: Hand?) {
        var outcome: Outcome
        var netloss: Double

        if (hand.surrendered())
            if (!hand.isDoubled()) {
                netloss = hand.initialBet / 2.0
                outcome = Outcome.DOUBLE_RESCUE
            } else {
                netloss = hand.initialBet.toDouble()
                outcome = Outcome.SURRENDER
            }
        else {
            netloss = hand.netBet() + hand.insurance()
            outcome = when {
                hand.isDoubled() -> Outcome.DOUBLE_LOSS
                dealerHand == null -> Outcome.BUST
                else -> Outcome.LOSS
            }
        }
        if (netloss > 0.0) {
            logger.info("Player lost $netloss")
        } else {
            logger.info("Player lost a FREE hand")
        }
        player.recordResult(outcome, netloss, hand, dealerHand)
    }

    private fun handleDealerBlackJack(dealerHand: Hand) {
        playerHands.forEach { (player, ph) ->
            player.finalDealerHand(dealerHand)
            ph.hands.forEach { hand ->
                player.finalHand(hand)
                if (hand.isBlackJack()) {
                    if (rule.alwaysPay21)
                        recordWin(
                            player,
                            hand,
                            hand.totalBet() * rule.blackjackPayFactor,
                            Outcome.BLACKJACK,
                            dealerHand
                        )
                    else if (!hand.equalPayment)
                        recordPush(player, hand, dealerHand)
                    else {
                        logger.info("Player accepted equal payment on Blackjack")
                        recordWin(player, hand, hand.totalBet().toDouble(), Outcome.BLACKJACK_EQUAL_PAYMENT, dealerHand)
                    }
                } else {
                    val insurance = hand.insurance()
                    if (insurance > 0.0) {
                        player.deposit(insurance)
                        hand.insure(0.0)
                        recordPush(player, hand, dealerHand)
                    } else {
                        recordLoss(player, hand, dealerHand)
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

        if (rule.allowDoubleRescue && hand.score() < 21) {
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

    private fun bonus21(hand: Hand, dealerUpCard: Card): Pair<Double, Outcome> {
        if (hand.isDoubled()) return Pair(hand.totalBet().toDouble(), Outcome.DOUBLE_WIN)
        return when (hand.nbCards()) {
            1 -> throw IllegalStateException("Can't have 21 with only one card")
            2, 4 -> Pair(hand.initialBet.toDouble(), Outcome.WIN)
            3 -> bonusSequence(hand, dealerUpCard)
            5 -> Pair(hand.initialBet * 1.5, Outcome.BONUS_21)
            6 -> Pair(hand.initialBet * 2.0, Outcome.BONUS_21_2)
            else -> Pair(hand.initialBet * 3.0, Outcome.BONUS_21_3)
        }
    }

    private fun suitedBonusFactor(suit: Suit) = if (suit == Suit.SPADES) 3.0 else 2.0

    private fun bonusSequence(hand: Hand, dealerUpCard: Card): Pair<Double, Outcome> {
        val suits = Array(3) { i -> hand[i].suit }
        var count = 0
        for (i in 0..<hand.nbCards()) {
            if (hand[i].value in 6..8) ++count else break
        }
        if (count == 3) {
            if (suits[0] == suits[1] && suits[1] == suits[2]) {
                if (dealerUpCard.value == 7 && hand[0].value == 7 && hand[1].value == 7) {
                    return Pair(
                        if (hand.initialBet >= 25) 5000.0 else 1000.0 + hand.initialBet * suitedBonusFactor(
                            suits[0]
                        ), Outcome.SUPER_7_BONUS
                    )

                }
                val factor = suitedBonusFactor(suits[0])
                return Pair(factor * hand.initialBet, if (factor == 3.0) Outcome.BONUS_21_3 else Outcome.BONUS_21_2)
            }
            return Pair(hand.initialBet * 1.5, Outcome.BONUS_21)
        }
        return Pair(hand.initialBet.toDouble(), Outcome.WIN)
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
                        recordWin(player, hand, hand.totalBet().toDouble(), Outcome.BLACKJACK_EQUAL_PAYMENT, null)
                    } else {
                        logger.info("BlackJack pays ${rule.blackjackPayFactor} the bet")
                        recordWin(player, hand, hand.totalBet() * rule.blackjackPayFactor, Outcome.BLACKJACK, null)
                    }
                    hands.removeAt(i)
                } else {
                    playHand(player, ph, hand, i, dealerUpCard)
                    val h = hands[i]
                    player.finalHand(h)
                    if (h.score() == 21 && rule.alwaysPay21) {
                        val bonus = bonus21(h, dealerUpCard)
                        recordWin(player, h, bonus.first, bonus.second, null)
                        hands.removeAt(i)
                    } else if (h.isBusted()) {
                        recordLoss(player, h, null)
                        hands.removeAt(i)
                    } else if (h.surrendered()) {
                        if (!hand.isDoubled()) {
                            player.deposit(hand.totalBet() / 2.0)
                            recordLoss(player, h, null)
                        } else {
                            player.deposit((hand.totalBet() - hand.initialBet).toDouble())
                            recordLoss(player, h, null)
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
                val totalBet = hand.totalBet().toDouble()
                val score = hand.score()
                when {
                    dealerScore > 22 || (dealerScore == 22 && !rule.pushOn22) -> recordWin(
                        player,
                        hand,
                        totalBet,
                        Outcome.DEALER_BUST,
                        dealerHand
                    )

                    score > dealerScore -> recordWin(
                        player,
                        hand,
                        totalBet,
                        if (hand.isDoubled() || hand.isFreeDoubled()) Outcome.DOUBLE_WIN else Outcome.WIN,
                        dealerHand
                    )

                    score == dealerScore || dealerScore == 22 -> recordPush(player, hand, dealerHand)
                    else -> recordLoss(player, hand, dealerHand)
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

