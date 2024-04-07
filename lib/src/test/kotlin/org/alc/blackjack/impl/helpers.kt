package org.alc.blackjack.impl

import io.mockk.Matcher
import io.mockk.MockKMatcherScope
import org.alc.blackjack.model.Hand
import org.alc.card.model.Card


data class HandMatch(
    val expected: List<Card>,
) : Matcher<Hand> {

    override fun match(arg: Hand?): Boolean {
        if (arg == null) return false
        if (arg.nbCards() != expected.size) return false
        for (i in 0..<arg.nbCards()) {
            if (arg.getCard(i) != arg[i]) return false
        }
        return true
    }

    override fun toString() = "HandMatch($expected)"

    @Suppress("UNCHECKED_CAST")
    override fun substitute(map: Map<Any, Any>): Matcher<Hand> {
        return copy(expected = expected.map { map.getOrDefault(it as Any?, it) } as List<Card>)
    }
}

fun MockKMatcherScope.handMatch(cards: List<Card>) = match(HandMatch(cards))
fun MockKMatcherScope.handMatch(vararg cards: Card) = match(HandMatch(listOf(*cards)))


class HandBuilder(
    var initialBet: Int,
    var isFromSplit: Boolean = false,
    var canBeHit: Boolean = true,
    var canBeSplit: Boolean = true,
    var isFree: Boolean = false
) {
    fun build(vararg cards: Card): Hand =
        HandImpl(initialBet, isFromSplit, canBeHit, canBeSplit, isFree).also { h -> cards.forEach { h.addCard(it) } }

}



