package com.example.pos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Official Google 'G' Icon built directly in Compose using official brand vector paths and colors.
 * Brand colors:
 * - Red (#EA4335)
 * - Yellow (#FBBC05)
 * - Green (#34A853)
 * - Blue (#4285F4)
 */
@Composable
fun GoogleIcon(
    modifier: Modifier = Modifier
) {
    val iconModifier = Modifier
        .size(20.dp)
        .then(modifier)

    // Official Google Brand Colors
    val blueColor = Color(0xFF4285F4)
    val greenColor = Color(0xFF34A853)
    val yellowColor = Color(0xFFFBBC05)
    val redColor = Color(0xFFEA4335)

    // Vector Path Data (24x24 viewBox)
    val bluePathData = "M 23.49 12.27 c 0 -0.85 -0.07 -1.71 -0.22 -2.54 H 12 v 4.81 h 6.44 c -0.28 1.48 -1.13 2.74 -2.4 3.58 v 2.98 h 3.88 c 2.27 -2.09 3.57 -5.17 3.57 -8.83 z"
    val greenPathData = "M 12 24 c 3.24 0 5.95 -1.08 7.93 -2.91 l -3.88 -2.98 c -1.08 0.72 -2.45 1.16 -4.05 1.16 c -3.12 0 -5.77 -2.11 -6.72 -4.96 H 1.29 v 3.09 C 3.26 21.3 7.37 24 12 24 z"
    val yellowPathData = "M 5.28 14.31 c -0.25 -0.72 -0.38 -1.49 -0.38 -2.31 s 0.14 -1.59 0.38 -2.31 V 6.59 H 1.29 C 0.47 8.24 0 10.06 0 12 s 0.47 3.76 1.29 5.41 l 3.99 -3.10 z"
    val redPathData = "M 12 4.75 c 1.77 0 3.35 0.61 4.6 1.8 l 3.42 -3.42 C 17.95 1.19 15.24 0 12 0 C 7.37 0 3.26 2.7 1.29 6.59 l 3.99 3.09 c 0.95 -2.85 3.6 -4.93 6.72 -4.93 z"

    val bluePath = remember { PathParser().parsePathString(bluePathData).toPath() }
    val greenPath = remember { PathParser().parsePathString(greenPathData).toPath() }
    val yellowPath = remember { PathParser().parsePathString(yellowPathData).toPath() }
    val redPath = remember { PathParser().parsePathString(redPathData).toPath() }

    Canvas(modifier = iconModifier) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f

        scale(scaleX, scaleY, pivot = Offset.Zero) {
            drawPath(bluePath, blueColor)
            drawPath(greenPath, greenColor)
            drawPath(yellowPath, yellowColor)
            drawPath(redPath, redColor)
        }
    }
}
