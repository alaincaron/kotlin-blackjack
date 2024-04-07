package org.alc.blackjack.impl

import io.mockk.every
import org.alc.blackjack.model.TableRule
import org.alc.card.model.Card
import org.alc.card.model.Rank
import org.alc.card.model.Suit
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FreeBetStrategyTest : AbstractStrategyTestHelper() {

    protected override fun createStrategy(gainFactor: Double) = FreeBetStrategy(account, gainFactor)

    @Test
    fun `should always refuse insurance`() {
        initStrategy()
        // always reject insurance
        val hand = HandImpl(1)
        assertFalse(strategy.insurance(hand))
    }

    @Test
    fun `equalPayment should return true if blackjackPlayFactor is less than 50 percent`() {
        every { table.rule } returns TableRule.FREEBET.copy(blackjackPayFactor = 1.49)
        initStrategy()
        assertTrue(strategy.equalPayment())
    }

    @Test
    fun `equalPayment should return false if blackjackPlayFactor is at least 50 percent`() {
        every { table.rule } returns TableRule.FREEBET
        initStrategy()
        assertFalse(strategy.equalPayment())
    }

    @Test
    fun `initialBet should return the min if enough`() {
        every { account.balance() } returns 10.0
        every { table.minBet } returns 1

        initStrategy()

        assertEquals(1, strategy.initialBet())
    }

    @Test
    fun `initialBet should return the 0 if enough balance`() {
        every { account.balance() } returns 0.5
        every { table.minBet } returns 1

        initStrategy()

        assertEquals(0, strategy.initialBet())
    }

    @Test
    fun `nextMove default rules never split tens and stand`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREEBET

        initStrategy()

        val hand = HandBuilder(initialBet = 1).build(Card.ten.spades, Card.ten.spades)
        for (dealerCard in Rank.entries.map { Card(it, Suit.SPADES) }) {
            assertStand(hand, dealerCard)
        }
    }

    @Test
    fun `nextMove default rules never split fives and always double`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREEBET

        initStrategy()

        val hand = HandBuilder(initialBet = 1).build(Card.five.spades, Card.five.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            assertDouble(hand, dealerCard)
        }
    }

    @Test
    fun `nextMove default rules always split eights`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREEBET

        initStrategy()

        val hand = HandBuilder(1).build(Card.eight.spades, Card.eight.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            assertSplit(hand, dealerCard)
        }
    }

    @Test
    fun `nextMove default rules always double eleven`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREEBET

        initStrategy()

        val hand = HandBuilder(1).build(Card.eight.spades, Card.three.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            assertDouble(hand, dealerCard)
        }
    }

    @Test
    fun `nextMove splits even if insufficient balance`() {
        every { account.balance() } returnsMany listOf(1.0, 0.0)
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREEBET

        initStrategy()

        val hand = HandBuilder(1).build(Card.eight.spades, Card.eight.clubs)
        assertSplit(hand, Card.six.diamonds)
        assertSplit(hand, Card.ace.spades)
    }

    @Test
    fun `nextMove doubles even if insufficient balance`() {
        every { account.balance() } returnsMany listOf(1.0, 0.0)
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREEBET

        initStrategy()

        val hand = HandBuilder(1).build(Card.three.spades, Card.six.clubs)
        assertDouble(hand, Card.six.diamonds)
        assertDouble(hand, Card.ace.spades)
    }

    @Test
    fun `nextMove doubles free soft-20 against 6 with own money`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREEBET

        initStrategy()

        val hand = HandBuilder(initialBet = 1, isFree = true).build(Card.ace.diamonds, Card.nine.diamonds)
        assertStand(hand, Card.ace.clubs)
        assertDouble(hand, Card.six.clubs)
    }

    @Test
    fun `nextMove doubles free soft-18 against 5 and 6 with own money`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREEBET

        initStrategy()

        val hand = HandBuilder(initialBet = 1, isFree = false).build(Card.ace.diamonds, Card.seven.hearts)
        assertHit(hand, Card.ace.clubs)
        assertDouble(hand, Card.six.clubs)
        assertDouble(hand, Card.five.hearts)
    }
    @Test
    fun `nextMove stands on free soft-20 against 6 if not engough money`() {
        every { account.balance() } returnsMany listOf(1.0, 0.0)
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREEBET

        initStrategy()

        val hand = HandBuilder(initialBet = 1, isFree = false).build(Card.ace.diamonds, Card.nine.diamonds)
        assertStand(hand, Card.ace.clubs)
        assertStand(hand, Card.six.clubs)
    }

}