package org.alc.blackjack.model

import org.alc.card.model.Card

interface Strategy : Observer {
    val account: Account

    fun enteredTable(table: Table)
    fun leftTable(table: Table)

    fun insurance(hand: Hand): Boolean
    fun equalPayment(): Boolean
    fun initialBet(): Int
    fun nextMove(hand: Hand, dealerCard: Card): Decision

    override fun cardDealt(card: Card)  {}
    override fun deckShuffled()  {}

    fun nbPush(): Int

    fun nbWin(): Int
    fun nbLoss(): Int
    fun avgWin(): Double
    fun avgLoss(): Double

    fun nbRounds() = nbPush() + nbLoss() + nbWin()

    fun printStats( out: (String)-> Unit) {
        out("balance = ${account.balance()}")
        val avgWinRound = nbWin().toDouble() / nbRounds()
        val avgLossRound = nbLoss().toDouble() / nbRounds()
        val avgPushRound = nbPush().toDouble() / nbRounds()
        out("nbRounds = ${nbRounds()}")
        out("avgWinRound = $avgWinRound")
        out("avgLostRound = $avgLossRound")
        out("avgPushRound = $avgPushRound")
        out("avgWin= ${avgWin()}")
        out("avgLoss = ${avgLoss()}")
    }

}
