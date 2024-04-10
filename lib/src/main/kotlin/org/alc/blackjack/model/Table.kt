package org.alc.blackjack.model

interface Table {
    val minBet: Int
    val maxBet: Int
    val rule: TableRule
    val nbDecks: Int
    fun createAccount(initialAmount: Double): Account
    fun addPlayer(player: Player): Boolean
    fun removePlayer(player: Player): Boolean
    fun nbPlayers(): Int
    fun newRound(): Boolean

    fun canSplit(account: Account, hand: Hand) =
        hand.canBeSplit() && (account.balance() >= hand.initialBet || rule.allowFreeSplit)

    fun canDouble(account: Account, hand: Hand) = hand.canBeDoubled(rule)
            && (account.balance() >= hand.initialBet || rule.allowFreeDouble)

    fun canSurrender(hand: Hand) = (rule.allowSurrender && hand.canSurrender()) ||
            (rule.allowDoubleRescue && hand.isDoubled())
}
