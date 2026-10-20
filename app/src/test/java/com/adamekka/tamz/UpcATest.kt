package com.adamekka.tamz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpcATest {
    @Test
    fun acceptsValidCodesAndPreservesLeadingZeros() {
        listOf("036000291452", "012345678905", "123456789012", "000000000000", "999999999993")
            .forEach { digits ->
                assertEquals(digits, (UpcA.parse(digits) as UpcResult.Valid).barcode.digits)
            }
    }

    @Test
    fun rejectsInvalidLengthAndNonAsciiDigits() {
        listOf("", "12345678901", "1234567890123", "12345678901a", " 036000291452", "０36000291452")
            .forEach { assertTrue(it, UpcA.parse(it) is UpcResult.Invalid) }
    }

    @Test
    fun rejectsASingleDigitChangeAtEveryPosition() {
        val digits = "036000291452"
        digits.indices.forEach { index ->
            val changed = digits.toCharArray()
            changed[index] = '0' + (changed[index].digitToInt() + 1) % 10
            assertTrue("Changed digit $index", UpcA.parse(String(changed)) is UpcResult.Invalid)
        }
        assertEquals(
            "Neplatná kontrolní číslice. Poslední číslice má být 2.",
            (UpcA.parse("036000291453") as UpcResult.Invalid).message
        )
    }

    @Test
    fun encodesAllDigitPatternsWithStartMiddleAndEndGuards() {
        val examples = mapOf(
            "012345678905" to listOf(
                "101", "0001101", "0011001", "0010011", "0111101", "0100011", "0110001",
                "01010", "1010000", "1000100", "1001000", "1110100", "1110010", "1001110", "101"
            ),
            "987654321098" to listOf(
                "101", "0001011", "0110111", "0111011", "0101111", "0110001", "0100011",
                "01010", "1000010", "1101100", "1100110", "1110010", "1110100", "1001000", "101"
            )
        )
        examples.forEach { (digits, expected) ->
            val barcode = (UpcA.parse(digits) as UpcResult.Valid).barcode
            assertEquals(95, barcode.modules.size)
            assertEquals(expected.joinToString(""), barcode.modules.joinToString("") { if (it) "1" else "0" })
        }
    }
}
