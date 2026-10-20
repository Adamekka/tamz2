package com.adamekka.tamz

import android.graphics.BitmapFactory
import android.graphics.Color
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.oned.UPCAReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BarcodeAppTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun validatesInputAndPreservesGeneratedCodeAcrossRecreation() {
        compose.onNodeWithText("Uložit do galerie").assertDoesNotExist()
        compose.onNodeWithText("Vykreslit").performClick()
        compose.onNodeWithText("Zadejte přesně 12 číslic od 0 do 9.").assertIsDisplayed()

        compose.onNodeWithText("12místný kód UPC-A").performTextInput("036000291452")
        compose.onNodeWithText("Vykreslit").performClick()
        compose.onNodeWithContentDescription("Čárový kód UPC-A 036000291452").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithContentDescription("Čárový kód UPC-A 036000291452").assertIsDisplayed()

        compose.onNodeWithText("12místný kód UPC-A").performTextReplacement("036000291453")
        compose.onNodeWithContentDescription("Čárový kód UPC-A 036000291452").assertDoesNotExist()
        compose.onNodeWithText("Vykreslit").performClick()
        compose.onNodeWithText("Neplatná kontrolní číslice. Poslední číslice má být 2.").assertIsDisplayed()
        compose.onNodeWithText("Uložit do galerie").assertDoesNotExist()
    }

    @Test
    fun scannerIsAvailableFromBottomTab() {
        compose.onNodeWithText("Načíst").performClick()
        compose.onNodeWithText("Načíst UPC-A fotoaparátem").assertIsDisplayed()
        compose.onNodeWithText("Vytvořit").performClick()
        compose.onNodeWithText("12místný kód UPC-A").assertIsDisplayed()
    }

    @Test
    fun savesCompletePngToGalleryWithWhiteQuietZones() {
        val context = compose.activity.applicationContext
        val barcode = (UpcA.parse("036000291452") as UpcResult.Valid).barcode
        val uri = saveBarcode(context, barcode).getOrThrow()
        try {
            val resolver = context.contentResolver
            resolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.IS_PENDING, MediaStore.Images.Media.MIME_TYPE),
                null, null, null
            )!!.use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
                assertEquals("image/png", cursor.getString(1))
            }
            val bitmap = resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
            assertNotNull(bitmap)
            assertEquals(678, bitmap.width)
            assertEquals(432, bitmap.height)
            for (x in 0 until 54) {
                assertEquals(Color.WHITE, bitmap.getPixel(x, bitmap.height / 2))
                assertEquals(Color.WHITE, bitmap.getPixel(bitmap.width - 1 - x, bitmap.height / 2))
            }
            assertEquals(Color.BLACK, bitmap.getPixel(54, bitmap.height / 2))
            assertEquals(Color.BLACK, bitmap.getPixel(623, bitmap.height / 2))
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val decoded = UPCAReader().decode(
                BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels)))
            )
            assertEquals(barcode.digits, decoded.text)
        } finally {
            context.contentResolver.delete(uri, null, null)
        }
    }
}
