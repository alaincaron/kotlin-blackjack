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
    private val minBet = 10.0
    private val maxBet = 100.0
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
            Card(Rank.TWO, Suit.SPADES), // throw-away card
            *cards
        )
        every { strategy.initialBet() } returns minBet

    }

    @Test
    fun `should offer insurance on visible ace and payback insurance if blackjack`() {
        initTable()
        val cards = arrayOf(
            Card(Rank.FIVE, Suit.CLUBS), // player 1st card
            Card(Rank.ACE, Suit.DIAMONDS), // dealer visible card
            Card(Rank.SIX, Suit.HEARTS),  // player 2nd card
            Card(Rank.QUEEN, Suit.SPADES), // dealer hidden card
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
            Card(Rank.FIVE, Suit.SPADES), // player 1st card
            Card(Rank.ACE, Suit.DIAMONDS), // dealer visible card
            Card(Rank.SIX, Suit.HEARTS),  // player 2nd card
            Card(Rank.KING, Suit.SPADES), // dealer hidden card
        )
        val playerCards = listOf(cards[0], cards[2])
        val dealerCards = listOf(cards[1], cards[3])
        val dealerCard = cards[3]
        prepareShoe(*cards)
        every { strategy.insurance(handMatch(playerCards)) } returns false

        table.newRound()

        verify { strategy.recordLoss(minBet) }
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
            Card(Rank.FIVE, Suit.SPADES), // player 1st card
            Card(Rank.ACE, Suit.SPADES), // dealer visible card
            Card(Rank.SIX, Suit.HEARTS),  // player 2nd card
            Card(Rank.NINE, Suit.DIAMONDS), // dealer hidden card
            Card(Rank.TEN, Suit.HEARTS) // player 3rd card
        )
        prepareShoe(*cards)
        val dealerCards = listOf(cards[1], cards[3])
        val playerCards = mutableListOf(cards[0], cards[2])
        every { strategy.insurance(handMatch(playerCards)) } returns false
        every { strategy.nextMove(handMatch(playerCards), dealerCards[0]) } returns Decision.HIT

        table.newRound()

        verify { strategy.dealerCardVisible(dealerCards[1])}
        verify { strategy.recordWin(minBet) }
        assert(account.balance() == initialAmount + minBet)
        playerCards.add(cards[4])
        verify { strategy.finalHand(handMatch(playerCards))}
        verify { strategy.finalDealerHand(handMatch(dealerCards))}
    }

    @Test
    fun `should offer equal payment on visible ace and both have blackjack and push if refused`() {
        initTable()
        prepareShoe(
            Card(Rank.ACE, Suit.SPADES), // player 1st card
            Card(Rank.ACE, Suit.SPADES), // dealer visible card
            Card(Rank.KING, Suit.HEARTS),  // player 2nd card
            Card(Rank.KING, Suit.DIAMONDS) // dealer hidden card
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
            Card(Rank.ACE, Suit.SPADES), // player 1st card
            Card(Rank.ACE, Suit.SPADES), // dealer visible card
            Card(Rank.KING, Suit.HEARTS),  // player 2nd card
            Card(Rank.KING, Suit.DIAMONDS) // dealer hidden card
        )
        every { strategy.equalPayment() } returns true

        table.newRound()

        verify { strategy.recordWin(minBet) }
        assertEquals(initialAmount + minBet, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should offer equal payment on visible ace and pay bet if accepted`() {
        initTable()
        prepareShoe(
            Card(Rank.ACE, Suit.SPADES), // player 1st card
            Card(Rank.ACE, Suit.SPADES), // dealer visible card
            Card(Rank.KING, Suit.HEARTS),  // player 2nd card
            Card(Rank.ACE, Suit.DIAMONDS) // dealer hidden card
        )
        every { strategy.equalPayment() } returns true

        table.newRound()

        verify { strategy.recordWin(minBet) }
        assertEquals(initialAmount + minBet, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should offer equal payment on visible ace and pay premium bet if refused`() {
        initTable()
        prepareShoe(
            Card(Rank.ACE, Suit.SPADES), // player 1st card
            Card(Rank.ACE, Suit.SPADES), // dealer visible card
            Card(Rank.KING, Suit.HEARTS),  // player 2nd card
            Card(Rank.ACE, Suit.DIAMONDS) // dealer hidden card
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
            Card(Rank.ACE, Suit.SPADES),   // player 1st card
            Card(Rank.SIX, Suit.SPADES),   // dealer visible card
            Card(Rank.ACE, Suit.HEARTS),   // player 2nd card
            Card(Rank.SIX, Suit.DIAMONDS), // dealer hidden card
            Card(Rank.TEN, Suit.DIAMONDS), // player 2nd card, 1st hand
            Card(Rank.ACE, Suit.DIAMONDS), // player 2nd card, 2nd
            Card(Rank.FIVE, Suit.SPADES)   // dealer 3rd card
        )
        prepareShoe(*cards)
        val dealerCards = listOf(cards[1], cards[3], cards[6])
        val dealerCard = cards[1]
        val playerInitialHand = listOf(cards[0], cards[2])
        val playerFirstHandCards = listOf(cards[0], cards[4])
        val playerSecondHandCards = listOf(cards[2], cards[5])
        every { strategy.nextMove(handMatch(playerInitialHand), dealerCard) } returns Decision.SPLIT

        table.newRound()

        verify { strategy.recordWin(minBet) }
        verify { strategy.recordLoss(minBet) }
        assertEquals(initialAmount, account.balance())
        verify { strategy.finalHand(handMatch(playerFirstHandCards)) }
        verify { strategy.finalHand(handMatch(playerSecondHandCards)) }
        verify { strategy.finalDealerHand(handMatch(dealerCards)) }
    }

    @Test
    fun `should allow to double ot hit on split of pair of non-aces`() {
        initTable()
        prepareShoe(
            Card(Rank.EIGHT, Suit.SPADES),   // player 1st card
            Card(Rank.SIX, Suit.SPADES),   // dealer visible card
            Card(Rank.EIGHT, Suit.HEARTS),   // player 2nd card
            Card(Rank.SIX, Suit.DIAMONDS), // dealer hidden card
            Card(Rank.THREE, Suit.DIAMONDS), // player 2nd card, 1st hand
            Card(Rank.TEN, Suit.HEARTS),    // player 3rd card, 1st hand
            Card(Rank.SIX, Suit.DIAMONDS), // player 2nd card, 2nd hand
            Card(Rank.TEN, Suit.SPADES),   // player 3rd, 3rd hand
            Card(Rank.FIVE, Suit.SPADES)   // dealer 3rd card
        )
        every { strategy.nextMove(any(), any()) } returnsMany listOf(
            Decision.SPLIT,
            Decision.DOUBLE,
            Decision.HIT
        )

        table.newRound()

        verify { strategy.recordWin(minBet * 2.0) }
        verify { strategy.recordLoss(minBet) }
        assertEquals(initialAmount + minBet, account.balance())
        verify(exactly = 2) { strategy.finalHand(any()) }
        verify { strategy.finalDealerHand(any()) }
    }

    @Test
    fun `should stand on soft 17 if dealHitsOnSoft17 rule is not enabled`() {
        initTable(dealerHitsOnSoft17 = false)
        prepareShoe(
            Card(Rank.EIGHT, Suit.SPADES),   // player 1st card
            Card(Rank.SIX, Suit.SPADES),   // dealer visible card
            Card(Rank.NINE, Suit.HEARTS),   // player 2nd card
            Card(Rank.ACE, Suit.DIAMONDS), // dealer hidden card
        )
        every { strategy.nextMove(any(), any()) } returns Decision.STAND

        table.newRound()

        verify { strategy.recordPush() }
        assertEquals(initialAmount, account.balance())
        verify { strategy.dealerReceived(Card(Rank.SIX, Suit.SPADES)) }
        verify { strategy.dealerCardVisible(Card(Rank.ACE, Suit.DIAMONDS)) }
        verify { strategy.received(Card(Rank.EIGHT, Suit.SPADES)) }
        verify { strategy.received(Card(Rank.NINE, Suit.HEARTS)) }
    }

    @Test
    fun `should hit on soft 17 if dealHitsOnSoft17 rule is enabled`() {
        initTable()
        prepareShoe(
            Card(Rank.EIGHT, Suit.SPADES),   // player 1st card
            Card(Rank.SIX, Suit.SPADES),   // dealer visible card
            Card(Rank.NINE, Suit.HEARTS),   // player 2nd card
            Card(Rank.ACE, Suit.DIAMONDS), // dealer hidden card
            Card(Rank.THREE, Suit.SPADES)
        )
        every { strategy.nextMove(any(), any()) } returns Decision.STAND

        table.newRound()

        verify { strategy.recordLoss(minBet) }
        assertEquals(initialAmount - minBet, account.balance())
        verify { strategy.dealerReceived(Card(Rank.SIX, Suit.SPADES)) }
        verify { strategy.dealerCardVisible(Card(Rank.ACE, Suit.DIAMONDS)) }
        verify { strategy.dealerReceived(Card(Rank.THREE, Suit.SPADES)) }
        verify { strategy.received(Card(Rank.EIGHT, Suit.SPADES)) }
        verify { strategy.received(Card(Rank.NINE, Suit.HEARTS)) }
    }
}
