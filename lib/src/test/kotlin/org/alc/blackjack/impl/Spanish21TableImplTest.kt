package org.alc.blackjack.impl

import io.mockk.*
import org.alc.blackjack.model.*
import org.alc.card.model.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class Spanish21TableImplTest : TableImplTestHelper(TableRule.SPANISH21) {

    @Test
    fun `should not offer equal payment on visible ace and pay regular blackjack`() {
        initTable()

        val cards = listOf(
            Card.ace.spades, // player 1st card
            Card.ace.spades, // dealer visible card
            Card.king.hearts,  // player 2nd card
            Card.jack.diamonds // dealer hidden card
        )

        prepareShoe(cards)

        val playerHand = select(cards,0,2)
        val dealerHand = select(cards,1,3)

        table.newRound()
        val gain = minBet * table.rule.blackjackPayFactor
        verify(exactly = 0) { strategy.equalPayment() }
        verify { strategy.recordResult(Outcome.BLACKJACK, gain, handMatch(playerHand), handMatch(dealerHand))}
        assertEquals(initialAmount + gain, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should allow to double or hit on split of pair of aces`() {
        initTable()
        val cards = listOf(
            Card.ace.spades,   // player 1st card
            Card.six.spades,   // dealer visible card
            Card.ace.hearts,   // player 2nd card
            Card.six.diamonds, // dealer hidden card
            Card.three.diamonds, // player 2nd card, 1st hand
            Card.seven.hearts,    // player 3rd card, 1st hand
            Card.six.diamonds, // player 2nd card, 2nd hand
            Card.three.spades,   // player 3rd card, 2nd hand
            Card.nine.diamonds // dealer 3rd card
        )
        prepareShoe(cards)

        val playerFirstHand = select(cards,0,4,5)
        val playerSecondHand = select(cards,2,6,7)
        val dealerHand = select(cards,1,3,8)
        every { strategy.nextMove(any(), any()) } returnsMany listOf(
            Decision.SPLIT,
            Decision.DOUBLE,
            Decision.HIT,
            Decision.STAND
        )

        table.newRound()

        verify { strategy.recordResult(Outcome.DOUBLE_WIN, minBet * 2.0, handMatch(playerFirstHand), null) }
        verify { strategy.recordResult(Outcome.LOSS, minBet.toDouble(), handMatch(playerSecondHand), handMatch(dealerHand))}
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
        val playerHand = select(cards,0,2)
        val dealerHand = select(cards,1,3)
        every { strategy.nextMove(any(), any()) } returns Decision.STAND

        table.newRound()

        verify { strategy.recordResult(Outcome.PUSH, 0.0, handMatch(playerHand), handMatch(dealerHand))}
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
            Card.three.spades
        )
        prepareShoe(cards)

        val playerHand = select(cards, 0,2)
        val dealerHand = select(cards, 1,3,4)
        every { strategy.nextMove(any(), any()) } returns Decision.STAND

        table.newRound()

        verify { strategy.recordResult(Outcome.LOSS, minBet.toDouble(), handMatch(playerHand), handMatch(dealerHand))}
        assertEquals(initialAmount - minBet, account.balance())
        verify { strategy.dealerReceived(Card.six.spades) }
        verify { strategy.dealerCardVisible(Card.ace.diamonds) }
        verify { strategy.dealerReceived(Card.three.spades) }
        verify { strategy.received(Card.eight.spades) }
        verify { strategy.received(Card.nine.hearts) }
    }

    @Test
    fun `should pay super-7 bonus`() {
        initTable()
        val cards = listOf(
            Card.seven.spades, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.seven.spades, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.seven.spades, // player 3rd card
        )
        prepareShoe(cards)

        val playerHand = select(cards, 0, 2, 4)

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordResult(Outcome.SUPER_7_BONUS, 1000.0 + minBet * 3.0, handMatch(playerHand),null )}
        assertEquals(initialAmount + 1000.0 + minBet * 3, account.balance())
    }

    @Test
    fun `should pay suited-7 bonus`() {
        initTable()
        val cards = listOf(
            Card.seven.clubs, // player 1st card
            Card.jack.diamonds, // dealer 1st card
            Card.seven.clubs, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.seven.clubs, // player 3rd card
        )
        prepareShoe(cards)
        val playerHand = select(cards, 0, 2, 4)

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordResult(Outcome.BONUS_21_2, minBet * 2.0, handMatch(playerHand),null )}
        assertEquals(initialAmount + minBet * 2.0, account.balance())
    }

    @Test
    fun `should pay non-suited-7 bonus`() {
        initTable()
        val cards = listOf(
            Card.seven.clubs, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.seven.clubs, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.seven.diamonds, // player 3rd card
        )
        prepareShoe(cards)
        val playerHand = select(cards, 0, 2, 4)

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordResult(Outcome.BONUS_21, minBet * 1.5, handMatch(playerHand),null )}
        assertEquals(initialAmount + minBet * 1.5, account.balance())
    }

    @Test
    fun `should pay spade sequence bonus`() {
        initTable()
        val cards = listOf(
            Card.seven.spades, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.eight.spades, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.six.spades, // player 3rd card
        )
        prepareShoe(cards)
        val playerHand = select(cards, 0, 2, 4)

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordResult(Outcome.BONUS_21_3, minBet * 3.0, handMatch(playerHand),null )}
        assertEquals(initialAmount + minBet * 3.0, account.balance())
    }

    @Test
    fun `should pay suited sequence bonus`() {
        initTable()
        val cards = listOf(
            Card.seven.diamonds, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.six.diamonds, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.eight.diamonds, // player 3rd card
        )
        prepareShoe(cards)
        val playerHand = select(cards, 0,2,4)

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordResult(Outcome.BONUS_21_2, minBet * 2.0, handMatch(playerHand), null) }
        assertEquals(initialAmount + minBet * 2.0, account.balance())
    }

    @Test
    fun `should pay unsuited sequence bonus`() {
        initTable()
        val cards = listOf(
            Card.eight.diamonds, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.six.diamonds, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.seven.clubs, // player 3rd card
        )
        prepareShoe(cards)
        val playerHand = select(cards, 0,2,4)

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordResult(Outcome.BONUS_21,minBet * 1.5, handMatch(playerHand), null) }
        assertEquals(initialAmount + minBet * 1.5, account.balance())
    }

    @Test
    fun `should pay five card bonus`() {
        initTable()
        val cards = listOf(
            Card.two.diamonds, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.three.diamonds, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.four.clubs, // player 3rd card
            Card.six.clubs, // player 4th card
            Card.six.clubs, // player 5th card
        )
        prepareShoe(cards)
        val playerHand = select(cards,0,2,4,5,6)

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordResult(Outcome.BONUS_21, minBet * 1.5, handMatch(playerHand), null) }
        assertEquals(initialAmount + minBet * 1.5, account.balance())
    }

    @Test
    fun `should pay six card bonus`() {
        initTable()
        val cards = listOf(
            Card.two.diamonds, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.three.diamonds, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.four.clubs, // player 3rd card
            Card.three.clubs, // player 4th card
            Card.three.clubs, // player 5th card
            Card.six.diamonds // player 6th card
        )
        prepareShoe(cards)
        val playerHand = select(cards, 0, 2,4,5,6,7)
        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordResult(Outcome.BONUS_21_2, minBet * 2.0, handMatch(playerHand), null) }
        assertEquals(initialAmount + minBet * 2.0, account.balance())
    }

    @Test
    fun `should pay seven card bonus`() {
        initTable()
        val cards = listOf(
            Card.two.diamonds, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.three.diamonds, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.four.clubs, // player 3rd card
            Card.three.clubs, // player 4th card
            Card.three.clubs, // player 5th card
            Card.ace.diamonds, // player 6th card
            Card.five.spades  // player 7th card
        )
        prepareShoe(cards)
        val playerHand = select(cards, 0, 2,4,5,6,7,8)


        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordResult(Outcome.BONUS_21_3, minBet * 3.0, handMatch(playerHand), null) }
        assertEquals(initialAmount + minBet * 3.0, account.balance())
    }
}
