package org.alc.blackjack.impl

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.unmockkAll
import org.alc.blackjack.model.*
import org.alc.card.model.Card
import org.alc.card.model.Rank
import org.alc.card.model.Suit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultStrategyTest {

    @RelaxedMockK
    private lateinit var account: Account

    @RelaxedMockK
    private lateinit var table: Table

    private lateinit var strategy: DefaultStrategy

    fun assertMove(decision: Decision, hand: Hand, dealerCard: Card) {
        assertEquals(decision, strategy.nextMove(hand, dealerCard)) { "Failed to ${decision.toString().lowercase()} on ${dealerCard.rank}"}
    }

    fun assertHit(hand: Hand, dealerCard: Card) = assertMove(Decision.HIT, hand, dealerCard)
    fun assertStand(hand: Hand, dealerCard: Card) = assertMove(Decision.STAND, hand, dealerCard)
    fun assertDouble(hand: Hand, dealerCard: Card) = assertMove(Decision.DOUBLE, hand, dealerCard)
    fun assertSplit(hand: Hand, dealerCard: Card) = assertMove(Decision.SPLIT, hand, dealerCard)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    private fun initStrategy(gainFactor: Double = 0.25) {
        strategy = DefaultStrategy(account, gainFactor)
        strategy.enteredTable(table)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `should always refuse insurance`() {
        initStrategy()
        // always reject insurance
        val hand = HandImpl(1.0)
        assertFalse(strategy.insurance(hand))
    }

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
    fun `initialBet should return the min if enough`() {
        every { account.balance() } returns 10.0
        every { table.minBet } returns 1.0

        initStrategy()

        assertEquals(1.0, strategy.initialBet())
    }

    @Test
    fun `initialBet should return the 0 if enough balance`() {
        every { account.balance() } returns 0.5
        every { table.minBet } returns 1.0

        initStrategy()

        assertEquals(0.0, strategy.initialBet())
    }

    @Test
    fun `nextMove default rules never split tens and stand`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1.0
        every { table.rule } returns TableRule.DEFAULT

        initStrategy()

        val hand = HandBuiler(initialBet = 1.0).build(Card.ten.spades, Card.ten.spades)
        for (dealerCard in Rank.entries.map { Card(it, Suit.SPADES) }) {
            assertStand(hand, dealerCard)
        }
    }

    @Test
    fun `nextMove default rules never split fives and double except for dealer showing ace or ten`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1.0
        every { table.rule } returns TableRule.DEFAULT

        initStrategy()

        val hand = HandBuiler(initialBet = 1.0).build(Card.five.spades, Card.five.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            when (dealerRank.value()) {
                1, 10 -> assertHit(hand, dealerCard)
                else -> assertDouble(hand, dealerCard)
            }
        }
    }

    @Test
    fun `nextMove default rules always split eights`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1.0
        every { table.rule } returns TableRule.DEFAULT

        initStrategy()

        val hand = HandBuiler(1.0).build(Card.eight.spades, Card.eight.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            assertSplit(hand, dealerCard)
        }
    }

    @Test
    fun `nextMove default rules always double eleven`() {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1.0
        every { table.rule } returns TableRule.DEFAULT

        initStrategy()

        val hand = HandBuiler(1.0).build(Card.eight.spades, Card.three.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            assertDouble(hand, dealerCard)
        }
    }

    @Test fun `nextMove does not split if insufficient balance`() {
        every { account.balance() } returnsMany listOf(1.0, 0.0)
        every { table.minBet } returns 1.0
        every { table.rule } returns TableRule.DEFAULT

        initStrategy()

        val hand = HandBuiler(1.0).build(Card.eight.spades, Card.eight.clubs)
        assertStand(hand, Card.six.diamonds)
        assertHit(hand, Card.ace.spades)
    }

    @Test fun `nextMove does not double if insufficient balance`() {
        every { account.balance() } returnsMany listOf(1.0, 0.0)
        every { table.minBet } returns 1.0
        every { table.rule } returns TableRule.DEFAULT

        initStrategy()

        val hand = HandBuiler(1.0).build(Card.three.spades, Card.eight.clubs)
        assertHit(hand, Card.six.diamonds)
        assertHit(hand, Card.ace.spades)
    }
}