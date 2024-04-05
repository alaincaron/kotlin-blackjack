package org.alc.blackjack.impl

import org.alc.blackjack.model.Hand
import org.alc.blackjack.model.TableRule
import org.alc.card.model.Card
import org.alc.card.model.Rank
import org.alc.card.model.Suit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandImplTest {

    private fun createHand(
        canBeSplit: Boolean = true,
        canBeHit: Boolean = true,
        isFromSplit: Boolean = false
    ) =
        HandImpl(
            initialBet = 1.0,
            canBeSplit = canBeSplit,
            canBeHit = canBeHit,
            isFromSplit = isFromSplit
        )

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun `should respect constructor canBeHit flag`() {
        val hand = createHand(canBeHit = false)
        hand.addCard(Card(Rank.FOUR, Suit.CLUBS))
        hand.addCard(Card(Rank.SEVEN, Suit.SPADES))
        assert(!hand.canBeDoubled(TableRule.DEFAULT))
    }

    @Test
    fun `should allow doubling when allowed`() {
        val hand = createHand()
        hand.addCard(Card(Rank.FOUR, Suit.CLUBS))
        hand.addCard(Card(Rank.SEVEN, Suit.SPADES))
        assert(hand.canBeDoubled(TableRule.DEFAULT))
    }

    @Test
    fun `should disallow doubling when more than 2 cards with default rules`() {
        val hand = createHand()
        hand.addCard(Card(Rank.FOUR, Suit.CLUBS))
        hand.addCard(Card(Rank.TWO, Suit.SPADES))
        hand.addCard(Card(Rank.FIVE, Suit.HEARTS))
        assert(!hand.canBeDoubled(TableRule.DEFAULT))
    }

    @Test
    fun `should allow doubling when more than 2 cards when rule allows it`() {
        val hand = createHand()
        hand.addCard(Card(Rank.FOUR, Suit.CLUBS))
        hand.addCard(Card(Rank.TWO, Suit.SPADES))
        hand.addCard(Card(Rank.FIVE, Suit.HEARTS))
        assert(hand.canBeDoubled(TableRule.DEFAULT.copy(allowDoubleAnytime = true)))
    }

    @Test
    fun `should respect constructor canBeSplit flag`() {
        val hand = createHand(canBeSplit = false)
        hand.addCard(Card(Rank.FOUR, Suit.CLUBS))
        hand.addCard(Card(Rank.FOUR, Suit.SPADES))
        assert(!hand.canBeSplit())
    }

    @Test
    fun `should allow splitting ACES`() {
        val hand = createHand()
        hand.addCard(Card(Rank.ACE, Suit.CLUBS))
        hand.addCard(Card(Rank.ACE, Suit.SPADES))
        assert(hand.canBeSplit())
    }

    @Test
    fun `should allow splitting pairs`() {
        val hand = createHand()
        hand.addCard(Card(Rank.FOUR, Suit.CLUBS))
        hand.addCard(Card(Rank.FOUR, Suit.SPADES))
        assert(hand.canBeSplit())
    }

    @Test
    fun `should allow splitting figures`() {
        val hand = createHand()
        hand.addCard(Card(Rank.KING, Suit.CLUBS))
        hand.addCard(Card(Rank.JACK, Suit.SPADES))
        assert(hand.canBeSplit())
    }

    @Test
    fun `should disallow splitting when not a pair`() {
        val hand = createHand()
        hand.addCard(Card(Rank.KING, Suit.CLUBS))
        hand.addCard(Card(Rank.FIVE, Suit.SPADES))
        assert(!hand.canBeSplit())
    }

    @Test
    fun `should disallow splitting when more than 2 cards`() {
        val hand = createHand()
        hand.addCard(Card(Rank.FOUR, Suit.CLUBS))
        hand.addCard(Card(Rank.FOUR, Suit.SPADES))
        hand.addCard(Card(Rank.FOUR, Suit.HEARTS))
        assert(!hand.canBeSplit())
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
                val total = Hand.value(c1) + Hand.value(c2)
                val canBeFreelyDoubled = !h.isSoft() && (total in 9..11)
                assert(h.canBeFreelyDoubled(rule) == canBeFreelyDoubled) { "canBeFreelyDoubled failed for $r1 and $r2" }
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
                val splittable = Hand.value(c1) == Hand.value(c2) && Hand.value(c1) != 10
                assert(h.canBeFreelySplit(rule) == splittable) { "isFreeSplittable failed for $r1 and $r2" }
            }
        }
    }
}

