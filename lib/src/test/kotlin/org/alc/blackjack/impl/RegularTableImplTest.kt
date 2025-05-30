package org.alc.blackjack.impl

import io.mockk.*
import org.alc.blackjack.model.*
import org.alc.card.model.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class RegularTableImplTest : TableImplTestHelper(TableRule.DEFAULT) {

    @Test
    fun `should offer equal payment on visible ace and both have blackjack and push if refused`() {
        initTable()
        val cards = listOf(
            Card.ace.spades, // player 1st card
            Card.ace.spades, // dealer visible card
            Card.king.hearts,  // player 2nd card
            Card.king.diamonds // dealer hidden card
        )
        prepareShoe(cards)
        val playerCards = select(cards, 0, 2)
        val dealerCards = select(cards, 1, 3)
        every { strategy.equalPayment() } returns false

        table.newRound()

        verify { strategy.recordResult(Outcome.PUSH, 0.0, handMatch(playerCards), handMatch(dealerCards)) }
        assertEquals(initialAmount, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should offer equal payment on visible ace an pay if dealer has blackjack and accepted`() {
        initTable()
        val cards = listOf(
            Card.ace.spades, // player 1st card
            Card.ace.spades, // dealer visible card
            Card.king.hearts,  // player 2nd card
            Card.king.diamonds // dealer hidden card
        )
        val playerCards = select(cards, 0, 2)
        val dealerCards = select(cards, 1, 2)
        prepareShoe(cards)

        every { strategy.equalPayment() } returns true

        table.newRound()

        verify {
            strategy.recordResult(
                Outcome.BLACKJACK_EQUAL_PAYMENT,
                minBet.toDouble(),
                handMatch(playerCards),
                handMatch(dealerCards)
            )
        }
        assertEquals(initialAmount + minBet, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should offer equal payment on visible ace and pay bet if accepted`() {
        initTable()
        val cards = listOf(
            Card.ace.spades, // player 1st card
            Card.ace.spades, // dealer visible card
            Card.king.hearts,  // player 2nd card
            Card.ace.diamonds // dealer hidden card
        )

        val playCards = select(cards, 0, 2)

        prepareShoe(cards)

        every { strategy.equalPayment() } returns true

        table.newRound()

        verify { strategy.recordResult(Outcome.BLACKJACK_EQUAL_PAYMENT, minBet.toDouble(), handMatch(playCards), null) }
        assertEquals(initialAmount + minBet, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should offer equal payment on visible ace and pay premium bet if refused`() {
        initTable()
        val cards = listOf(
            Card.ace.spades, // player 1st card
            Card.ace.spades, // dealer visible card
            Card.king.hearts,  // player 2nd card
            Card.ace.diamonds // dealer hidden card
        )
        prepareShoe(cards)
        val playerCards = select(cards, 0, 2)
        every { strategy.equalPayment() } returns false

        table.newRound()

        verify { strategy.recordResult(Outcome.BLACKJACK, minBet * 1.5, handMatch(playerCards), null) }
        assertEquals(initialAmount + minBet * 1.5, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should allow to split pair ACES only once`() {
        initTable()

        val cards = listOf(
            Card.ace.spades,   // player 1st card
            Card.six.spades,   // dealer visible card
            Card.ace.hearts,   // player 2nd card
            Card.six.diamonds, // dealer hidden card
            Card.jack.diamonds, // player 2nd card, 1st hand
            Card.ace.diamonds, // player 2nd card, 2nd
            Card.five.spades   // dealer 3rd card
        )
        prepareShoe(cards)
        val dealerCards = listOf(cards[1], cards[3], cards[6])
        val playerInitialHand = listOf(cards[0], cards[2])
        val playerFirstHandCards = listOf(cards[0], cards[4])
        val playerSecondHandCards = listOf(cards[2], cards[5])
        every { strategy.nextMove(handMatch(playerInitialHand), dealerCards[0]) } returns Decision.SPLIT

        table.newRound()

        verify {
            strategy.recordResult(
                Outcome.WIN,
                minBet.toDouble(),
                handMatch(playerFirstHandCards),
                handMatch(dealerCards)
            )
        }
        verify {
            strategy.recordResult(
                Outcome.LOSS,
                minBet.toDouble(),
                handMatch(playerSecondHandCards),
                handMatch(dealerCards)
            )
        }

        assertEquals(initialAmount, account.balance())
        verify { strategy.finalHand(handMatch(playerFirstHandCards)) }
        verify { strategy.finalHand(handMatch(playerSecondHandCards)) }
        verify { strategy.finalDealerHand(handMatch(dealerCards)) }
    }

    @Test
    fun `should allow to double ot hit on split of pair of non-aces`() {
        initTable()
        val cards = listOf(
            Card.eight.spades,   // player 1st card
            Card.six.spades,   // dealer visible card
            Card.eight.hearts,   // player 2nd card
            Card.six.diamonds, // dealer hidden card
            Card.three.diamonds, // player 2nd card, 1st hand
            Card.jack.hearts,    // player 3rd card, 1st hand
            Card.six.diamonds, // player 2nd card, 2nd hand
            Card.jack.spades,   // player 3rd card, 2nd hand
            Card.five.spades   // dealer 3rd card
        )

        val firstHand = select(cards, 0, 4, 5)
        val secondHand = select(cards, 2, 6, 7)
        val dealerHand = select(cards, 1, 3, 8)

        prepareShoe(cards)
        every { strategy.nextMove(any(), any()) } returnsMany listOf(
            Decision.SPLIT,
            Decision.DOUBLE,
            Decision.HIT
        )

        table.newRound()

        verify { strategy.recordResult(Outcome.DOUBLE_WIN, minBet * 2.0, handMatch(firstHand), handMatch(dealerHand))}
        verify { strategy.recordResult(Outcome.BUST, minBet.toDouble(), handMatch(secondHand), null) }
        assertEquals(initialAmount + minBet, account.balance())
        verify(exactly = 2) { strategy.finalHand(any()) }
        verify { strategy.finalDealerHand(any()) }
    }

    @Test
    fun `should stand on soft 17 if dealHitsOnSoft17 rule is not enabled`() {
        initTable(TableRule.DEFAULT.copy(dealerHitsOnSoft17 = false))
        val cards = listOf(
            Card.eight.spades,   // player 1st card
            Card.six.spades,   // dealer visible card
            Card.nine.hearts,   // player 2nd card
            Card.ace.diamonds, // dealer hidden card
        )
        prepareShoe(cards)

        every { strategy.nextMove(any(), any()) } returns Decision.STAND

        val playerHand = select(cards, 0, 2)
        val dealerHand = select(cards, 1, 3)

        table.newRound()

        verify { strategy.recordResult(Outcome.PUSH, 0.0, handMatch(playerHand), handMatch(dealerHand)) }

        assertEquals(initialAmount, account.balance())
        verify { strategy.dealerReceived(Card.six.spades) }
        verify { strategy.dealerCardVisible(Card.ace.diamonds) }
        verify { strategy.received(Card.eight.spades) }
        verify { strategy.received(Card.nine.hearts) }
    }

    @Test
    fun `should hit on soft 17 if dealHitsOnSoft17 rule is enabled`() {
        initTable()
        val cards = listOf(
            Card.eight.spades,   // player 1st card
            Card.six.spades,   // dealer visible card
            Card.nine.hearts,   // player 2nd card
            Card.ace.diamonds, // dealer hidden card
            Card.three.spades // dealer 3rd card
        )
        prepareShoe(cards)
        val playerCards = select(cards, 0, 2)
        val dealerCards = select(cards, 1,3,4)

        every { strategy.nextMove(any(), any()) } returns Decision.STAND

        table.newRound()

        verify {
            strategy.recordResult(
                Outcome.LOSS,
                minBet.toDouble(),
                handMatch(playerCards),
                handMatch(dealerCards)
            )
        }
        assertEquals(initialAmount - minBet, account.balance())
        verify { strategy.dealerReceived(Card.six.spades) }
        verify { strategy.dealerCardVisible(Card.ace.diamonds) }
        verify { strategy.dealerReceived(Card.three.spades) }
        verify { strategy.received(Card.eight.spades) }
        verify { strategy.received(Card.nine.hearts) }
    }
}
