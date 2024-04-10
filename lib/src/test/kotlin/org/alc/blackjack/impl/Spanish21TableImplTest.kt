package org.alc.blackjack.impl

import io.mockk.*
import org.alc.blackjack.model.*
import org.alc.card.model.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class Spanish21TableImplTest: TableImplTestHelper(TableRule.SPANISH21) {

    @Test
    fun `should not offer equal payment on visible ace and pay regular blackjack`() {
        initTable()
        prepareShoe(
            Card.ace.spades, // player 1st card
            Card.ace.spades, // dealer visible card
            Card.king.hearts,  // player 2nd card
            Card.jack.diamonds // dealer hidden card
        )

        table.newRound()

        verify(exactly = 0) { strategy.equalPayment() }
        verify { strategy.recordWin(minBet * table.rule.blackjackPayFactor) }
        assertEquals(initialAmount + minBet * table.rule.blackjackPayFactor, account.balance())
        verify(exactly = 0) { strategy.nextMove(any(), any()) }
    }

    @Test
    fun `should allow to double or hit on split of pair of aces`() {
        initTable()
        prepareShoe(
            Card.ace.spades,   // player 1st card
            Card.six.spades,   // dealer visible card
            Card.ace.hearts,   // player 2nd card
            Card.six.diamonds, // dealer hidden card
            Card.three.diamonds, // player 2nd card, 1st hand
            Card.seven.hearts,    // player 3rd card, 1st hand
            Card.six.diamonds, // player 2nd card, 2nd hand
            Card.three.spades,   // player 3rd, 3rd hand
            Card.nine.diamonds // dealer 3rd card
        )
        every { strategy.nextMove(any(), any()) } returnsMany listOf(
            Decision.SPLIT,
            Decision.DOUBLE,
            Decision.HIT,
            Decision.STAND
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

    @Test
    fun `should pay super-7 bonus`() {
        initTable()
        prepareShoe(
            Card.seven.spades, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.seven.spades, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.seven.spades, // player 3rd card
        )

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordWin(1000.0 + minBet * 3) }
        assertEquals(initialAmount + 1000.0 + minBet * 3, account.balance())
    }

    @Test
    fun `should pay suited-7 bonus`() {
        initTable()
        prepareShoe(
            Card.seven.clubs, // player 1st card
            Card.jack.diamonds, // dealer 1st card
            Card.seven.clubs, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.seven.clubs, // player 3rd card
        )

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordWin( minBet * 2.0) }
        assertEquals(initialAmount + minBet * 2.0, account.balance())
    }

    @Test
    fun `should pay non-suited-7 bonus`() {
        initTable()
        prepareShoe(
            Card.seven.clubs, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.seven.clubs, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.seven.diamonds, // player 3rd card
        )

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordWin( minBet * 1.5) }
        assertEquals(initialAmount + minBet * 1.5, account.balance())
    }

    @Test
    fun `should pay spade sequence bonus`() {
        initTable()
        prepareShoe(
            Card.seven.spades, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.eight.spades, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.six.spades, // player 3rd card
        )

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordWin( minBet * 3.0) }
        assertEquals(initialAmount + minBet * 3.0, account.balance())
    }

    @Test
    fun `should pay suited sequence bonus`() {
        initTable()
        prepareShoe(
            Card.seven.diamonds, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.six.diamonds, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.eight.diamonds, // player 3rd card
        )

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordWin( minBet * 2.0) }
        assertEquals(initialAmount + minBet * 2.0, account.balance())
    }

    @Test
    fun `should pay unsuited sequence bonus`() {
        initTable()
        prepareShoe(
            Card.eight.diamonds, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.six.diamonds, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.seven.clubs, // player 3rd card
        )

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordWin( minBet * 1.5) }
        assertEquals(initialAmount + minBet * 1.5, account.balance())
    }

    @Test
    fun `should pay five card bonus`() {
        initTable()
        prepareShoe(
            Card.two.diamonds, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.three.diamonds, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.four.clubs, // player 3rd card
            Card.six.clubs, // player 4th card
            Card.six.clubs, // player 5th card
        )

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordWin( minBet * 1.5) }
        assertEquals(initialAmount + minBet * 1.5, account.balance())
    }
    @Test
    fun `should pay six card bonus`() {
        initTable()
        prepareShoe(
            Card.two.diamonds, // player 1st card
            Card.seven.diamonds, // dealer 1st card
            Card.three.diamonds, // player 2nd card
            Card.two.diamonds, // dealer 2nd card
            Card.four.clubs, // player 3rd card
            Card.three.clubs, // player 4th card
            Card.three.clubs, // player 5th card
            Card.six.diamonds // player 6th card
        )

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordWin( minBet * 2.0) }
        assertEquals(initialAmount + minBet * 2.0, account.balance())
    }

    @Test
    fun `should pay seven card bonus`() {
        initTable()
        prepareShoe(
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

        every { strategy.nextMove(any(), any()) } returns Decision.HIT

        table.newRound()

        verify { strategy.recordWin( minBet * 3.0) }
        assertEquals(initialAmount + minBet * 3.0, account.balance())
    }
}
