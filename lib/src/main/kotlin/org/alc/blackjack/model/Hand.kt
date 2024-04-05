package org.alc.blackjack.model

import org.alc.card.model.Card

interface Hand {
    val isFromSplit: Boolean
    val canBeHit: Boolean
    fun addCard(c: Card)
    fun nbCards(): Int
    fun score(): Int
    operator fun get(i: Int) = getCard(i)
    fun isBlackJack(): Boolean = nbCards() == 2 && score() == 21 && !isFromSplit
    fun isBusted(): Boolean = score() > 21
    fun surrendered(): Boolean
    fun canBeSplit(): Boolean
    fun isSoft(): Boolean
    val initialBet: Double
    fun insurance(): Double
    fun totalBet(): Double
    fun canBeDoubled(rule: TableRule): Boolean
    fun getCard(idx: Int): Card
    fun canSurrender(): Boolean = nbCards() == 2 && !isFromSplit
    fun canBeFreelySplit(rule: TableRule) = rule.allowFreeSplit && value(getCard(0)) != 10 && canBeSplit()
    fun canBeFreelyDoubled(rule:TableRule) = rule.allowFreeDouble && canBeDoubled(rule) && !isSoft() && score() in 9..11

    companion object {
        fun value(c: Card): Int {
            val r = c.rank.ordinal
            return if (r in 0..8) r + 1 else 10
        }
    }
}
