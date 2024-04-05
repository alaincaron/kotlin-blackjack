package org.alc.card.impl

import org.alc.card.model.Shuffler
import java.security.SecureRandom
import java.util.*

class RandomShuffler<T>(private val random: Random) : Shuffler<T> {
    constructor() : this(SecureRandom())

    override fun shuffle(list: MutableList<T>, start: Int, end: Int) =
        list.subList(start, end).shuffle(random)

    override fun shuffle(list: MutableList<T>) =
        list.shuffle(random)
}
