package com.adamekka.tamz

import org.junit.Assert.assertEquals
import org.junit.Test

class SavingsTest {
    @Test
    fun monthlyDepositsAreAddedAfterInterest() {
        val result = calculateSavings(SavingsInput(5_000.0, 3_000.0, 4.0, 20, 0.0, 0.0))

        assertEquals(1_111_436.79, result.total, 0.01)
        assertEquals(725_000.0, result.contributions, 0.0)
        assertEquals(386_436.79, result.interest, 0.01)
    }

    @Test
    fun noInterestStillIncludesEveryMonthlyDeposit() {
        val result = calculateSavings(SavingsInput(5_000.0, 100.0, 0.0, 2, 0.0, 0.0))

        assertEquals(7_400.0, result.total, 0.0)
        assertEquals(7_400.0, result.contributions, 0.0)
        assertEquals(0.0, result.interest, 0.0)
    }

    @Test
    fun taxReducesMonthlyInterestAndInflationOnlyDiscountsFinalValue() {
        val result = calculateSavings(SavingsInput(1_000.0, 0.0, 12.0, 1, 50.0, 10.0))

        assertEquals(1_061.6778118645, result.total, 0.000001)
        assertEquals(61.6778118645, result.interest, 0.000001)
        assertEquals(965.1616471495, result.inflationAdjusted, 0.000001)
        assertEquals(1_000.0, result.contributions, 0.0)
    }

    @Test
    fun fullTaxLeavesOnlyContributions() {
        val result = calculateSavings(SavingsInput(1_000.0, 100.0, 30.0, 1, 100.0, 0.0))

        assertEquals(2_200.0, result.total, 0.0)
        assertEquals(0.0, result.interest, 0.0)
    }

    @Test
    fun zeroYearsKeepsOnlyInitialDeposit() {
        val result = calculateSavings(SavingsInput(5_000.0, 3_000.0, 4.0, 0, 15.0, 2.0))

        assertEquals(5_000.0, result.total, 0.0)
        assertEquals(5_000.0, result.contributions, 0.0)
        assertEquals(0.0, result.interest, 0.0)
        assertEquals(5_000.0, result.inflationAdjusted, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidStoredTaxIsRejectedDuringConstruction() {
        SavingsInput(5_000.0, 0.0, 5.0, 5, 101.0, 0.0)
    }
}
