package org.alc.blackjack.model

interface Table {
    val minBet: Double
    val maxBet: Double
    val rule: TableRule
    val nbDecks: Int
    fun createAccount(initialAmount: Double): Account
    fun addPlayer(player: Player): Boolean
    fun removePlayer(player: Player): Boolean
    fun nbPlayers(): Int
    fun newRound(): Boolean

}
