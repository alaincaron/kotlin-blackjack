package org.alc.blackjack.impl

import org.alc.blackjack.model.*
import org.alc.card.model.Card

abstract class DefaultStrategy(account: Account, gainFactor: Double? =null):AbstractStrategy(account) {
    private val initialBalance: Double = account.balance()
    private val upperBound: Double = (1 + (gainFactor ?: 1.0)) * initialBalance

    override fun insurance(hand: Hand) = false
    override fun equalPayment() = table().rule.blackjackPayFactor < 1.5
    override fun initialBet(): Int {
        val minBet = table().minBet
        val balance = account.balance()
        return if (balance >= upperBound || balance < minBet) 0 else minBet
    }



    override fun nextMove(hand: Hand, dealerCard: Card): Decision {
        return when {
            shouldSplit(hand, dealerCard) -> Decision.SPLIT
            hand.isSoft() -> softDecision(hand, dealerCard)
            else -> hardDecision(hand, dealerCard)
        }
    }

    protected abstract fun hardDecision(hand: Hand, dealerCard: Card): Decision
    protected abstract fun softDecision(hand: Hand, dealerCard: Card): Decision
    protected abstract fun shouldSplit(hand: Hand, dealerCard: Card): Boolean
}
