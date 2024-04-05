package org.alc.card.model

import org.junit.jupiter.api.Test

class CardTest {

    @Test fun getCardTest() {
        var card = Card(Rank.KING, Suit.DIAMONDS)
        assert(Rank.KING == card.rank)
        assert(Suit.DIAMONDS == card.suit)

        card = Card(Rank.ACE, Suit.SPADES)
        assert(Rank.ACE == card.rank)
        assert(Suit.SPADES == card.suit)
    }

    @Test fun toStringTest() {
        var card = Card(Rank.FIVE, Suit.SPADES)
        assert(card.toString() == "FIVE of SPADES")

        card = Card(Rank.ACE, Suit.DIAMONDS)
        assert(card.toString() == "ACE of DIAMONDS")
    }
}
