package com.adamekka.tamz

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate

@Composable
fun Tile(tile: Char, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawTile(tile, size.width) }
}

@Composable
fun FittedBoard(rows: List<String>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val edge = minOf(maxWidth / rows.first().length, maxHeight / rows.size)
        Board(rows, Modifier.size(edge * rows.first().length, edge * rows.size))
    }
}

@Composable
fun Board(rows: List<String>, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(rows.first().length.toFloat() / rows.size)) {
        val edge = size.width / rows.first().length
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, tile ->
                translate(x * edge, y * edge) { drawTile(tile, edge) }
            }
        }
    }
}

private fun DrawScope.drawTile(tile: Char, edge: Float) {
    if (tile == ' ') return
    val inset = edge * 0.035f
    val tileSize = Size(edge - inset * 2, edge - inset * 2)
    drawRect(Color(0xFFD8E9F8), size = Size(edge, edge))
    drawRect(Color(0xFFB5C9DB), style = Stroke(edge * 0.018f), size = Size(edge, edge))
    if (tile == '#') {
        drawRoundRect(Color(0xFF65717E), Offset(inset, inset), tileSize, CornerRadius(edge * 0.07f))
        drawLine(Color(0xFF9DA8B2), Offset(edge * 0.12f, edge * 0.15f), Offset(edge * 0.88f, edge * 0.15f), edge * 0.04f)
        return
    }
    if (tile in ".+*") {
        drawCircle(Color(0xFF865A00), edge * 0.25f, Offset(edge / 2, edge / 2), style = Stroke(edge * 0.065f))
        drawCircle(Color(0xFF865A00), edge * 0.07f, Offset(edge / 2, edge / 2))
    }
    if (tile in "$*") {
        val color = if (tile == '*') Color(0xFF387D39) else Color(0xFFA45C19)
        drawRoundRect(color, Offset(edge * 0.12f, edge * 0.12f), Size(edge * 0.76f, edge * 0.76f), CornerRadius(edge * 0.04f))
        drawRect(Color(0xFFFFD291), Offset(edge * 0.2f, edge * 0.2f), Size(edge * 0.6f, edge * 0.6f), style = Stroke(edge * 0.05f))
        drawLine(Color(0xFFFFD291), Offset(edge * 0.23f, edge * 0.23f), Offset(edge * 0.77f, edge * 0.77f), edge * 0.065f)
        drawLine(Color(0xFFFFD291), Offset(edge * 0.77f, edge * 0.23f), Offset(edge * 0.23f, edge * 0.77f), edge * 0.065f)
        // The center mark distinguishes a filled goal without relying on green alone.
        if (tile == '*') drawCircle(Color.White, edge * 0.09f, Offset(edge / 2, edge / 2))
    }
    if (tile in "@+") {
        drawCircle(Color(0xFF174C80), edge * 0.34f, Offset(edge / 2, edge / 2))
        drawCircle(Color.White, edge * 0.065f, Offset(edge * 0.39f, edge * 0.44f))
        drawCircle(Color.White, edge * 0.065f, Offset(edge * 0.61f, edge * 0.44f))
        drawLine(Color.White, Offset(edge * 0.38f, edge * 0.64f), Offset(edge * 0.62f, edge * 0.64f), edge * 0.045f)
    }
}
