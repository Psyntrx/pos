package com.example.pos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pos.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Palette constants derived from the app's existing emerald palette.
// These produce the dark-to-emerald gradient range used in the abstract shapes.
// ─────────────────────────────────────────────────────────────────────────────

private val DeepForest = Color(0xFF061A10)
private val DarkEmerald = Color(0xFF0A2E1A)
private val MidEmerald = Color(0xFF105E32)
private val BrightEmerald = Color(0xFF14A05A)
private val GlowEmerald = Color(0xFF1FD87A)
private val PaleEmerald = Color(0xFF82E8B2)
private val SphereHighlight = Color(0xFFA6F0CD)

// Light mode palette variants
private val LightDeepGreen = Color(0xFF0F4D2A)
private val LightMidGreen = Color(0xFF1A7D44)
private val LightBrightGreen = Color(0xFF22A95C)
private val LightSphereHighlight = Color(0xFF6FD99A)

/**
 * Full-screen abstract decorative background with large overlapping organic blob shapes
 * and spheres, inspired by the reference image's welcome panel.
 *
 * Used on the Welcome / Splash screen where the background fills the entire viewport.
 */
@Composable
fun AbstractBlobBackground(
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == BgDark ||
            MaterialTheme.colorScheme.background == Color(0xFF090F0D)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // ── Base gradient ────────────────────────────────────────────────
        drawRect(
            brush = if (isDark) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF040D08),
                        DeepForest,
                        DarkEmerald
                    ),
                    startY = 0f,
                    endY = h * 0.65f
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B3D22),
                        LightDeepGreen,
                        LightMidGreen
                    ),
                    startY = 0f,
                    endY = h * 0.65f
                )
            }
        )

        // ── Large blob 1 (top-left, biggest) ─────────────────────────────
        val blob1 = Path().apply {
            moveTo(-w * 0.15f, -h * 0.05f)
            cubicTo(
                w * 0.20f, -h * 0.10f,
                w * 0.55f, h * 0.15f,
                w * 0.35f, h * 0.45f
            )
            cubicTo(
                w * 0.18f, h * 0.65f,
                -w * 0.10f, h * 0.55f,
                -w * 0.20f, h * 0.30f
            )
            close()
        }
        drawPath(
            path = blob1,
            brush = Brush.radialGradient(
                colors = if (isDark) {
                    listOf(MidEmerald.copy(alpha = 0.7f), DarkEmerald.copy(alpha = 0.3f))
                } else {
                    listOf(LightMidGreen.copy(alpha = 0.7f), LightDeepGreen.copy(alpha = 0.3f))
                },
                center = Offset(w * 0.15f, h * 0.20f),
                radius = w * 0.55f
            )
        )

        // ── Large blob 2 (top-right, overlapping) ────────────────────────
        val blob2 = Path().apply {
            moveTo(w * 0.55f, -h * 0.12f)
            cubicTo(
                w * 1.10f, h * 0.05f,
                w * 1.05f, h * 0.45f,
                w * 0.60f, h * 0.50f
            )
            cubicTo(
                w * 0.30f, h * 0.52f,
                w * 0.25f, h * 0.15f,
                w * 0.55f, -h * 0.12f
            )
            close()
        }
        drawPath(
            path = blob2,
            brush = Brush.radialGradient(
                colors = if (isDark) {
                    listOf(MidEmerald.copy(alpha = 0.55f), DeepForest.copy(alpha = 0.15f))
                } else {
                    listOf(LightBrightGreen.copy(alpha = 0.5f), LightDeepGreen.copy(alpha = 0.15f))
                },
                center = Offset(w * 0.75f, h * 0.22f),
                radius = w * 0.50f
            )
        )

        // ── Mid blob 3 (center-left flowing down) ────────────────────────
        val blob3 = Path().apply {
            moveTo(-w * 0.05f, h * 0.30f)
            cubicTo(
                w * 0.25f, h * 0.25f,
                w * 0.50f, h * 0.50f,
                w * 0.30f, h * 0.70f
            )
            cubicTo(
                w * 0.10f, h * 0.85f,
                -w * 0.10f, h * 0.70f,
                -w * 0.05f, h * 0.30f
            )
            close()
        }
        drawPath(
            path = blob3,
            brush = Brush.radialGradient(
                colors = if (isDark) {
                    listOf(BrightEmerald.copy(alpha = 0.30f), DarkEmerald.copy(alpha = 0.05f))
                } else {
                    listOf(LightBrightGreen.copy(alpha = 0.35f), LightDeepGreen.copy(alpha = 0.05f))
                },
                center = Offset(w * 0.15f, h * 0.50f),
                radius = w * 0.45f
            )
        )

        // ── Sphere 1 (small, top-right area) ─────────────────────────────
        val sphereCenter1 = Offset(w * 0.82f, h * 0.12f)
        val sphereRadius1 = w * 0.12f
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (isDark) {
                    listOf(
                        SphereHighlight.copy(alpha = 0.30f),
                        BrightEmerald.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        LightSphereHighlight.copy(alpha = 0.40f),
                        LightBrightGreen.copy(alpha = 0.20f),
                        Color.Transparent
                    )
                },
                center = Offset(sphereCenter1.x - sphereRadius1 * 0.3f, sphereCenter1.y - sphereRadius1 * 0.3f),
                radius = sphereRadius1
            ),
            center = sphereCenter1,
            radius = sphereRadius1
        )

        // ── Sphere 2 (medium, center-left) ───────────────────────────────
        val sphereCenter2 = Offset(w * 0.12f, h * 0.38f)
        val sphereRadius2 = w * 0.18f
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (isDark) {
                    listOf(
                        PaleEmerald.copy(alpha = 0.18f),
                        GlowEmerald.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        LightSphereHighlight.copy(alpha = 0.30f),
                        LightBrightGreen.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                },
                center = Offset(sphereCenter2.x - sphereRadius2 * 0.25f, sphereCenter2.y - sphereRadius2 * 0.25f),
                radius = sphereRadius2
            ),
            center = sphereCenter2,
            radius = sphereRadius2
        )

        // ── Sphere 3 (small, bottom-right) ───────────────────────────────
        val sphereCenter3 = Offset(w * 0.72f, h * 0.48f)
        val sphereRadius3 = w * 0.08f
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (isDark) {
                    listOf(
                        GlowEmerald.copy(alpha = 0.35f),
                        BrightEmerald.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        LightSphereHighlight.copy(alpha = 0.45f),
                        LightBrightGreen.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                },
                center = Offset(sphereCenter3.x - sphereRadius3 * 0.3f, sphereCenter3.y - sphereRadius3 * 0.3f),
                radius = sphereRadius3
            ),
            center = sphereCenter3,
            radius = sphereRadius3
        )

        // ── Bottom gradient fade for text readability ────────────────────
        drawRect(
            brush = Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(
                        Color.Transparent,
                        Color(0xFF040D08).copy(alpha = 0.7f),
                        Color(0xFF040D08)
                    )
                } else {
                    listOf(
                        Color.Transparent,
                        Color(0xFF0B3D22).copy(alpha = 0.7f),
                        Color(0xFF0B3D22)
                    )
                },
                startY = h * 0.50f,
                endY = h
            )
        )
    }
}

/**
 * Shorter abstract decorative header for form screens (Sign In, Sign Up, Forgot Password).
 * Occupies approximately the top 30-35% of the screen with the same organic blob aesthetic.
 *
 * Inspired by the reference image's panels 2 and 3 — decorative header with form below.
 */
@Composable
fun AbstractBlobHeader(
    modifier: Modifier = Modifier,
    headerHeight: Dp = 260.dp
) {
    val isDark = MaterialTheme.colorScheme.background == BgDark ||
            MaterialTheme.colorScheme.background == Color(0xFF090F0D)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .clip(
                RoundedCornerShape(
                    bottomStart = 32.dp,
                    bottomEnd = 32.dp
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // ── Base gradient ────────────────────────────────────────────
            drawRect(
                brush = if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF040D08),
                            DeepForest,
                            DarkEmerald.copy(alpha = 0.95f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B3D22),
                            LightDeepGreen,
                            LightMidGreen.copy(alpha = 0.95f)
                        )
                    )
                }
            )

            // ── Blob 1 (top-left) ────────────────────────────────────────
            val blob1 = Path().apply {
                moveTo(-w * 0.15f, -h * 0.10f)
                cubicTo(
                    w * 0.30f, -h * 0.20f,
                    w * 0.55f, h * 0.30f,
                    w * 0.25f, h * 0.70f
                )
                cubicTo(
                    w * 0.05f, h * 0.95f,
                    -w * 0.15f, h * 0.70f,
                    -w * 0.15f, h * 0.30f
                )
                close()
            }
            drawPath(
                path = blob1,
                brush = Brush.radialGradient(
                    colors = if (isDark) {
                        listOf(MidEmerald.copy(alpha = 0.6f), DarkEmerald.copy(alpha = 0.15f))
                    } else {
                        listOf(LightMidGreen.copy(alpha = 0.6f), LightDeepGreen.copy(alpha = 0.15f))
                    },
                    center = Offset(w * 0.15f, h * 0.35f),
                    radius = w * 0.50f
                )
            )

            // ── Blob 2 (top-right) ───────────────────────────────────────
            val blob2 = Path().apply {
                moveTo(w * 0.50f, -h * 0.15f)
                cubicTo(
                    w * 1.10f, h * 0.05f,
                    w * 1.00f, h * 0.65f,
                    w * 0.55f, h * 0.80f
                )
                cubicTo(
                    w * 0.30f, h * 0.90f,
                    w * 0.20f, h * 0.30f,
                    w * 0.50f, -h * 0.15f
                )
                close()
            }
            drawPath(
                path = blob2,
                brush = Brush.radialGradient(
                    colors = if (isDark) {
                        listOf(MidEmerald.copy(alpha = 0.45f), DeepForest.copy(alpha = 0.10f))
                    } else {
                        listOf(LightBrightGreen.copy(alpha = 0.4f), LightDeepGreen.copy(alpha = 0.10f))
                    },
                    center = Offset(w * 0.70f, h * 0.30f),
                    radius = w * 0.45f
                )
            )

            // ── Sphere (small highlight) ─────────────────────────────────
            val sCenter = Offset(w * 0.78f, h * 0.25f)
            val sRadius = w * 0.10f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isDark) {
                        listOf(
                            SphereHighlight.copy(alpha = 0.28f),
                            BrightEmerald.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    } else {
                        listOf(
                            LightSphereHighlight.copy(alpha = 0.40f),
                            LightBrightGreen.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    },
                    center = Offset(sCenter.x - sRadius * 0.3f, sCenter.y - sRadius * 0.3f),
                    radius = sRadius
                ),
                center = sCenter,
                radius = sRadius
            )

            // ── Second sphere (left side) ────────────────────────────────
            val sCenter2 = Offset(w * 0.18f, h * 0.65f)
            val sRadius2 = w * 0.07f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isDark) {
                        listOf(
                            GlowEmerald.copy(alpha = 0.25f),
                            BrightEmerald.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    } else {
                        listOf(
                            LightSphereHighlight.copy(alpha = 0.35f),
                            LightBrightGreen.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    },
                    center = Offset(sCenter2.x - sRadius2 * 0.3f, sCenter2.y - sRadius2 * 0.3f),
                    radius = sRadius2
                ),
                center = sCenter2,
                radius = sRadius2
            )
        }
    }
}
