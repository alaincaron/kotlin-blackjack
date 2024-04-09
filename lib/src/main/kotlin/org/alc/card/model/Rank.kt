package org.alc.card.model

enum class Rank {
    ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING;

    val value get() = when (ordinal) {
        0 -> 11
        in 1..8 -> ordinal + 1
        else -> 10
    }
}
