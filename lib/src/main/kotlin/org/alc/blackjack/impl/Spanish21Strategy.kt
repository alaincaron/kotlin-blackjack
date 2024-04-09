package org.alc.blackjack.impl

import org.alc.blackjack.model.Account
import org.alc.blackjack.model.Decision
import org.alc.blackjack.model.Hand
import org.alc.card.model.Card
import org.alc.card.model.Suit

class Spanish21Strategy(account: Account, gainFactor: Double? = null) : DefaultStrategy(account, gainFactor) {

    enum class Sequence21 { SUITED, ANY, SPADES }

    override fun insurance(hand: Hand): Boolean {
        return false
    }

    override fun equalPayment(): Boolean {
        return false
    }

    override fun nextMove(hand: Hand, dealerCard: Card) =
        if (!hand.canBeHit()) {
            when (hand.score()) {
                in 12..16 -> if (dealerCard.value >= 8) Decision.SURRENDER else Decision.STAND
                17 -> if (dealerCard.value == 11) Decision.SURRENDER else Decision.STAND
                else -> Decision.STAND
            }
        } else super.nextMove(hand, dealerCard)

    override fun hardDecision(hand: Hand, dealerCard: Card) =
        if (table().rule.dealerHitsOnSoft17) hardDecisionDealerHitOnSoft17(
            hand,
            dealerCard
        ) else hardDecisionDealerStandOnSoft17(hand, dealerCard)

    override fun softDecision(hand: Hand, dealerCard: Card) =
        if (table().rule.dealerHitsOnSoft17) softDecisionDealerHitOnSoft17(
            hand,
            dealerCard,
        ) else softDecisionDealerStandOnSoft17(hand, dealerCard)

    override fun shouldSplit(hand: Hand, dealerCard: Card) =
        hand.canBeSplit() && account.balance() >= hand.initialBet && (if (table().rule.dealerHitsOnSoft17)
            shouldSplitDealerHitOnSoft17(hand, dealerCard) else
            shouldSplitDealerStandOnSoft17(hand, dealerCard)
                )

    private fun doubleOrHit(hand: Hand, threshold: Int = Int.MAX_VALUE): Decision =
        if (hand.nbCards() >= threshold || account.balance() < hand.initialBet || !hand.canBeDoubled(table().rule)) Decision.HIT
        else Decision.DOUBLE

    private fun standOrHit(hand: Hand, threshold: Int, sequence: Sequence21? = null): Decision {
        if (hand.nbCards() >= threshold) return Decision.HIT
        if (sequence == null || hand.nbCards() != 2) return Decision.STAND
        val values = 6..8
        val v1 = hand[0].value
        val v2 = hand[1].value
        if (v1 !in values || v2 !in values || v1 == v2) return Decision.STAND
        val s1 = hand[0].suit
        val s2 = hand[1].suit
        if (s1 != s2) {
            return if (sequence == Sequence21.ANY) Decision.HIT else Decision.STAND
        }
        if (sequence == Sequence21.SPADES) {
            return if (s1 == Suit.SPADES) Decision.HIT else Decision.STAND
        }
        // cards are suited. Meeting Suited requirements
        return Decision.HIT
    }


    // Dealer stands on Soft 17
    private fun hardDecisionDealerStandOnSoft17(hand: Hand, dealerCard: Card) = when (hand.score()) {
        in 4..8 -> Decision.HIT
        9 -> if (dealerCard.value == 6) doubleOrHit(hand, 4) else Decision.HIT
        10 -> when (dealerCard.value) {
            2, 3 -> doubleOrHit(hand, 5)
            4, 5, 6 -> doubleOrHit(hand)
            7 -> doubleOrHit(hand, 4)
            8 -> doubleOrHit(hand, 3)
            else -> Decision.HIT
        }

        11 -> when (dealerCard.value) {
            2, 7, 8, 9 -> doubleOrHit(hand, 4)
            3, 4, 5, 6 -> doubleOrHit(hand, 5)
            else -> doubleOrHit(hand, 3)
        }

        12, 13 -> Decision.HIT
        14 -> when (dealerCard.value) {
            4, 6 -> standOrHit(hand, 4, Sequence21.ANY)
            5 -> standOrHit(hand, 5, Sequence21.ANY)
            else -> Decision.HIT
        }

        15 -> when (dealerCard.value) {
            2 -> standOrHit(hand, 4, Sequence21.ANY)
            3 -> standOrHit(hand, 5, Sequence21.ANY)
            4 -> standOrHit(hand, 5, Sequence21.SPADES)
            5 -> standOrHit(hand, 6)
            6 -> standOrHit(hand, 6, Sequence21.SPADES)
            else -> Decision.HIT
        }

        16 -> when (dealerCard.value) {
            2 -> standOrHit(hand, 5)
            3, 4 -> standOrHit(hand, 6)
            5, 6 -> Decision.STAND
            else -> Decision.HIT
        }

        17 -> when (dealerCard.value) {
            in 2..7 -> Decision.STAND
            8, 9, 10 -> standOrHit(hand, 6)
            else -> if (hand.canSurrender() && table().rule.allowSurrender) Decision.SURRENDER else Decision.HIT
        }

        else -> Decision.STAND
    }

    private fun softDecisionDealerStandOnSoft17(hand: Hand, dealerCard: Card) = when (hand.score()) {
        12, 13, 14, 15 -> Decision.HIT
        16 -> if (dealerCard.value == 6) doubleOrHit(hand, 4) else Decision.HIT
        17 -> when (dealerCard.value) {
            4, 5, 6 -> doubleOrHit(hand, dealerCard.value - 1)
            else -> Decision.HIT
        }

        18 -> when (dealerCard.value) {
            2, 3 -> standOrHit(hand, 4)
            4 -> doubleOrHit(hand, 4)
            5, 6 -> doubleOrHit(hand, 5)
            7 -> standOrHit(hand, 6)
            8 -> standOrHit(hand, 4)
            else -> Decision.HIT
        }

        else -> if (dealerCard.value == 10) standOrHit(hand, 6) else Decision.STAND
    }

    // Only difference is with Hit On Soft17 is that eights are always split (even against ace)
    private fun shouldSplitDealerStandOnSoft17(hand: Hand, dealerCard: Card) =
        hand[0].value == 8 || shouldSplitDealerHitOnSoft17(hand, dealerCard)


    // Dealer hits on Soft 17
    private fun hardDecisionDealerHitOnSoft17(hand: Hand, dealerCard: Card) = when (hand.score()) {
        in 4..8 -> Decision.HIT
        9 -> if (dealerCard.value == 6) doubleOrHit(hand) else Decision.HIT
        10 -> when (dealerCard.value) {
            2, 3 -> doubleOrHit(hand, 5)
            4, 5, 6 -> doubleOrHit(hand)
            7 -> doubleOrHit(hand, 4)
            8 -> doubleOrHit(hand, 3)
            else -> Decision.HIT
        }

        11 -> when (dealerCard.value) {
            2, 7, 8, 9 -> doubleOrHit(hand, 4)
            3, 4, 5, 6 -> doubleOrHit(hand, 5)
            else -> doubleOrHit(hand, 3)
        }

        12 -> Decision.HIT
        13 -> if (dealerCard.value == 6) standOrHit(hand, 4, Sequence21.ANY) else Decision.HIT
        14 -> when (dealerCard.value) {
            4 -> standOrHit(hand, 4, Sequence21.ANY)
            5 -> standOrHit(hand, 5, Sequence21.SUITED)
            6 -> standOrHit(hand, 6, Sequence21.SPADES)
            else -> Decision.HIT
        }

        15 -> when (dealerCard.value) {
            2 -> standOrHit(hand, 4, Sequence21.ANY)
            3 -> standOrHit(hand, 5, Sequence21.SUITED)
            4, 5 -> standOrHit(hand, 6)
            7 -> Decision.STAND
            else -> Decision.HIT
        }

        16 -> when (dealerCard.value) {
            2, 3, 4 -> standOrHit(hand, 6)
            5, 6 -> Decision.STAND
            7, 8, 9, 10 -> Decision.HIT
            else -> if (hand.canSurrender() && table().rule.allowSurrender) Decision.SURRENDER else Decision.HIT
        }

        17 -> when (dealerCard.value) {
            in 2..7 -> Decision.STAND
            8, 9, 10 -> standOrHit(hand, 6)
            else -> if (hand.canSurrender() && table().rule.allowSurrender) Decision.SURRENDER else Decision.HIT
        }

        else -> Decision.STAND
    }

    private fun softDecisionDealerHitOnSoft17(hand: Hand, dealerCard: Card) = when (hand.score()) {
        12, 13, 14 -> Decision.HIT
        15 -> if (dealerCard.value == 6) doubleOrHit(hand, 4) else Decision.HIT
        16 -> when (dealerCard.value) {
            5, 6 -> doubleOrHit(hand, dealerCard.value - 2)
            else -> Decision.HIT
        }

        17 -> when (dealerCard.value) {
            4, 5, 6 -> doubleOrHit(hand, dealerCard.value - 1)
            else -> Decision.HIT
        }

        18 -> when (dealerCard.value) {
            2, 3, 8 -> standOrHit(hand, 4)
            4, 5, 6 -> doubleOrHit(hand, dealerCard.value)
            7 -> standOrHit(hand, 6)
            else -> Decision.HIT
        }

        19 -> when (dealerCard.value) {
            10, 11 -> standOrHit(hand, 6)
            else -> Decision.STAND
        }

        else -> Decision.STAND
    }

    private fun shouldSplitDealerHitOnSoft17(hand: Hand, dealerCard: Card) = when (hand[0].value) {
        2, 3 -> dealerCard.value in 2..8
        4, 5, 10 -> false
        6 -> dealerCard.value in 4..6
        7 -> when (dealerCard.value) {
            in 2..6 -> true
            7 -> hand[0].suit != hand[1].suit
            else -> false
        }

        8 -> dealerCard.value != 11
        9 -> when (dealerCard.value) {
            2, 7, 10, 11 -> false
            else -> true
        }

        11 -> true
        else -> throw IllegalStateException("Invalid card value: ${hand[0]}")
    }
}




