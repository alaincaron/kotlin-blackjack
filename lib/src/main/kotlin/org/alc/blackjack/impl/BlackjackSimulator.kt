package org.alc.blackjack.impl

import org.alc.blackjack.model.*

fun simulate(
    name: String,
    table: Table,
    factory: (account: Account) -> Strategy,
    loopCount: Int = 1000
) {

    var nbLosses = 0
    var nbWin = 0
    var gainSum = 0.0 // to compute avg gain of winning games
    var outcomeSum = 0.0 // to compute avg outcome of a game
    var nbRounds = 0

    val t1 = System.nanoTime()
    for (i in 1..loopCount) {
        val strategy = factory(table.createAccount(50.0))
        val player = Player(strategy)
        val initialBalance = player.balance()
        table.addPlayer(player)
        while (table.newRound()) {
        }
        nbRounds += player.nbRounds()
        outcomeSum += player.balance()
        if (player.balance() < table.minBet) {
            nbLosses += 1
        } else {
            nbWin += 1
            gainSum += (player.balance() - initialBalance)
        }
        table.removePlayer(player)
    }
    val duration: Double = (System.nanoTime() - t1) / 1e6
    val avgRound = nbRounds.toDouble() / loopCount
    val avgOutcome = outcomeSum / loopCount
    val avgGain = gainSum / nbWin
    val lossPercent = nbLosses.toDouble() / loopCount
    val winPercent = nbWin.toDouble() / loopCount
    val avgDuration = duration / loopCount
    println(
        "%-25s %10.2f %10.2f %10.2f %5.2f %5.2f %8.2fms".format(
            name,
            avgRound,
            avgOutcome,
            avgGain,
            lossPercent,
            winPercent,
            avgDuration
        )
    )
}

fun main() {
    var table = TableImpl(rule = TableRule.DEFAULT)
    simulate("basic.25", table, { a -> RegularStrategy(a, 0.25) })
    simulate("basic.50", table, { a -> RegularStrategy(a, 0.50) })
    simulate("basic.100", table, { a -> RegularStrategy(a, 1.00) })
    table = TableImpl(rule = TableRule.FREE_BET)
    simulate("freebet.25", table, { a -> FreeBetStrategy(a, 0.25) })
    simulate("freebet.50", table, { a -> FreeBetStrategy(a, 0.50) })
    simulate("freebet.100", table, { a -> FreeBetStrategy(a, 1.00) })
}

