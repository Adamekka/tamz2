package com.adamekka.tamz

import android.annotation.SuppressLint
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class SavedCalculation(val savedAt: Long, val input: SavingsInput) {
    init {
        require(savedAt > 0)
    }
}

fun readSavingsHistory(preferences: SharedPreferences): Result<List<SavedCalculation>> =
    try {
        val stored = preferences.getString("history", null)
        if (stored == null) {
            // No saved value means the user has not saved a calculation yet.
            Result.success(emptyList())
        } else {
            val array = JSONArray(stored)
            Result.success(
                List(array.length()) { index ->
                    val entry = array.getJSONObject(index)
                    SavedCalculation(
                        savedAt = entry.getLong("savedAt"),
                        input = SavingsInput(
                            initialDeposit = entry.getDouble("initialDeposit"),
                            monthlyDeposit = entry.getDouble("monthlyDeposit"),
                            annualInterest = entry.getDouble("annualInterest"),
                            years = entry.getInt("years"),
                            taxRate = entry.getDouble("taxRate"),
                            annualInflation = entry.getDouble("annualInflation")
                        )
                    )
                }
            )
        }
    } catch (error: Exception) {
        Result.failure(error)
    }

// The KTX edit helper returns Unit, so it cannot report a failed synchronous disk write.
@SuppressLint("UseKtx")
fun writeSavingsHistory(
    preferences: SharedPreferences,
    entries: List<SavedCalculation>
): Result<Unit> =
    try {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("savedAt", entry.savedAt)
                    .put("initialDeposit", entry.input.initialDeposit)
                    .put("monthlyDeposit", entry.input.monthlyDeposit)
                    .put("annualInterest", entry.input.annualInterest)
                    .put("years", entry.input.years)
                    .put("taxRate", entry.input.taxRate)
                    .put("annualInflation", entry.input.annualInflation)
            )
        }
        // commit confirms the disk write before the UI reports that the calculation was saved.
        if (preferences.edit().putString("history", array.toString()).commit()) {
            Result.success(Unit)
        } else {
            Result.failure(IOException("History preferences could not be written to disk"))
        }
    } catch (error: Exception) {
        Result.failure(error)
    }
