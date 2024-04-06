package org.alc.card.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CardTest {

    @Test fun getCardTest() {
        var card = Card(Rank.KING, Suit.DIAMONDS)
        assertSame(Rank.KING, card.rank)
        assertSame(Suit.DIAMONDS, card.suit)
        assertSame(card, Card.king.diamonds)

        card = Card(Rank.ACE, Suit.SPADES)
        assertSame(Rank.ACE, card.rank)
        assertSame(Suit.SPADES, card.suit)
        assertSame(card, Card.ace.spades)
    }

    @Test fun toStringTest() {
        var card = Card.five.spades
        assertEquals("FIVE of SPADES", card.toString())

        card = Card.ace.diamonds
        assertEquals("ACE of DIAMONDS", card.toString())
    }
}
