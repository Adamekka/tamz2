package com.adamekka.tamz

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.graphics.createBitmap
import java.io.IOException
import kotlin.math.roundToInt

@Composable
fun BarcodeView(barcode: UpcA) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(113f / 72f)
            .semantics { contentDescription = "Čárový kód UPC-A ${barcode.digits}" }
    ) {
        drawIntoCanvas {
            drawBarcode(it.nativeCanvas, barcode, size.width.roundToInt(), size.height.roundToInt())
        }
    }
}

// Both the custom Composable and the exported PNG use this drawing code.
internal fun drawBarcode(canvas: Canvas, barcode: UpcA, width: Int, height: Int) {
    assert(width >= 113 && height > 0) { "Barcode canvas must fit all 113 modules including quiet zones" }
    // Whole-pixel modules keep bar edges sharp; the extra nine modules on each side must stay white.
    val unit = (width / 113).toFloat()
    val start = (width - 95 * unit) / 2f
    val paint = Paint().apply { color = Color.WHITE }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.color = Color.BLACK
    barcode.modules.forEachIndexed { index, black ->
        if (black) {
            val guard = index < 3 || index in 45..49 || index >= 92
            canvas.drawRect(
                start + index * unit,
                height * 0.05f,
                start + (index + 1) * unit,
                height * if (guard) 0.84f else 0.78f,
                paint
            )
        }
    }
    paint.isAntiAlias = true
    paint.textSize = 5f * unit
    paint.textAlign = Paint.Align.CENTER
    barcode.digits.forEachIndexed { index, digit ->
        val position = if (index < 6) 3 + index * 7 else 50 + (index - 6) * 7
        canvas.drawText(digit.toString(), start + (position + 3.5f) * unit, height * 0.95f, paint)
    }
}

fun saveBarcode(context: Context, barcode: UpcA): Result<Uri> {
    val resolver = context.contentResolver
    var pending: Uri? = null
    return try {
        val bitmap = createBitmap(678, 432)
        drawBarcode(Canvas(bitmap), barcode, bitmap.width, bitmap.height)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "UPC-A-${barcode.digits}-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/UPC-A")
            // Keep incomplete files out of the gallery until the PNG is fully written.
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("MediaStore did not create an image")
        pending = uri
        val output = resolver.openOutputStream(uri) ?: throw IOException("MediaStore did not open the image")
        output.use {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                throw IOException("PNG encoding failed")
            }
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        if (resolver.update(uri, values, null, null) != 1) {
            throw IOException("MediaStore did not publish the image")
        }
        Result.success(uri)
    } catch (error: Exception) {
        if (pending != null) {
            try {
                resolver.delete(pending, null, null)
            } catch (cleanup: Exception) {
                error.addSuppressed(cleanup)
            }
        }
        Result.failure(error)
    }
}
