package org.alc.blackjack.impl

import io.mockk.*
import org.alc.blackjack.model.*
import org.alc.card.model.*


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

fun MockKMatcherScope.handMatch(items: List<Card>) = match(HandMatch(items))
fun MockKMatcherScope.handMatch(vararg items: Card) = match(HandMatch(listOf(*items)))


