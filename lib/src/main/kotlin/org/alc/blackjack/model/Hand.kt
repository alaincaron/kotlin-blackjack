package org.alc.blackjack.model

import org.alc.card.model.*

interface Hand {
    val isFromSplit: Boolean
    val canBeHit: Boolean
    val isFree: Boolean
    fun addCard(c: Card)
    fun nbCards(): Int
    fun cards(): List<Card>
    fun score(): Int
    operator fun get(i: Int) = getCard(i)
    fun isBlackJack(): Boolean = nbCards() == 2 && score() == 21 && !isFromSplit
    fun isBusted(): Boolean = score() > 21
    fun surrendered(): Boolean
    fun canBeSplit(): Boolean
    fun isSoft(): Boolean
    val initialBet: Int
    fun insurance(): Double
    fun totalBet(): Int
    fun canBeDoubled(rule: TableRule): Boolean
    fun getCard(idx: Int): Card
    fun canSurrender(): Boolean = nbCards() == 2 && !isFromSplit
    fun canBeFreelySplit(rule: TableRule) = rule.allowFreeSplit && score() != 20 && canBeSplit()
    fun canBeFreelyDoubled(rule:TableRule) = rule.allowFreeDouble && canBeDoubled(rule) && !isSoft() && score() in 9..11
    fun isFreeDoubled(): Boolean
    fun isDoubled() = totalBet() > initialBet

    fun canBeHit() = canBeHit && !isDoubled()

    fun netBet(): Int {
        var netBet = if (isFree) 0 else initialBet
        if (!isFreeDoubled()) netBet += (totalBet() - initialBet)
        return netBet
    }
}
