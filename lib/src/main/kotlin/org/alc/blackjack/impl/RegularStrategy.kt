package org.alc.blackjack.impl

import org.alc.blackjack.model.Account
import org.alc.blackjack.model.Decision
import org.alc.blackjack.model.Hand
import org.alc.blackjack.model.Table
import org.alc.card.model.Card

open class RegularStrategy(account: Account, gainFactor: Double? = null) : DefaultStrategy(account, gainFactor) {

    private fun canSplit(hand: Hand) = hand.canBeSplit() && account.balance() >= hand.initialBet
    private fun canDouble(hand: Hand) = hand.canBeDoubled(table().rule) && account.balance() >= hand.initialBet

    override fun shouldSplit(hand: Hand, dealerCard: Card): Boolean {
        if (!canSplit(hand)) return false
        if (hand.isSoft()) return true
        val rule = table().rule
        val splitTable = if (rule.dealerHitsOnSoft17) splitTableH else splitTableS
        val idx = (hand.score() / 2) - 2
        val code = splitTable[idx][dealerCard.value -2]
        return code == 'Y' || (code == 'p' && !rule.allowSurrender)
    }

    override fun softDecision(hand: Hand, dealerCard: Card): Decision {
        val softTable = if (table().rule.dealerHitsOnSoft17) softTableH else softTableS
        return toDecision(softTable[hand.score() - 12][dealerCard.value - 2], hand)
    }

    override fun hardDecision(hand: Hand, dealerCard: Card): Decision {
        val score = hand.score()
        if (score > 17) return Decision.STAND
        if (score <= 8) return Decision.HIT
        val hardTable = if (table().rule.dealerHitsOnSoft17) hardTableH else hardTableS
        return toDecision(hardTable[score - 9][dealerCard.value -2], hand)
    }

    private fun toDecision(c: Char, hand: Hand): Decision = when (c) {
        'H' -> Decision.HIT
        'S' -> Decision.STAND
        'D' -> if (canDouble(hand)) Decision.DOUBLE else Decision.HIT
        'd' -> if (canDouble(hand)) Decision.DOUBLE else Decision.STAND
        'R' -> if (table().rule.allowSurrender) Decision.SURRENDER else Decision.HIT
        'r' -> if (table().rule.allowSurrender) Decision.SURRENDER else Decision.STAND
        else -> throw IllegalArgumentException("Invalid decision code: $c")
    }

    @Suppress("SpellCheckingInspection")
    companion object {

        //
        // H: HIT
        // S: STAND
        // D: Double if allowed, hit otherwise
        // d: Double if allowed, stand otherwise
        // R: Surrender if allowed, hit otherwise
        // r: Surrender if allowed, stand otherwise
        // p: Split if surrender is allowed, defer otherwise
        private val splitTableH by lazy {
            arrayOf(
                "YYYYYYNNNN", /* 2 */
                "YYYYYYNNNN", /* 3 */
                "NNNYYNNNNN", /* 4 */
                "NNNNNNNNNN", /* 5 */
                "YYYYYNNNNN", /* 6 */
                "YYYYYYNNNN", /* 7 */
                "YYYYYYYYYp", /* 8 */
                "YYYYYNYYNN", /* 9 */
                "NNNNNNNNNN", /* 10 */
            )
        }

        //noinspection SpellCheckingInspection
        private val softTableH by lazy {
            arrayOf(
                "HHHDDHHHHH", /* A */
                "HHHDDHHHHH", /* 2 */
                "HHHDDHHHHH", /* 3 */
                "HHDDDHHHHH", /* 4 */
                "HHDDDHHHHH", /* 5 */
                "HDDDDHHHHH", /* 6 */
                "dddddSSHHH", /* 7 */
                "SSSSdSSSSS", /* 8 */
                "SSSSSSSSSS"  /* 9 */
            )
        }

        //noinspection SpellCheckingInspection
        private val hardTableH by lazy {
            arrayOf(
                "HDDDDHHHHH", /* 9 */
                "DDDDDDDDHH", /* 10 */
                "DDDDDDDDDD", /* 11 */
                "HHSSSHHHHH", /* 12 */
                "SSSSSHHHHH", /* 13 */
                "SSSSSHHHHH", /* 14 */
                "SSSSSHHHRR", /* 15 */
                "SSSSSHHRRR", /* 16 */
                "SSSSSSSSSr", /* 17 */
            )
        }

        //noinspection SpellCheckingInspection
        private val splitTableS by lazy {
            arrayOf(
                "YYYYYYNNNN", /* 2 */
                "YYYYYYNNNN", /* 3 */
                "NNNYYNNNNN", /* 4 */
                "NNNNNNNNNN", /* 5 */
                "YYYYYNNNNN", /* 6 */
                "YYYYYYNNNN", /* 7 */
                "YYYYYYYYYY", /* 8 */
                "YYYYYNYYNN", /* 9 */
                "NNNNNNNNNN", /* 10 */
            )
        }

        //noinspection SpellCheckingInspection
        private val softTableS by lazy {
            arrayOf(
                "HHHHHHHHHH", /* A */
                "HHHDDHHHHH", /* 2 */
                "HHHDDHHHHH", /* 3 */
                "HHDDDHHHHH", /* 4 */
                "HHDDDHHHHH", /* 5 */
                "HDDDDHHHHH", /* 6 */
                "SddddSSHHH", /* 7 */
                "SSSSSSSSSS", /* 8 */
                "SSSSSSSSSS"  /* 9 */
            )
        }

        //noinspection SpellCheckingInspection
        private val hardTableS by lazy {
            arrayOf(
                "HDDDDHHHHH", /* 9 */
                "DDDDDDDDHH", /* 10 */
                "DDDDDDDDDH", /* 11 */
                "HHSSSHHHHH", /* 12 */
                "SSSSSHHHHH", /* 13 */
                "SSSSSHHHHH", /* 14 */
                "SSSSSHHHRH", /* 15 */
                "SSSSSHHRRR", /* 16 */
                "SSSSSSSSSS", /* 17 */
            )
        }
    }
}
