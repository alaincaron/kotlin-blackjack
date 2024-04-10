package org.alc.blackjack.impl

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.unmockkAll
import io.mockk.verify
import org.alc.blackjack.model.*
import org.alc.card.model.Card
import org.alc.card.model.GameShoe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

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

    @Test
    fun `should offer insurance on visible ace and payback insurance if blackjack`() {
        initTable()
        val cards = arrayOf(
            Card.five.clubs, // player 1st card
            Card.ace.diamonds, // dealer visible card
            Card.six.hearts,  // player 2nd card
            Card.queen.spades, // dealer hidden card
        )
        prepareShoe(*cards)
        val dealerCards = listOf(cards[1], cards[3])
        val playerCards = listOf(cards[0], cards[2])
        every { strategy.insurance(handMatch(playerCards)) } returns true

        table.newRound()

        verify { strategy.recordPush() }
        assertEquals(initialAmount, account.balance())
        verify { strategy.dealerCardVisible(cards[3]) }
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
        verify { strategy.finalDealerHand(handMatch(dealerCards)) }
        verify { strategy.finalHand(handMatch(playerCards)) }
    }

    @Test
    fun `should offer insurance on visible ace and end the hand for the player if blackjack and insurance refused`() {
        initTable()

        val cards = arrayOf(
            Card.five.spades, // player 1st card
            Card.ace.diamonds, // dealer visible card
            Card.six.hearts,  // player 2nd card
            Card.king.spades, // dealer hidden card
        )
        val playerCards = listOf(cards[0], cards[2])
        val dealerCards = listOf(cards[1], cards[3])
        val dealerCard = cards[3]
        prepareShoe(*cards)
        every { strategy.insurance(handMatch(playerCards)) } returns false

        table.newRound()

        verify { strategy.recordLoss(minBet.toDouble()) }
        assertEquals(initialAmount - minBet, account.balance())
        verify { strategy.dealerCardVisible(dealerCard) }
        verify { strategy.finalHand(handMatch(playerCards)) }
        verify { strategy.finalDealerHand(handMatch(dealerCards)) }
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should offer insurance on visible ace and play normally if insurance refused`() {
        initTable()
        val cards = arrayOf(
            Card.five.spades, // player 1st card
            Card.ace.spades, // dealer visible card
            Card.six.hearts,  // player 2nd card
            Card.seven.diamonds, // dealer hidden card
            Card.nine.hearts // player 3rd card
        )
        prepareShoe(*cards)
        val dealerCards = listOf(cards[1], cards[3])
        val playerCards = mutableListOf(cards[0], cards[2])
        every { strategy.insurance(handMatch(playerCards)) } returns false
        every { strategy.nextMove(handMatch(playerCards), dealerCards[0]) } returns Decision.HIT

        table.newRound()

        verify { strategy.dealerCardVisible(dealerCards[1])}
        verify { strategy.recordWin(minBet.toDouble()) }
        assertEquals(initialAmount + minBet, account.balance())
        playerCards.add(cards[4])
        verify { strategy.finalHand(handMatch(playerCards))}
        verify { strategy.finalDealerHand(handMatch(dealerCards))}
    }
}
