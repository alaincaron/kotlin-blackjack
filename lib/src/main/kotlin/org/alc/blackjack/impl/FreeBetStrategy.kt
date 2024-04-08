package org.alc.blackjack.impl

import org.alc.blackjack.model.Account
import org.alc.blackjack.model.Decision
import org.alc.blackjack.model.Hand
import org.alc.blackjack.model.Table
import org.alc.card.model.Card

class FreeBetStrategy(account: Account, gainFactor: Double? = null) : DefaultStrategy(account, gainFactor) {

    override fun hardDecision(hand: Hand, dealerCard: Card, table: Table) =
        if (hand.isFree) hardDecisionFreeHand(hand, dealerCard, table) else hardDecisionPayingHand(hand, dealerCard, table)

    override fun softDecision(hand: Hand, dealerCard: Card, table: Table) =
        if (hand.isFree) softDecisionFreeHand(hand, dealerCard, table) else softDecisionPayingHand(hand, dealerCard, table)

    override fun shouldSplit(hand: Hand, dealerCard: Card, table: Table) = hand.canBeFreelySplit(table.rule) && hand.score() != 10

    private fun hardDecisionPayingHand(hand: Hand, dealerCard: Card, table: Table) = when (hand.score()) {
        in 4..8 -> Decision.HIT
        in 9..11 -> if (hand.canBeDoubled(table.rule)) Decision.DOUBLE else Decision.HIT
        12 -> if (dealerCard.value in 5..6) Decision.STAND else Decision.HIT
        13 -> if (dealerCard.value in 3..6) Decision.STAND else Decision.HIT
        14 -> if (dealerCard.value in 2..6) Decision.STAND else Decision.HIT
        15 -> when (dealerCard.value) {
            in 2..6 -> Decision.STAND
            in 7..9 -> Decision.HIT
            else -> if (table.rule.allowSurrender) Decision.SURRENDER else Decision.HIT
        }

        16 -> when (dealerCard.value) {
            in 2..6 -> Decision.STAND
            in 7..8 -> Decision.HIT
            else -> if (table.rule.allowSurrender) Decision.SURRENDER else Decision.HIT
        }

        17 -> if (dealerCard.value == 1 && table.rule.allowSurrender) Decision.SURRENDER else Decision.STAND
        in 18..21 -> Decision.STAND
        else -> throw IllegalStateException("Unexpected hand :${hand}")
    }

    private fun hardDecisionFreeHand(hand: Hand, dealerCard: Card, table: Table) = when (hand.score()) {
        in 4..8 -> Decision.HIT
        in 9..11 -> if (hand.canBeDoubled(table.rule)) Decision.DOUBLE else Decision.HIT
        12 -> if (dealerCard.value in 5..6) Decision.STAND else Decision.HIT
        13, 14 -> if (dealerCard.value in 3..6) Decision.STAND else Decision.HIT
        15, 16 -> if (dealerCard.value in 2..6) Decision.STAND else Decision.HIT
        17 -> if (dealerCard.value in 2..6 || dealerCard.value == 10) Decision.STAND else Decision.HIT
        in 18..21 -> Decision.STAND
        else -> throw IllegalStateException("Unexpected hand :${hand}")
    }

    private fun doubleIfEnoughFund(hand: Hand, table: Table, fallbackDecision: Decision) =
        if (hand.canBeDoubled(table.rule) &&
            hand.initialBet <= account.balance()
            ) Decision.DOUBLE else fallbackDecision

    private fun softDecisionFreeHand(hand: Hand, dealerCard: Card, table: Table) = when (hand.score()) {
        in 12..15 -> Decision.HIT
        16 -> if (dealerCard.value == 6) doubleIfEnoughFund(hand, table, Decision.HIT) else Decision.HIT
        17 -> if (dealerCard.value in 5..6) doubleIfEnoughFund(hand, table, Decision.HIT) else Decision.HIT
        18 -> when (dealerCard.value) {
            in 4..6 -> doubleIfEnoughFund(hand, table, Decision.STAND)
            7 -> Decision.STAND
            else -> doubleIfEnoughFund(hand, table, Decision.HIT)
        }
        19 -> if (dealerCard.value in 5..6) doubleIfEnoughFund(hand, table, Decision.STAND) else Decision.STAND
        20 -> if (dealerCard.value == 6) doubleIfEnoughFund(hand, table, Decision.STAND) else Decision.STAND
        21 -> Decision.STAND
        else -> throw IllegalStateException("Unexpected hand :${hand}")
    }

    private fun softDecisionPayingHand(hand: Hand, dealerCard: Card, table: Table) = when (hand.score()) {
        in 12..15 -> Decision.HIT
        16 -> if (dealerCard.value == 6) doubleIfEnoughFund(hand, table, Decision.HIT) else Decision.HIT
        17 -> if (dealerCard.value in 5..6) doubleIfEnoughFund(hand, table, Decision.STAND) else Decision.HIT
        18 -> when (dealerCard.value) {
            in 2..4 -> Decision.STAND
            in 5..6 -> doubleIfEnoughFund(hand, table, Decision.STAND)
            in 7..8 -> Decision.STAND
            else -> Decision.HIT
        }
        in 19..21 -> Decision.STAND
        else -> throw IllegalStateException("Unexpected hand :${hand}")

    }

}
