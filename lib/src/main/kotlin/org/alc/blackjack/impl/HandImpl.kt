package org.alc.blackjack.impl

import org.alc.blackjack.model.Hand
import org.alc.blackjack.model.TableRule
import org.alc.card.model.Card
import org.alc.card.model.Rank


internal class HandImpl(
    override val initialBet: Int,
    override val isFromSplit: Boolean = false,
    override val canBeHit: Boolean = true,
    private val canBeSplit: Boolean = true,
    override val isFree: Boolean = false
) : Hand {
    private var _score = 0
    private val cards = ArrayList<Card>()
    private var _soft = false
    private var _insurance = 0.0
    private var _totalBet = initialBet
    internal var equalPayment: Boolean = false
    private var _surrendered = false
    private var freeDouble: Boolean = false

    internal fun doubleBet(free: Boolean = false) {
        _totalBet *= 2
        if(free) freeDouble = true
    }

    internal fun surrender() {
        _surrendered = true
    }

    override fun getCard(idx: Int): Card = cards[idx]

    internal fun insure(premium: Double) {
        _insurance = premium
    }


    override fun canBeSplit() =
        canBeSplit && cards.size == 2 && cards[0].value == cards[1].value

    override fun canBeDoubled(rule: TableRule) = when (cards.size) {
        0, 1 -> false
        2 -> canBeHit()
        else -> rule.allowDoubleAnytime && canBeHit()
    }

    override fun addCard(c: Card) {
        cards.add(c)
        val value = c.value
        _score += if (value == 11) 1 else value
        if (value == 11 || _soft) {
            _soft = _score <= 11
        }
    }

    override fun nbCards() = cards.size

    override fun score() = if (_soft && _score <= 11) _score + 10 else _score
    override fun isFreeDoubled() = freeDouble
    override fun isSoft() = _soft
    override fun insurance() = _insurance
    override fun totalBet() = _totalBet
    override fun surrendered() = _surrendered

    override fun toString() =
         "HandImpl(initialBet=$initialBet, cards=$cards, isFromSplit=$isFromSplit, canBeHit=$canBeHit, canBeSplit=$canBeSplit, isFree=$isFree, _score=$_score, _soft=$_soft, _insurance=$_insurance, _totalBet=$_totalBet, equalPayment=$equalPayment, _surrendered=$_surrendered, freeDouble=$freeDouble)"

}






