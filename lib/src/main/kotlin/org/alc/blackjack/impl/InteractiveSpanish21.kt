package org.alc.blackjack.impl

import org.alc.blackjack.model.Player
import org.alc.blackjack.model.TableRule
import org.alc.card.impl.RandomShuffler
import org.alc.card.impl.Spanish21GameShoeImpl
import org.alc.card.model.Card
import java.security.SecureRandom

fun main() {
    val random = SecureRandom()
    val shuffler = RandomShuffler<Card>(random)
    val gameShoe = Spanish21GameShoeImpl(shuffler)
    val table = TableImpl(
        gameShoe = gameShoe,
        nbDecks = 8,
        random = random,
        minBet = 25,
        maxBet = 1000,
        rule = TableRule.SPANISH21
    )
    val account = table.createAccount(1000.0)
    val strategy = TrainingStrategy(Spanish21Strategy(account))
    val player = Player(strategy)
    table.addPlayer(player)
    while (table.newRound()) {
    }
    player.printStats { s: String -> println(s) }
}
