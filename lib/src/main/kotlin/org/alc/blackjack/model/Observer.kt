package org.alc.blackjack.model

import org.alc.card.model.*

interface Observer {
    fun recordResult(outcome: Outcome, amount: Double, playerHand: Hand, dealerHand: Hand?)
    fun cardDealt(card: Card)
    fun deckShuffled()
    fun received(card: Card)
    fun dealerReceived(card: Card)
    fun dealerCardVisible(card: Card)
    fun finalHand(hand: Hand)
    fun finalDealerHand(hand: Hand)
}
