package com.adamekka.tamz

import kotlin.math.pow

data class SavingsInput(
    val initialDeposit: Double,
    val monthlyDeposit: Double,
    val annualInterest: Double,
    val years: Int,
    val taxRate: Double,
    val annualInflation: Double
) {
    init {
        // Saved input is checked here as well, so invalid stored values cannot reach the calculation.
        require(initialDeposit.isFinite() && initialDeposit in 0.0..1_000_000.0)
        require(monthlyDeposit.isFinite() && monthlyDeposit in 0.0..100_000.0)
        require(annualInterest.isFinite() && annualInterest in 0.0..30.0)
        require(years in 0..50)
        require(taxRate.isFinite() && taxRate in 0.0..100.0)
        require(annualInflation.isFinite() && annualInflation in 0.0..20.0)
    }
}

data class SavingsResult(
    val total: Double,
    val contributions: Double,
    val interest: Double,
    val inflationAdjusted: Double
)

fun calculateSavings(input: SavingsInput): SavingsResult {
    val months = input.years * 12
    val monthlyRate = input.annualInterest / 100.0 / 12.0 * (1.0 - input.taxRate / 100.0)
    var total = input.initialDeposit
    repeat(months) {
        // End-of-month deposits earn interest starting in the following month.
        total = total * (1.0 + monthlyRate) + input.monthlyDeposit
    }
    val contributions = input.initialDeposit + input.monthlyDeposit * months
    return SavingsResult(
        total = total,
        contributions = contributions,
        interest = total - contributions,
        inflationAdjusted = total / (1.0 + input.annualInflation / 100.0).pow(input.years)
    )
}
