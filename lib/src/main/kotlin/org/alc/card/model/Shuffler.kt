package org.alc.card.model

interface Shuffler<T> {
    fun shuffle(list: MutableList<T>, start: Int, end: Int)
    fun shuffle(list: MutableList<T>) = shuffle(list, 0, list.size)
    fun shuffle(list: MutableList<T>, start: Int) = shuffle(list, start, list.size)
}
