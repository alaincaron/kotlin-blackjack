package org.alc.blackjack.impl

import org.alc.blackjack.model.Account
import org.alc.blackjack.model.Decision
import org.alc.blackjack.model.Hand
import org.alc.card.model.Card


open class InteractiveStrategy(account: Account) : AbstractStrategy(account) {
    private fun parseBoolean(str: String): Boolean? = when (str) {
        "y", "yes" -> true
        "n", "no" -> false
        else -> null
    }

    override fun insurance(hand: Hand) = queryUser(
        {
            println("hand total = ${hand.score()}")
            print("insurance? (Y/N)> ")
        },
        ::parseBoolean
    )

    override fun equalPayment() =
        queryUser({ print("BlackJack equal payment? (Y/N)> ") }, ::parseBoolean)

    private fun <T> queryUser(prompt: () -> Unit, f: (String) -> T?): T {
        var result: T? = null
        while (result == null) {
            prompt()
            val str = readln().trim()
            when (str.lowercase()) {
                "stat", "stats" -> printStats { println(it) }
                "balance" -> println("balance = ${account.balance()}")
                "rules" -> println("rules = ${table().rule}")
                else -> result = f(str)
            }
        }
        return result
    }

    override fun initialBet(): Int =
        queryUser(
            { print("Initial bet: between ${table().minBet} and ${table().maxBet}, or 0 to quit. > ") },
            { reply: String ->
                var result: Int? = null
                try {
                    val bet = reply.toInt()
                    if (bet == 0 || (bet >= table().minBet && bet <= table().maxBet)) result = bet
                } catch (e: NumberFormatException) {
                    // ignore
                }
                result
            }
        )

    override fun nextMove(hand: Hand, dealerCard: Card) = nextMove(hand, dealerCard, null)

     protected fun nextMove(hand: Hand, dealerCard: Card, hint: Decision? = null): Decision {
        val msg = printMsg(hand, hint)
        return queryUser(
            {
                println("Dealer's visible card is ${dealerCard.rank}")
                print(msg)
            },
            { reply: String ->
                when (reply) {
                    "hit" -> Decision.HIT
                    "stand" -> Decision.STAND
                    "split" -> if (hand.canBeSplit()) Decision.SPLIT else null
                    "double" -> if (hand.canBeDoubled(table().rule)) Decision.DOUBLE else null
                    "surrender" -> if (hand.canSurrender() && table().rule.allowSurrender) Decision.SURRENDER else null
                    "hint" -> hint?.run {
                        println("Hint : ${toString().lowercase()}")
                        null
                    }
                    "show" -> hand.run {
                        println("Hand : $this")
                        null
                    }
                    else -> null
                }
            }
        )
    }

    private fun printMsg(hand: Hand, hint: Decision? = null): String {
        val builder = StringBuilder("You have ")
        val score = hand.score()
        if (hand.canBeSplit()) {
            val firstCard = hand.getCard(0)
            val secondCard = hand.getCard(1)
            if (firstCard.rank == secondCard.rank) {
                builder.append("a pair of ").append(firstCard.rank)
            } else {
                builder.append("a ").append(firstCard.rank).append(" and a ").append(secondCard.rank)
            }
            builder.append(" for a total of ")
        } else {
            builder.append("a total of ")
        }
        if (hand.isSoft()) {
            builder.append("(${score - 10}|$score)")
        } else {
            builder.append(score)
        }
        builder.append(" stand")
        if (hand.canBeHit()) builder.append("|hit")
        if (table().canSplit(account, hand)) builder.append("|split")
        if (table().canDouble(account,hand)) builder.append("|double")
        if (table().canSurrender(hand)) builder.append("|surrender")
        if (hint != null) builder.append("|hint")
        builder.append(" > ")

        return builder.toString()
    }

    override fun received(card: Card): Unit =
        println("Player received $card")

    override fun dealerReceived(card: Card): Unit =
        println("Dealer received $card")

    override fun dealerCardVisible(card: Card): Unit =
        println("Dealer turns card: $card")

    override fun finalHand(hand: Hand) {
        if (hand.isBusted()) println("Player busted with ${hand.score()}")
        else if (hand.isBlackJack()) println("Player got blackjack")
        else if (hand.surrendered()) println("Player surrendered with ${hand.score()}")
        else println("Player score is ${hand.score()}")
    }

    override fun finalDealerHand(hand: Hand) {
        if (hand.isBusted()) println("Dealer busted with ${hand.score()}")
        else if (hand.isBlackJack()) println("Dealer got blackjack")
        else println("Dealer score is ${hand.score()}")
    }

    override fun recordPush() {
        super.recordPush()
        println("PUSH")
    }

    override fun recordWin(amount: Double) {
        super.recordWin(amount)
        println("Player won $amount")
    }

    override fun recordLoss(amount: Double) {
        super.recordLoss(amount)
        println("Player lost $amount")
    }


}
