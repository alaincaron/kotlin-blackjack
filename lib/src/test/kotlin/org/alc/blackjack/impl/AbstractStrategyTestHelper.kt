package org.alc.blackjack.impl

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.unmockkAll
import org.alc.blackjack.model.*
import org.alc.card.model.Card
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

abstract class AbstractStrategyTestHelper {
    @RelaxedMockK
    protected lateinit var account: Account

    @RelaxedMockK
    protected lateinit var table: Table

    protected lateinit var strategy: Strategy

    protected fun assertMove(decision: Decision, hand: Hand, dealerCard: Card) {
        assertEquals(decision, strategy.nextMove(hand, dealerCard)) { "Failed to ${decision.toString().lowercase()} on ${dealerCard.rank}"}
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
}