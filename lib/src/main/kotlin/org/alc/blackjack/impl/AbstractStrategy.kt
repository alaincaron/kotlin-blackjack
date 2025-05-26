package org.alc.blackjack.impl

import org.alc.blackjack.model.*
import org.alc.card.model.Card
import kotlin.math.max

abstract class AbstractStrategy(final override val account: Account) : Strategy {
    private var _nbPush = 0
    private var _nbWin = 0
    private var _nbLoss = 0
    private var amountWon = 0.0
    private var amountLoss = 0.0

    private var _table: Table? = null

    override fun enteredTable(table: Table) {
        this._table = table
    }

    override fun leftTable(table: Table) {
        if (_table == table) {
            _table = null
        }
    }

    protected fun table() = _table ?: throw IllegalStateException("No table")
    protected fun tableNoThrow() = _table

    override fun recordPush() {
        ++_nbPush
    }

    override fun nbPush() = _nbPush

    override fun recordWin(amount: Double) {
        _nbWin += 1
        amountWon += amount
    }

    override fun recordLoss(amount: Double) {
        _nbLoss += 1
        amountLoss += amount
    }

    override fun nbWin() = _nbWin
    override fun nbLoss() = _nbLoss
    override fun avgWin() = amountWon / max(1, _nbWin)
    override fun avgLoss() = amountLoss / max(1, _nbLoss)
    override fun received(card: Card) {}
    override fun dealerReceived(card: Card) {}
    override fun dealerCardVisible(card: Card) {}
    override fun finalHand(hand: Hand) {}
    override fun finalDealerHand(hand: Hand) {}

    override fun recordResult(outcome: Outcome, amount: Double, playerHand: Hand, dealerHand: Hand?){
    }
}
