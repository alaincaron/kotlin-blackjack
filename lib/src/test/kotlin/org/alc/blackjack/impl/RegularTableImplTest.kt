package org.alc.blackjack.impl

import io.mockk.*
import org.alc.blackjack.model.*
import org.alc.card.model.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class RegularTableImplTest: TableImplTestHelper(TableRule.DEFAULT) {

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
        assertEquals(initialAmount, account.balance())
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
            Card.jack.diamonds, // player 2nd card, 1st hand
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
            Card.jack.hearts,    // player 3rd card, 1st hand
            Card.six.diamonds, // player 2nd card, 2nd hand
            Card.jack.spades,   // player 3rd, 3rd hand
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
        initTable(TableRule.DEFAULT.copy(dealerHitsOnSoft17 = false))
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
