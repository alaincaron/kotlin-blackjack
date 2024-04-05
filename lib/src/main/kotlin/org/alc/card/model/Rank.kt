package org.alc.card.model

enum class Rank {
    ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING;

    fun value(): Int {
        return if (ordinal in 0..8) ordinal + 1 else 10
    }
}
