package org.alc.blackjack.impl

import io.mockk.*
import org.alc.blackjack.model.*
import org.alc.card.model.Card
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FreeBetTableImplTest : TableImplTestHelper(TableRule.FREE_BET) {

    @Test
    fun `should allow to split pair ACES only once`() {
        initTable()

        val cards = listOf(
            Card.ace.spades,   // player 1st card
            Card.six.spades,   // dealer visible card
            Card.ace.hearts,   // player 2nd card
            Card.six.diamonds, // dealer hidden card
            Card.ten.diamonds, // player 2nd card, 1st hand
            Card.ace.diamonds, // player 2nd card, 2nd hand
            Card.five.spades   // dealer 3rd card
        )
        prepareShoe(cards)
        val dealerCards = select(cards,1,3,6)
        val dealerCard = dealerCards.first()
        val playerInitialHand = select(cards, 0, 2)
        val playerFirstHandCards = select(cards, 0, 4)
        val playerSecondHandCards = select(cards, 2, 5)
        every { strategy.nextMove(handMatch(playerInitialHand), dealerCard) } returns Decision.SPLIT

        table.newRound()

        verify(exactly = 1) { strategy.recordResult(Outcome.WIN, minBet.toDouble(), handMatch(playerFirstHandCards), handMatch(dealerCards))}
        verify(exactly = 1) { strategy.recordResult(Outcome.LOSS, 0.0, handMatch(playerSecondHandCards), handMatch(dealerCards))}
        assertEquals(initialAmount + minBet, account.balance())
        verify { strategy.finalHand(handMatch(playerFirstHandCards)) }
        verify { strategy.finalHand(handMatch(playerSecondHandCards)) }
        verify { strategy.finalDealerHand(handMatch(dealerCards)) }
    }

    @Test
    fun `should allow to double or hit on split of pair of non-aces`() {
        initTable()
        val cards = listOf(
            Card.eight.spades,   // player 1st card
            Card.six.spades,   // dealer visible card
            Card.eight.hearts,   // player 2nd card
            Card.six.diamonds, // dealer hidden card
            Card.three.diamonds, // player 2nd card, 1st hand
            Card.ten.hearts,    // player 3rd card, 1st hand
            Card.six.diamonds, // player 2nd card, 2nd hand
            Card.ten.spades,   // player 3rd card, 2nd hand
            Card.five.spades   // dealer 3rd card
        )
        prepareShoe(cards)

        val playerFirstHandCards = select(cards, 0, 4, 5)
        val playerSecondHandCards = select(cards, 2, 6, 7 )
        val dealerCards = select(cards, 1, 3, 8)
        every { strategy.nextMove(any(), any()) } returnsMany listOf(
            Decision.SPLIT,
            Decision.DOUBLE,
            Decision.HIT
        )

        table.newRound()

        verify { strategy.recordResult(Outcome.DOUBLE_WIN, minBet * 2.0, handMatch(playerFirstHandCards), handMatch(dealerCards))}
        verify { strategy.recordResult(Outcome.BUST, 0.0,handMatch(playerSecondHandCards), null)}
        assertEquals(initialAmount + 2 * minBet, account.balance())
        verify { strategy.finalHand(handMatch(playerFirstHandCards))}
        verify { strategy.finalHand(handMatch(playerSecondHandCards))}
        verify { strategy.finalDealerHand(handMatch(dealerCards)) }
        verify { strategy.dealerCardVisible(dealerCards[1])}
        verifyOrder {
            strategy.dealerReceived(dealerCards[0])
            strategy.dealerReceived(dealerCards[2])
        }
    }

}
