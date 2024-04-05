package org.alc.card.impl

import org.alc.card.model.Card
import org.alc.card.model.GameShoe
import org.alc.card.model.Shuffler

open class DefaultGameShoeImpl(
    private val shuffler: Shuffler<Card> = RandomShuffler(),
) : GameShoe {

    private val shoe: ArrayDeque<Card> = ArrayDeque()

    override fun reset() {
        shoe.clear()
    }

    protected open fun getDeck() = Card.newDeck()

    override fun addDecks(nbDecks: Int, shuffled: Boolean) {
        assert(nbDecks > 0) { "Number of decks must be greater than zero" }
        val deck = getDeck()
        val initialSize = nbRemainingCards()
        repeat(nbDecks) { shoe.addAll(deck) }
        if (shuffled) {
            shuffler.shuffle(shoe, initialSize, nbRemainingCards())
        }
    }

    override fun shuffle() = shuffler.shuffle(shoe)

    override fun remainingCards() = shoe.toList()

    override fun nbRemainingCards() = shoe.size

    override fun dealCard() = shoe.removeFirstOrNull()

    /**
     * Deal specified number of cards. Returns empty list if there are less than nbCardsToDeal remaining in the deck.
     *
     */
    override fun dealCards(nbCardsToDeal: Int): List<Card> {
        assert(nbCardsToDeal >= 1) { "Number of cards to deal must be larger than or equal to 1" }
        if (nbCardsToDeal > nbRemainingCards()) {
            return shoe.take(0)
        }
        val result = shoe.take(nbCardsToDeal)
        shoe.subList(0, nbCardsToDeal).clear()
        return result
    }
}

