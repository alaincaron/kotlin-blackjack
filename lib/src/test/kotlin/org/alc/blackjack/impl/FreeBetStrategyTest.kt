package org.alc.blackjack.impl

import io.mockk.every
import org.alc.blackjack.model.Decision
import org.alc.blackjack.model.TableRule
import org.alc.card.model.Card
import org.alc.card.model.Rank
import org.alc.card.model.Suit
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FreeBetStrategyTest : AbstractStrategyTestHelper() {

    override fun createStrategy(gainFactor: Double) = FreeBetStrategy(account, gainFactor)

    @Test
    fun `equalPayment should return true if blackjackPlayFactor is less than 50 percent`() {
        every { table.rule } returns TableRule.FREE_BET.copy(blackjackPayFactor = 1.49)
        initStrategy()
        assertTrue(strategy.equalPayment())
    }

    @Test
    fun `equalPayment should return false if blackjackPlayFactor is at least 50 percent`() {
        every { table.rule } returns TableRule.FREE_BET
        initStrategy()
        assertFalse(strategy.equalPayment())
    }

    @Test
    fun `nextMove default rules never split tens and stand`() = neverSplitTensAndStand(TableRule.FREE_BET)

    @Test
    fun `nextMove default rules never split fives and always double`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREE_BET

        initStrategy()

        val hand = HandBuilder(initialBet = 1).build(Card.five.spades, Card.five.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            assertDouble(hand, dealerCard)
        }
    }

    @Test
    fun `nextMove default rules always split eights`() = pairOfEights(Decision.SPLIT, TableRule.FREE_BET)

    @Test
    fun `nextMove default rules always double 9 to 11`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREE_BET

        initStrategy()
        for (v1 in 1..7) {
            for (v2 in 7 - v1..9 - v1) {
                val card1 = Card(Rank.entries[v1], Suit.DIAMONDS)
                val card2 = Card(Rank.entries[v2], Suit.SPADES)
                if (card2.rank == Rank.ACE) continue // skip 8 and ACE
                val hand = HandBuilder(1).build(card1, card2)
                for (dealerRank in Rank.entries) {
                    val dealerCard = Card(dealerRank, Suit.SPADES)
                    assertDouble(hand, dealerCard)
                }
            }
        }
    }

    @Test
    fun `nextMove splits even if insufficient balance`() {
        every { account.balance() } returnsMany listOf(1.0, 0.0)
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREE_BET

        initStrategy()

        val hand = HandBuilder(1).build(Card.eight.spades, Card.eight.clubs)
        assertSplit(hand, Card.six.diamonds)
        assertSplit(hand, Card.ace.spades)
    }

    @Test
    fun `nextMove doubles even if insufficient balance`() {
        every { account.balance() } returnsMany listOf(1.0, 0.0)
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREE_BET

        initStrategy()

        val hand = HandBuilder(1).build(Card.three.spades, Card.six.clubs)
        assertDouble(hand, Card.six.diamonds)
        assertDouble(hand, Card.ace.spades)
    }

    @Test
    fun `nextMove doubles free soft-20 against 6 with own money`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREE_BET

        initStrategy()

        val hand = HandBuilder(initialBet = 1, isFree = true).build(Card.ace.diamonds, Card.nine.diamonds)
        assertStand(hand, Card.ace.clubs)
        assertDouble(hand, Card.six.clubs)
    }

    @Test
    fun `nextMove doubles free soft-18 against 5 and 6 with own money`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREE_BET

        initStrategy()

        val hand = HandBuilder(initialBet = 1, isFree = false).build(Card.ace.diamonds, Card.seven.hearts)
        assertHit(hand, Card.ace.clubs)
        assertDouble(hand, Card.six.clubs)
        assertDouble(hand, Card.five.hearts)
    }

    @Test
    fun `nextMove stands on free soft-20 against 6 if not enough money`() {
        every { account.balance() } returnsMany listOf(1.0, 0.0)
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.FREE_BET

        initStrategy()

        val hand = HandBuilder(initialBet = 1, isFree = false).build(Card.ace.diamonds, Card.nine.diamonds)
        assertStand(hand, Card.ace.clubs)
        assertStand(hand, Card.six.clubs)
    }

}