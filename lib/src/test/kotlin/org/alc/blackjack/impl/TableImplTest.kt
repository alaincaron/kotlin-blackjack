package org.alc.blackjack.impl

import io.mockk.*
import io.mockk.impl.annotations.*
import org.alc.blackjack.model.*
import org.alc.card.model.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.*

class TableImplTest {

    private val initialAmount = 1000.0
    private val minBet = 10
    private val maxBet = 100
    private val nbDecks = 8

    @RelaxedMockK
    lateinit var shoe: GameShoe

    @RelaxedMockK
    lateinit var random: Random

    @RelaxedMockK
    lateinit var strategy: Strategy

    private lateinit var account: Account
    private lateinit var player: Player
    private lateinit var table: Table

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun initTable(dealerHitsOnSoft17: Boolean = true, blackjackPayFactor: Double = 1.5) {
        account = AccountImpl(initialAmount)
        every { strategy.account } returns account
        player = Player(strategy)
        val rule = TableRule(
            dealerHitsOnSoft17 = dealerHitsOnSoft17,
            blackjackPayFactor = blackjackPayFactor
        )
        table = TableImpl(
            minBet = minBet,
            maxBet = maxBet,
            rule,
            nbDecks = nbDecks,
            gameShoe = shoe,
            random = random
        )
        table.addPlayer(player)
    }

    private fun prepareShoe(vararg cards: Card) {
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
        assert(account.balance() == initialAmount)
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
        assert(account.balance() == initialAmount - minBet)
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
            Card.nine.diamonds, // dealer hidden card
            Card.ten.hearts // player 3rd card
        )
        prepareShoe(*cards)
        val dealerCards = listOf(cards[1], cards[3])
        val playerCards = mutableListOf(cards[0], cards[2])
        every { strategy.insurance(handMatch(playerCards)) } returns false
        every { strategy.nextMove(handMatch(playerCards), dealerCards[0]) } returns Decision.HIT

        table.newRound()

        verify { strategy.dealerCardVisible(dealerCards[1])}
        verify { strategy.recordWin(minBet.toDouble()) }
        assert(account.balance() == initialAmount + minBet)
        playerCards.add(cards[4])
        verify { strategy.finalHand(handMatch(playerCards))}
        verify { strategy.finalDealerHand(handMatch(dealerCards))}
    }

    @Test
    fun `should offer equal payment on visible ace and both have blackjack and push if refused`() {
        initTable()
        prepareShoe(
            Card.ace.spades, // player 1st card
            Card.ace.spades, // dealer visible card
            Card.king.hearts,  // player 2nd card
            Card.king.diamonds // dealer hidden card
        )
        every { strategy.equalPayment() } returns false

        table.newRound()

        verify { strategy.recordPush() }
        assert(account.balance() == initialAmount)
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should offer equal payment on visible ace and both have blackjack and pay if accepted`() {
        initTable()
        prepareShoe(
            Card.ace.spades, // player 1st card
            Card.ace.spades, // dealer visible card
            Card.king.hearts,  // player 2nd card
            Card.king.diamonds // dealer hidden card
        )
        every { strategy.equalPayment() } returns true

        table.newRound()

        verify { strategy.recordWin(minBet.toDouble()) }
        assertEquals(initialAmount + minBet, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should offer equal payment on visible ace and pay bet if accepted`() {
        initTable()
        prepareShoe(
            Card.ace.spades, // player 1st card
            Card.ace.spades, // dealer visible card
            Card.king.hearts,  // player 2nd card
            Card.ace.diamonds // dealer hidden card
        )
        every { strategy.equalPayment() } returns true

        table.newRound()

        verify { strategy.recordWin(minBet.toDouble()) }
        assertEquals(initialAmount + minBet, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should offer equal payment on visible ace and pay premium bet if refused`() {
        initTable()
        prepareShoe(
            Card.ace.spades, // player 1st card
            Card.ace.spades, // dealer visible card
            Card.king.hearts,  // player 2nd card
            Card.ace.diamonds // dealer hidden card
        )
        every { strategy.equalPayment() } returns false

        table.newRound()

        verify { strategy.recordWin(minBet * 1.5) }
        assertEquals(initialAmount + minBet * 1.5, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should allow to split pair ACES only once`() {
        initTable()

        val cards = arrayOf(
            Card.ace.spades,   // player 1st card
            Card.six.spades,   // dealer visible card
            Card.ace.hearts,   // player 2nd card
            Card.six.diamonds, // dealer hidden card
            Card.ten.diamonds, // player 2nd card, 1st hand
            Card.ace.diamonds, // player 2nd card, 2nd
            Card.five.spades   // dealer 3rd card
        )
        prepareShoe(*cards)
        val dealerCards = listOf(cards[1], cards[3], cards[6])
        val dealerCard = cards[1]
        val playerInitialHand = listOf(cards[0], cards[2])
        val playerFirstHandCards = listOf(cards[0], cards[4])
        val playerSecondHandCards = listOf(cards[2], cards[5])
        every { strategy.nextMove(handMatch(playerInitialHand), dealerCard) } returns Decision.SPLIT

        table.newRound()

        verify { strategy.recordWin(minBet.toDouble()) }
        verify { strategy.recordLoss(minBet.toDouble()) }
        assertEquals(initialAmount, account.balance())
        verify { strategy.finalHand(handMatch(playerFirstHandCards)) }
        verify { strategy.finalHand(handMatch(playerSecondHandCards)) }
        verify { strategy.finalDealerHand(handMatch(dealerCards)) }
    }

    @Test
    fun `should allow to double ot hit on split of pair of non-aces`() {
        initTable()
        prepareShoe(
            Card.eight.spades,   // player 1st card
            Card.six.spades,   // dealer visible card
            Card.eight.hearts,   // player 2nd card
            Card.six.diamonds, // dealer hidden card
            Card.three.diamonds, // player 2nd card, 1st hand
            Card.ten.hearts,    // player 3rd card, 1st hand
            Card.six.diamonds, // player 2nd card, 2nd hand
            Card.ten.spades,   // player 3rd, 3rd hand
            Card.five.spades   // dealer 3rd card
        )
        every { strategy.nextMove(any(), any()) } returnsMany listOf(
            Decision.SPLIT,
            Decision.DOUBLE,
            Decision.HIT
        )

        table.newRound()

        verify { strategy.recordWin(minBet * 2.0) }
        verify { strategy.recordLoss(minBet.toDouble()) }
        assertEquals(initialAmount + minBet, account.balance())
        verify(exactly = 2) { strategy.finalHand(any()) }
        verify { strategy.finalDealerHand(any()) }
    }

    @Test
    fun `should stand on soft 17 if dealHitsOnSoft17 rule is not enabled`() {
        initTable(dealerHitsOnSoft17 = false)
        prepareShoe(
            Card.eight.spades,   // player 1st card
            Card.six.spades,   // dealer visible card
            Card.nine.hearts,   // player 2nd card
            Card.ace.diamonds, // dealer hidden card
        )
        every { strategy.nextMove(any(), any()) } returns Decision.STAND

        table.newRound()

        verify { strategy.recordPush() }
        assertEquals(initialAmount, account.balance())
        verify { strategy.dealerReceived(Card.six.spades) }
        verify { strategy.dealerCardVisible(Card.ace.diamonds) }
        verify { strategy.received(Card.eight.spades) }
        verify { strategy.received(Card.nine.hearts) }
    }

    @Test
    fun `should hit on soft 17 if dealHitsOnSoft17 rule is enabled`() {
        initTable()
        prepareShoe(
            Card.eight.spades,   // player 1st card
            Card.six.spades,   // dealer visible card
            Card.nine.hearts,   // player 2nd card
            Card.ace.diamonds, // dealer hidden card
            Card.three.spades
        )
        every { strategy.nextMove(any(), any()) } returns Decision.STAND

        table.newRound()

        verify { strategy.recordLoss(minBet.toDouble()) }
        assertEquals(initialAmount - minBet, account.balance())
        verify { strategy.dealerReceived(Card.six.spades) }
        verify { strategy.dealerCardVisible(Card.ace.diamonds) }
        verify { strategy.dealerReceived(Card.three.spades) }
        verify { strategy.received(Card.eight.spades) }
        verify { strategy.received(Card.nine.hearts) }
    }
}
