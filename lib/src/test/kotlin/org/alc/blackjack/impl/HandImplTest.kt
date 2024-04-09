package org.alc.blackjack.impl

import org.alc.blackjack.model.TableRule
import org.alc.card.model.Card
import org.alc.card.model.Rank
import org.alc.card.model.Suit
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HandImplTest {

    private fun createHand(
        canBeSplit: Boolean = true,
        canBeHit: Boolean = true,
        isFromSplit: Boolean = false,
        isFree: Boolean = false
    ) =
        HandImpl(
            initialBet = 1,
            canBeSplit = canBeSplit,
            canBeHit = canBeHit,
            isFromSplit = isFromSplit,
            isFree = isFree
        )

    @Test
    fun `should respect constructor canBeHit flag`() {
        val hand = createHand(canBeHit = false)
        hand.addCard(Card.four.clubs)
        hand.addCard(Card.seven.spades)
        assertFalse(hand.canBeDoubled(TableRule.DEFAULT))
    }

    @Test fun `should compute net bet on paying hand doubled for free`() {
        val hand = createHand(isFree = false)
        assertEquals(1, hand.initialBet)
        assertEquals(1, hand.totalBet())
        assertEquals(1, hand.netBet())

        hand.doubleBet(free = true)
        assertEquals(1, hand.initialBet)
        assertEquals(2, hand.totalBet())
        assertEquals(1, hand.netBet())
    }


    @Test fun `should compute net bet on free hand doubled for free`() {
        val hand = createHand(isFree = true)
        assertEquals(1, hand.initialBet)
        assertEquals(1, hand.totalBet())
        assertEquals(0, hand.netBet())

        hand.doubleBet(free = true)
        assertEquals(1, hand.initialBet)
        assertEquals(2, hand.totalBet())
        assertEquals(0, hand.netBet())
    }

    @Test fun `should compute net bet on paying hand doubled with own money`() {
        val hand = createHand(isFree = false)
        assertEquals(1, hand.initialBet)
        assertEquals(1, hand.totalBet())
        assertEquals(1, hand.netBet())

        hand.doubleBet(free = false)
        assertEquals(1, hand.initialBet)
        assertEquals(2, hand.totalBet())
        assertEquals(2, hand.netBet())
    }


    @Test fun `should compute net bet on free hand doubled with own money`() {
        val hand = createHand(isFree = true)
        assertEquals(1, hand.initialBet)
        assertEquals(0, hand.netBet())
        assertEquals(1, hand.totalBet())

        hand.doubleBet(free = false)
        assertEquals(1, hand.initialBet)
        assertEquals(2, hand.totalBet())
        assertEquals(1, hand.netBet())
    }

    @Test
    fun `should allow doubling when allowed`() {
        val hand = createHand()
        hand.addCard(Card.four.clubs)
        hand.addCard(Card.seven.spades)
        assertTrue(hand.canBeDoubled(TableRule.DEFAULT))
    }

    @Test
    fun `should disallow doubling when more than 2 cards with default rules`() {
        val hand = createHand()
        hand.addCard(Card.four.clubs)
        hand.addCard(Card.two.spades)
        hand.addCard(Card.five.hearts)
        assertFalse(hand.canBeDoubled(TableRule.DEFAULT))
    }

    @Test
    fun `should allow doubling when more than 2 cards when rule allows it`() {
        val hand = createHand()
        hand.addCard(Card.four.clubs)
        hand.addCard(Card.two.spades)
        hand.addCard(Card.five.hearts)
        assertTrue(hand.canBeDoubled(TableRule.DEFAULT.copy(allowDoubleAnytime = true)))
    }

    @Test
    fun `should respect constructor canBeSplit flag`() {
        val hand = createHand(canBeSplit = false)
        hand.addCard(Card.four.clubs)
        hand.addCard(Card.four.spades)
        assertFalse(hand.canBeSplit())
    }

    @Test
    fun `should allow splitting ACES`() {
        val hand = createHand()
        hand.addCard(Card.ace.clubs)
        hand.addCard(Card.ace.spades)
        assertTrue(hand.canBeSplit())
    }

    @Test
    fun `should allow splitting pairs`() {
        val hand = createHand()
        hand.addCard(Card.four.clubs)
        hand.addCard(Card.four.spades)
        assertTrue(hand.canBeSplit())
    }

    @Test
    fun `should allow splitting figures`() {
        val hand = createHand()
        hand.addCard(Card.king.clubs)
        hand.addCard(Card.jack.spades)
        assertTrue(hand.canBeSplit())
    }

    @Test
    fun `should disallow splitting when not a pair`() {
        val hand = createHand()
        hand.addCard(Card.king.clubs)
        hand.addCard(Card.five.spades)
        assertFalse(hand.canBeSplit())
    }

    @Test
    fun `should disallow splitting when more than 2 cards`() {
        val hand = createHand()
        hand.addCard(Card.four.clubs)
        hand.addCard(Card.four.spades)
        hand.addCard(Card.four.hearts)
        assertFalse(hand.canBeSplit())
    }

    @Test
    fun `should allow free doubling when total is hard 9, 10 or 11`() {
        val rule = TableRule.DEFAULT.copy(allowFreeDouble = true)
        Rank.entries.forEach { r1 ->
            Rank.entries.forEach { r2 ->
                val h = createHand()
                val c1 = Card(r1, Suit.CLUBS)
                val c2 = Card(r2, Suit.HEARTS)
                h.addCard(c1)
                h.addCard(c2)
                val total = c1.value + c2.value
                val isSoft = c1.value == 11 || c2.value == 11
                val canBeFreelyDoubled = !isSoft && (total in 9..11)
                assertTrue(h.canBeFreelyDoubled(rule) == canBeFreelyDoubled) { "canBeFreelyDoubled failed for $r1 and $r2" }
            }
        }
    }

    @Test
    fun `should allow free splitting when pair of non figures`() {
        val rule = TableRule.DEFAULT.copy(allowFreeSplit = true)
        Rank.entries.forEach { r1 ->
            Rank.entries.forEach { r2 ->
                val h = createHand()
                val c1 = Card(r1, Suit.CLUBS)
                val c2 = Card(r2, Suit.HEARTS)
                h.addCard(c1)
                h.addCard(c2)
                val value = c1.value
                val splittable = value != 10  && value == c2.value
                assertTrue(h.canBeFreelySplit(rule) == splittable) { "isFreeSplittable failed for $r1 and $r2" }
            }
        }
    }
}

