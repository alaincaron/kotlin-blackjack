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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

abstract class AbstractStrategyTestHelper {
    @RelaxedMockK
    protected lateinit var account: Account

    @RelaxedMockK
    protected lateinit var table: Table

    protected lateinit var strategy: Strategy

    protected fun assertMove(decision: Decision, hand: Hand, dealerCard: Card) {
        assertEquals(decision, strategy.nextMove(hand, dealerCard)) { "Failed to ${decision.toString().lowercase()} on ${dealerCard.rank} with hand $hand"}
    }

    protected fun assertHit(hand: Hand, dealerCard: Card) = assertMove(Decision.HIT, hand, dealerCard)
    protected fun assertStand(hand: Hand, dealerCard: Card) = assertMove(Decision.STAND, hand, dealerCard)
    protected fun assertDouble(hand: Hand, dealerCard: Card) = assertMove(Decision.DOUBLE, hand, dealerCard)
    protected fun assertSplit(hand: Hand, dealerCard: Card) = assertMove(Decision.SPLIT, hand, dealerCard)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    protected abstract fun createStrategy(gainFactor: Double): Strategy

    protected fun initStrategy(gainFactor: Double = 0.25) {
        strategy = createStrategy(gainFactor)
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
        val hand = HandImpl(1)
        assertFalse(strategy.insurance(hand))
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


    protected fun pairOfEights(aceDecision: Decision, rule: TableRule) {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns rule

        initStrategy()

        val hand = HandBuilder(1).build(Card.eight.spades, Card.eight.spades)
        for (dealerRank in Rank.entries) {
            val dealerCard = Card(dealerRank, Suit.SPADES)
            assertMove(if (dealerRank == Rank.ACE) aceDecision else Decision.SPLIT, hand, dealerCard)
        }
    }

    protected fun neverSplitTensAndStand(rule: TableRule) {
        every { account.balance() } returns 1.0
        every { table.minBet } returns 1
        every { table.rule } returns rule

        initStrategy()

        val hand = HandBuilder(initialBet = 1).build(Card.ten.spades, Card.ten.spades)
        for (dealerCard in Rank.entries.map { Card(it, Suit.SPADES) }) {
            assertStand(hand, dealerCard)
        }
    }
}