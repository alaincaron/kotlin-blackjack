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

        verifyPlayerCards(strategy, playerCards)
        verifyDealerCards(strategy, dealerCards)

        verify { strategy.recordResult(Outcome.PUSH, 0.0, handMatch(playerCards), handMatch(dealerCards)) }
        assertEquals(initialAmount, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
        verify {
            strategy.finalHand(handMatch(playerCards))
            strategy.finalDealerHand(handMatch(dealerCards), DealerResult.BLACKJACK)
        }
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
        val dealerCards = select(cards, 1, 3)
        prepareShoe(cards)

        every { strategy.equalPayment() } returns true

        table.newRound()

        verifyPlayerCards(strategy, playerCards)
        verifyDealerCards(strategy, dealerCards)

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
        verify {
            strategy.finalHand(handMatch(playerCards))
            strategy.finalDealerHand(handMatch(dealerCards), DealerResult.BLACKJACK)
        }
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

        val playerCards = select(cards, 0, 2)
        val dealerCards = select(cards, 1, 3)

        prepareShoe(cards)

        every { strategy.equalPayment() } returns true

        table.newRound()

        verifyPlayerCards(strategy, playerCards)
        verifyDealerCards(strategy, dealerCards)

        verify {
            strategy.recordResult(
                Outcome.BLACKJACK_EQUAL_PAYMENT,
                minBet.toDouble(),
                handMatch(playerCards),
                handMatch(dealerCards[0])
            )
        }
        assertEquals(initialAmount + minBet, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }

        verify {
            strategy.finalHand(handMatch(playerCards))
            strategy.finalDealerHand(handMatch(dealerCards), DealerResult.NO_PULL)
        }
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
        val dealerCards = select(cards, 1, 3)
        every { strategy.equalPayment() } returns false

        table.newRound()

        verifyPlayerCards(strategy, playerCards)
        verifyDealerCards(strategy, dealerCards)

        verify {
            strategy.recordResult(
                Outcome.BLACKJACK,
                minBet * 1.5,
                handMatch(playerCards),
                handMatch(dealerCards[0])
            )
        }
        assertEquals(initialAmount + minBet * 1.5, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
        verify {
            strategy.finalHand(handMatch(playerCards))
            strategy.finalDealerHand(handMatch(dealerCards), DealerResult.NO_PULL)
        }
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
        val dealerCards = select(cards, 1 ,3, 6)
        val playerInitialCards = select(cards, 0, 2)
        val playerFirstHandCards = select(cards, 0, 4)
        val playerSecondHandCards = select(cards, 2, 5)
        every { strategy.nextMove(handMatch(playerInitialCards), dealerCards[0]) } returns Decision.SPLIT

        table.newRound()

        verifyPlayerCards(strategy, select(cards, 0, 2, 4, 5))
        verifyDealerCards(strategy, dealerCards)

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
        verify { strategy.finalDealerHand(handMatch(dealerCards), DealerResult.STAND) }
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

        val playerFirstHand = select(cards, 0, 4, 5)
        val playerSecondHand = select(cards, 2, 6, 7)
        val dealerHand = select(cards, 1, 3, 8)

        prepareShoe(cards)
        every { strategy.nextMove(any(), any()) } returnsMany listOf(
            Decision.SPLIT,
            Decision.DOUBLE,
            Decision.HIT
        )

        table.newRound()

        verifyPlayerCards(strategy, select(cards, 0, 2, 4, 5))
        verifyDealerCards(strategy, dealerHand)

        verify { strategy.recordResult(Outcome.DOUBLE_WIN, minBet * 2.0, handMatch(playerFirstHand), handMatch(dealerHand)) }
        verify {
            strategy.recordResult(
                Outcome.BUST,
                minBet.toDouble(),
                handMatch(playerSecondHand),
                handMatch(dealerHand[0])
            )
        }
        assertEquals(initialAmount + minBet, account.balance())
        verifyOrder {
            strategy.finalHand(handMatch(playerFirstHand))
            strategy.finalHand(handMatch(playerSecondHand))
        }
        verify { strategy.finalDealerHand(handMatch(dealerHand), DealerResult.STAND) }
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

        val playerHand = select(cards, 0,2)
        val dealerHand = select(cards, 1, 3)

        prepareShoe(cards)

        every { strategy.nextMove(any(), any()) } returns Decision.STAND

        table.newRound()

        verifyPlayerCards(strategy, playerHand)
        verifyDealerCards(strategy, dealerHand)

        verify { strategy.recordResult(Outcome.PUSH, 0.0, handMatch(playerHand), handMatch(dealerHand)) }

        assertEquals(initialAmount, account.balance())

        verify { strategy.finalHand(handMatch(playerHand))}
        verify { strategy.finalDealerHand(handMatch(dealerHand), DealerResult.STAND) }
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
        val dealerCards = select(cards, 1, 3, 4)

        every { strategy.nextMove(any(), any()) } returns Decision.STAND

        table.newRound()

        verifyPlayerCards(strategy, playerCards)
        verifyDealerCards(strategy, dealerCards)

        verify {
            strategy.recordResult(
                Outcome.LOSS,
                minBet.toDouble(),
                handMatch(playerCards),
                handMatch(dealerCards)
            )
        }
        assertEquals(initialAmount - minBet, account.balance())
        verify {
            strategy.finalHand(handMatch(playerCards))
            strategy.finalDealerHand(handMatch(dealerCards), DealerResult.STAND)
        }
    }
}
