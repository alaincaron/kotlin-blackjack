package org.alc.card.impl

import org.alc.card.model.Card
import org.alc.card.model.Rank
import org.alc.card.model.Shuffler

class Spanish21GameShoeImpl(shuffler: Shuffler<Card> = RandomShuffler())
    : DefaultGameShoeImpl(shuffler) {
        override fun getDeck() = Card.newDeck { card -> card.rank != Rank.TEN }
    }

