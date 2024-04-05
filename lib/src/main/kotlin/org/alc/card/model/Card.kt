package org.alc.card.model

class Card private constructor(val rank: Rank, val suit: Suit) {
    companion object {
        private val ALL_CARDS =
            List(Rank.entries.size * Suit.entries.size) {
                val r = it / Suit.entries.size
                val s = it % Suit.entries.size
                Card(Rank.entries[r], Suit.entries[s])
            }

        operator fun invoke(rank: Rank, suit: Suit): Card = ALL_CARDS[rank.ordinal * Suit.entries.size + suit.ordinal]

        fun newDeck() = ALL_CARDS
        fun newDeck(predicate: (Card) -> Boolean) = ALL_CARDS.filter(predicate)
    }

    override fun toString() = "$rank of $suit"
}
