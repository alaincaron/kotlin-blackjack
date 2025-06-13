package org.alc.blackjack.impl

import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
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

    protected fun prepareShoe( cards: List<Card>) {
        every { shoe.dealCard() } returnsMany buildList(cards.size + 1) {
            add(Card.two.spades) // throw-away card
            addAll(cards)
        }
        every { strategy.initialBet() } returns minBet
    }

    @Test
    fun `should offer insurance on visible ace and payback insurance if blackjack`() {
        initTable()
        val cards = listOf(
            Card.five.clubs, // player 1st card
            Card.ace.diamonds, // dealer visible card
            Card.six.hearts,  // player 2nd card
            Card.queen.spades, // dealer hidden card
        )
        prepareShoe(cards)
        val dealerCards = select(cards, 1, 3)
        val playerCards = select(cards,0, 2)
        every { strategy.insurance(handMatch(playerCards)) } returns true

        table.newRound()

        verifyPlayerCards(strategy, playerCards)
        verifyDealerCards(strategy, dealerCards)

        assertEquals(initialAmount, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
        verify { strategy.finalDealerHand(handMatch(dealerCards)) }
        verify { strategy.finalHand(handMatch(playerCards)) }
        verify { strategy.recordResult(Outcome.PUSH, 0.0, handMatch(playerCards), handMatch(dealerCards))}
    }

    @Test
    fun `should offer insurance on visible ace and end the hand for the player if blackjack and insurance refused`() {
        initTable()

        val cards = listOf(
            Card.five.spades, // player 1st card
            Card.ace.diamonds, // dealer visible card
            Card.six.hearts,  // player 2nd card
            Card.king.spades, // dealer hidden card
        )
        val playerCards = select(cards, 0, 2)
        val dealerCards = select(cards, 1, 3)
        prepareShoe(cards)
        every { strategy.insurance(handMatch(playerCards)) } returns false

        table.newRound()

        verifyPlayerCards(strategy, playerCards)
        verifyDealerCards(strategy, dealerCards)

        assertEquals(initialAmount - minBet, account.balance())
        verify { strategy.finalHand(handMatch(playerCards)) }
        verify { strategy.finalDealerHand(handMatch(dealerCards)) }
        verify { strategy.recordResult(Outcome.LOSS, minBet.toDouble(), handMatch(playerCards), handMatch(dealerCards))}
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should offer insurance on visible ace and play normally if insurance refused`() {
        initTable()
        val cards = listOf(
            Card.five.spades, // player 1st card
            Card.ace.spades, // dealer visible card
            Card.six.hearts,  // player 2nd card
            Card.seven.diamonds, // dealer hidden card
            Card.nine.hearts // player 3rd card
        )
        prepareShoe(cards)
        val dealerCards = select(cards, 1, 3)
        val playerInitialCards = select(cards, 0, 2)
        every { strategy.insurance(handMatch(playerInitialCards)) } returns false
        every { strategy.nextMove(handMatch(playerInitialCards), dealerCards[0]) } returns Decision.HIT
        val playerCards = playerInitialCards + cards[4]

        table.newRound()

        verifyPlayerCards(strategy, playerCards)
        verifyDealerCards(strategy, dealerCards)

        assertEquals(initialAmount + minBet, account.balance())
        verify { strategy.finalHand(handMatch(playerCards))}
        verify { strategy.finalDealerHand(handMatch(dealerCards))}
        verify { strategy.recordResult(Outcome.WIN, minBet.toDouble(), handMatch(playerCards), handMatch(dealerCards))}
    }
}
