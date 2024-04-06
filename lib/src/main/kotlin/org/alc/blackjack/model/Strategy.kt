package org.alc.blackjack.model

import org.alc.card.model.Card

interface Strategy {
    val account: Account

    fun enteredTable(table: Table)
    fun leftTable(table: Table)

    fun insurance(hand: Hand): Boolean
    fun equalPayment(): Boolean
    fun initialBet(): Double
    fun nextMove(hand: Hand, dealerCard: Card): Decision

    fun cardDealt(card: Card)  {}
    fun deckShuffled()  {}
    fun received(card: Card)
    fun dealerReceived(card: Card)
    fun dealerCardVisible(card: Card)
    fun finalHand(hand: Hand)
    fun finalDealerHand(hand: Hand)

    fun recordPush()
    fun nbPush(): Int

    fun recordWin(amount: Double)
    fun recordLoss(amount: Double)

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
