package org.alc.blackjack.impl

import org.alc.blackjack.model.Decision
import org.alc.blackjack.model.Hand
import org.alc.blackjack.model.Strategy
import org.alc.blackjack.model.Table
import org.alc.card.model.Card

class TrainingStrategy(private val referenceStrategy: Strategy) : InteractiveStrategy(referenceStrategy.account) {
    private var nbDecisions = 0
    private var nbErrors = 0

    override fun nextMove(hand: Hand, dealerCard: Card): Decision {
        val optimal = referenceStrategy.nextMove(hand, dealerCard)
        val result = nextMove(hand, dealerCard, optimal)
        nbDecisions += 1
        if (result != optimal) {
            nbErrors += 1
            println("Wrong decision: $optimal is a better choice.")
        }
        return result
    }

    override fun enteredTable(table: Table){
        super.enteredTable(table)
        referenceStrategy.enteredTable(table)
    }


    override fun leftTable(table: Table){
        super.leftTable(table)
        referenceStrategy.leftTable(table)
        nbDecisions = 0
        nbErrors = 0
    }

    override fun printStats(out: (String) -> Unit) {
        super.printStats(out)
        out("nbDecisions = $nbDecisions")
        out("nbErrors = $nbErrors")
        out("decisionRate = ${(nbDecisions - nbErrors).toDouble() / nbDecisions}")
    }
}
