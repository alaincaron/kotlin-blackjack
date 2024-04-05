package org.alc.blackjack.model

interface Account {
    fun balance(): Double
    fun withdraw(value: Double)
    fun deposit(value: Double)
}
