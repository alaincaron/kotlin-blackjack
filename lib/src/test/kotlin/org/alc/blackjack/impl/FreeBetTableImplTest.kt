package org.alc.blackjack.impl

import io.mockk.every
import io.mockk.verify
import org.alc.blackjack.model.Decision
import org.alc.blackjack.model.TableRule
import org.alc.card.model.Card
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FreeBetTableImplTest : TableImplTestHelper(TableRule.FREE_BET) {

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

        verify(exactly = 1) { strategy.recordWin(minBet.toDouble()) }
        verify(exactly = 1) { strategy.recordLoss(0.0) } // loss on free-hand
        assertEquals(initialAmount + minBet, account.balance())
        verify { strategy.finalHand(handMatch(playerFirstHandCards)) }
        verify { strategy.finalHand(handMatch(playerSecondHandCards)) }
        verify { strategy.finalDealerHand(handMatch(dealerCards)) }
    }

    @Test
    fun `should allow to double or hit on split of pair of non-aces`() {
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
        verify { strategy.recordLoss(0.0) }
        assertEquals(initialAmount + 2 * minBet, account.balance())
        verify(exactly = 2) { strategy.finalHand(any()) }
        verify { strategy.finalDealerHand(any()) }
    }

}
