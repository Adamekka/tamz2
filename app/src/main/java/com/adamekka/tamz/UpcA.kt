package com.adamekka.tamz

import java.io.Serializable

// Serializable lets rememberSaveable preserve the validated barcode when Android recreates the activity.
class UpcA private constructor(val digits: String) : Serializable {
    val modules: List<Boolean> = buildList(95) {
        fun append(pattern: Int, width: Int) {
            for (bit in width - 1 downTo 0) add((pattern shr bit) and 1 == 1)
        }

        append(0b101, 3)
        digits.take(6).forEach { append(L[it.digitToInt()], 7) }
        append(0b01010, 5)
        digits.drop(6).forEach { append(R[it.digitToInt()], 7) }
        append(0b101, 3)
    }

    companion object {
        private val L = listOf(
            0b0001101, 0b0011001, 0b0010011, 0b0111101, 0b0100011,
            0b0110001, 0b0101111, 0b0111011, 0b0110111, 0b0001011
        )
        // UPC-A encodes right-hand digits by inverting all seven bits of the left-hand pattern.
        private val R = L.map { it xor 0b1111111 }

        fun parse(value: String): UpcResult {
            if (value.length != 12 || value.any { it !in '0'..'9' }) {
                return UpcResult.Invalid("Zadejte přesně 12 číslic od 0 do 9.")
            }
            val sum = value.take(11).mapIndexed { index, digit ->
                digit.digitToInt() * if (index % 2 == 0) 3 else 1
            }.sum()
            val checkDigit = (10 - sum % 10) % 10
            if (value.last().digitToInt() != checkDigit) {
                return UpcResult.Invalid("Neplatná kontrolní číslice. Poslední číslice má být $checkDigit.")
            }
            return UpcResult.Valid(UpcA(value))
        }
    }
}

sealed interface UpcResult {
    data class Valid(val barcode: UpcA) : UpcResult
    data class Invalid(val message: String) : UpcResult
}
