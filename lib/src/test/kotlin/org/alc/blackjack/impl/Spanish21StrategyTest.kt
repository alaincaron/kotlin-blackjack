package org.alc.blackjack.impl

import io.mockk.every
import org.alc.blackjack.model.Decision
import org.alc.blackjack.model.TableRule
import org.alc.card.model.Card
import org.alc.card.model.Rank
import org.alc.card.model.Suit
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Spanish21StrategyTest : AbstractStrategyTestHelper() {

    override fun createStrategy(gainFactor: Double) = Spanish21Strategy(account, gainFactor)

    @Test
    fun `equalPayment should always be false`() {
        every { table.rule } returns TableRule.SPANISH21.copy(blackjackPayFactor = 1.0)
        initStrategy()
        assertFalse(strategy.equalPayment())
    }

    @Test
    fun `nextMove default rules never split tens and stand`() = neverSplitTensAndStand(TableRule.SPANISH21)

    @Test
    fun `nextMove default rules never split fives and double except against 9,10, and ACE`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.SPANISH21

        initStrategy()

        val hand = HandBuilder(initialBet = 1).build(Card.five.spades, Card.five.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            assertMove(if (dealerRank.value in 9..11 ) Decision.HIT else Decision.DOUBLE, hand, dealerCard)
        }
    }

    @Test
    fun `nextMove default Spanish21 rules always split eights but surrender on ACE`() =
        pairOfEights(Decision.SURRENDER, TableRule.SPANISH21)
    @Test

    fun `nextMove Spanish21-StandsOnSoft17 rules always split eights even on ACE`() =
        pairOfEights(Decision.SPLIT, TableRule.SPANISH21.copy(dealerHitsOnSoft17 = false))


    @Test
    fun `nextMove default rules always double eleven`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.SPANISH21

        initStrategy()

        val hand = HandBuilder(1).build(Card.eight.spades, Card.three.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            assertDouble(hand, dealerCard)
        }
    }

    @Test
    fun `nextMove can double with more than 2 cards`() {
        every { account.balance() } returns 1.0
        every { table.rule } returns TableRule.SPANISH21
        initStrategy()
        val hand = HandBuilder(1).build(Card.two.spades, Card.five.clubs, Card.four.hearts)
        for (dealerRank in Rank.entries) {
            assertMove(if (dealerRank.value >= 10) Decision.HIT else Decision.DOUBLE, hand, Card(dealerRank, Suit.HEARTS))
        }
    }

    @Test
    fun `nextMove hits instead of double when insufficient balance`() {
        every { account.balance() } returnsMany listOf(1.0, 0.0)
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.SPANISH21

        initStrategy()

        val hand = HandBuilder(1).build(Card.five.spades, Card.six.clubs)
        assertHit(hand, Card.six.diamonds)
        assertHit(hand, Card.ace.spades)
    }

    @Test
    fun `nextMove doubles soft-17 against 6 unless 5 cards or more`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.SPANISH21

        initStrategy()

        val hand1 = HandBuilder(initialBet = 1).build(Card.ace.diamonds, Card.two.diamonds, Card.ace.spades, Card.ace.clubs, Card.two.spades)
        assertHit(hand1, Card.six.clubs)
        val hand2 = HandBuilder(initialBet = 1).build(Card.ace.diamonds, Card.two.diamonds, Card.ace.spades, Card.three.clubs)
        assertDouble(hand2, Card.six.clubs)
    }

    @Test
    fun `nextMove doubles soft-18 against 5 unless 5 cards or more`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.SPANISH21

        initStrategy()

        val hand1 = HandBuilder(initialBet = 1).build(Card.ace.diamonds, Card.two.diamonds, Card.ace.spades, Card.two.clubs, Card.two.spades)
        assertHit(hand1, Card.five.clubs)
        val hand2 = HandBuilder(initialBet = 1).build(Card.ace.diamonds, Card.two.diamonds, Card.ace.spades, Card.four.clubs)
        assertDouble(hand2, Card.five.clubs)

    }


    @Test
    fun `nextMove stands on soft-20 against 6`() {
        every { account.balance() } returnsMany listOf(1.0, 0.0)
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.SPANISH21

        initStrategy()

        val hand = HandBuilder(initialBet = 1).build(Card.ace.diamonds, Card.nine.diamonds)
        assertStand(hand, Card.ace.clubs)
    }

}