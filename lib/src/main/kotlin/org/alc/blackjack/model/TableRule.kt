package org.alc.blackjack.model


data class TableRule(
    val dealerHitsOnSoft17: Boolean = true,
    val allowSurrender: Boolean = false,
    val allowFreeSplit: Boolean = false,
    val allowFreeDouble: Boolean = false,
    val allowDoubleAnytime: Boolean = false,
    val blackjackPayFactor: Double = 1.5,
    val maxSplit: Int = 4,
    val allowDoubleRescue: Boolean = false,
    val allowMultipleSplitAces: Boolean = false,
    val allowHitSplitAces: Boolean = false,
    val pushOn22: Boolean = false,
    val allowDoubleForLess: Boolean = false,
    val insureForLess: Boolean = false,
    val alwaysPay21: Boolean = false
) {
    companion object {
        val DEFAULT = TableRule()

        val SPANISH21 = TableRule(
            allowSurrender = true,
            allowDoubleAnytime = true,
            allowDoubleRescue = true,
            allowMultipleSplitAces = true,
            allowHitSplitAces = true,
            allowDoubleForLess = true,
            insureForLess = true,
            alwaysPay21 = true
        )

        val FREE_BET = TableRule(
            allowFreeSplit = true,
            allowFreeDouble = true,
            pushOn22 = true
        )
    }
}


