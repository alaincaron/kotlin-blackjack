package org.alc.card.model


interface GameShoe {
    fun reset()
    fun addDecks(nbDecks: Int, shuffled: Boolean = false)
    fun shuffle()
    fun nbRemainingCards(): Int
    fun remainingCards(): List<Card>
    fun dealCards(nbCardsToDeal: Int = 1): List<Card>
    fun dealCard(): Card?
}
