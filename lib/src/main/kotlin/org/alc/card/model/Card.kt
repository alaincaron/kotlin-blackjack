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

        class Builder internal constructor(private val rank: Rank) {
            val hearts: Card get() = invoke(rank, Suit.HEARTS)
            val diamonds: Card get() = invoke(rank, Suit.DIAMONDS)
            val clubs: Card get() = invoke(rank, Suit.CLUBS)
            val spades: Card get() = invoke(rank, Suit.SPADES)
        }

        val ace: Builder get() = Builder(Rank.ACE)
        val two: Builder get() = Builder(Rank.TWO)
        val three: Builder get() = Builder(Rank.THREE)
        val four: Builder get() = Builder(Rank.FOUR)
        val five: Builder get() = Builder(Rank.FIVE)
        val six: Builder get() = Builder(Rank.SIX)
        val seven: Builder get() = Builder(Rank.SEVEN)
        val eight: Builder get() = Builder(Rank.EIGHT)
        val nine: Builder get() = Builder(Rank.NINE)
        val ten: Builder get() = Builder(Rank.TEN)
        val jack: Builder get() = Builder(Rank.JACK)
        val queen: Builder get() = Builder(Rank.QUEEN)
        val king: Builder get() = Builder(Rank.KING)
    }

    override fun toString() = "$rank of $suit"
    val value get() = rank.value()
}
