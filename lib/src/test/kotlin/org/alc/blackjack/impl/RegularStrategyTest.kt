package org.alc.blackjack.impl

import io.mockk.every
import org.alc.blackjack.model.Decision
import org.alc.blackjack.model.TableRule
import org.alc.card.model.Card
import org.alc.card.model.Rank
import org.alc.card.model.Suit
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RegularStrategyTest : AbstractStrategyTestHelper() {

    override fun createStrategy(gainFactor: Double) = RegularStrategy(account, gainFactor)

    @Test
    fun `equalPayment should return true if blackjackPlayFactor is less than 50 percent`() {
        every { table.rule } returns TableRule(blackjackPayFactor = 1.49)
        initStrategy()
        assertTrue(strategy.equalPayment())
    }

    @Test
    fun `equalPayment should return false if blackjackPlayFactor is at least 50 percent`() {
        every { table.rule } returns TableRule(blackjackPayFactor = 1.5)
        initStrategy()
        assertFalse(strategy.equalPayment())
    }

    @Test
    fun `nextMove default rules never split tens and stand`() {
        neverSplitTensAndStand(TableRule.DEFAULT)
    }

    @Test
    fun `nextMove default rules never split fives and double except for dealer showing ace or ten`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.DEFAULT

        initStrategy()

        val hand = HandBuilder(initialBet = 1).build(Card.five.spades, Card.five.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            when (dealerCard.value) {
                10,11 -> assertHit(hand, dealerCard)
                else -> assertDouble(hand, dealerCard)
            }
        }
    }

    @Test
    fun `nextMove default rules always split eights`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.DEFAULT

        initStrategy()

        val hand = HandBuilder(1).build(Card.eight.spades, Card.eight.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            assertSplit(hand, dealerCard)
        }
    }

    @Test
    fun `nextMove do not split eights against ace if surrender is allowed`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.DEFAULT.copy(allowSurrender = true)

        initStrategy()

        val hand = HandBuilder(1).build(Card.eight.spades, Card.eight.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            assertMove(
                if (dealerRank == Rank.ACE ) Decision.SURRENDER else Decision.SPLIT,
                hand,
                dealerCard
            )
        }
    }


    @Test
    fun `nextMove default rules always double eleven`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.DEFAULT

        initStrategy()

        val hand = HandBuilder(1).build(Card.eight.spades, Card.three.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            assertDouble(hand, dealerCard)
        }
    }

    @Test fun `nextMove does not split if insufficient balance`() {
        every { account.balance() } returnsMany listOf(1.0, 0.0)
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.DEFAULT

        initStrategy()

        val hand = HandBuilder(1).build(Card.eight.spades, Card.eight.clubs)
        assertStand(hand, Card.six.diamonds)
        assertHit(hand, Card.ace.spades)
    }

    @Test fun `nextMove does not double if insufficient balance`() {
        every { account.balance() } returnsMany listOf(1.0, 0.0)
        every { table.minBet } returns 1
        every { table.rule } returns TableRule.DEFAULT

        initStrategy()

        val hand = HandBuilder(1).build(Card.three.spades, Card.eight.clubs)
        assertHit(hand, Card.six.diamonds)
        assertHit(hand, Card.ace.spades)
    }
}