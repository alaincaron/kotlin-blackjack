package org.alc.blackjack.model

class Player(private val strategy: Strategy) : Strategy by strategy, Account by strategy.account

