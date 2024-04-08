package org.alc.blackjack.impl

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.unmockkAll
import org.alc.blackjack.model.*
import org.alc.card.model.Card
import org.alc.card.model.GameShoe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.*

abstract class TableImplTestHelper(
    protected val defaultRule: TableRule,
    protected val initialAmount: Double = 1000.0,
    protected val minBet: Int = 10,
    protected val maxBet: Int = 1000,
    protected val nbDecks: Int = 8,
) {

    @RelaxedMockK
    protected lateinit var shoe: GameShoe

    @RelaxedMockK
    protected lateinit var random: Random

    @RelaxedMockK
    protected lateinit var strategy: Strategy

    protected lateinit var account: Account
    protected lateinit var table: Table

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    protected fun initTable(rule: TableRule? = null) {
        account = AccountImpl(initialAmount)
        every { strategy.account } returns account
        val player = Player(strategy)
        table = TableImpl(
            minBet = minBet,
            maxBet = maxBet,
            rule = rule ?: defaultRule,
            nbDecks = nbDecks,
            gameShoe = shoe,
            random = random
        )
        table.addPlayer(player)
    }

    protected fun prepareShoe(vararg cards: Card) {
        every { shoe.dealCard() } returnsMany listOf(
            Card.two.spades, // throw-away card
            *cards
        )
        every { strategy.initialBet() } returns minBet
    }
}
