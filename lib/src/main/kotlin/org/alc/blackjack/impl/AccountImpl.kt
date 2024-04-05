package org.alc.blackjack.impl

import org.alc.blackjack.model.Account

class AccountImpl(amount: Double) : Account {
    private var _balance: Double = amount

    override fun balance() = _balance

    override fun withdraw(value: Double) {
        _balance -= value
    }

    override fun deposit(value: Double) {
        _balance += value
    }
}

