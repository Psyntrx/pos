package com.example.pos.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.ui.theme.EmeraldBright
import com.example.pos.ui.theme.TextOnPrimary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// Local-only tokens (not in theme). Swap for theme colors if you have equivalents.
private val WelcomeBgTop = Color(0xFF020A05)
private val WelcomeBgBottom = Color(0xFF031109)
private val WaveLime = Color(0xFFB8F36B)

// Tiny floating sparkles (x, y normalized to the wave canvas)
private val Sparks = listOf(
    Offset(0.14f, 0.18f), Offset(0.28f, 0.08f), Offset(0.47f, 0.14f),
    Offset(0.63f, 0.05f), Offset(0.82f, 0.20f), Offset(0.91f, 0.42f),
    Offset(0.07f, 0.52f), Offset(0.36f, 0.30f), Offset(0.74f, 0.62f)
)

@Composable
fun SplashScreen(
    onGetStarted: () -> Unit
) {
    val fadeAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        fadeAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        )
    }

    // Slow, seamless loop that drives the waves (read inside draw only = no recomposition)
    val phase = rememberInfiniteTransition(label = "silk").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WelcomeBgTop, WelcomeBgBottom)))
            .drawBehind {
                // Full-screen haze so the bottom (behind the button) glows softly, no hard edge
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            EmeraldBright.copy(alpha = 0.22f),
                            EmeraldBright.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.78f),
                        radius = size.width * 1.1f
                    )
                )
            }
    ) {
        SilkWaves(
            phase = phase,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.50f)
                .align(BiasAlignment(0f, 0.22f))
                .alpha(fadeAlpha.value)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .alpha(fadeAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.19f))

            // Logo with a soft emerald halo behind it
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 72.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    EmeraldBright.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.width * 2.2f
                            ),
                            radius = size.width * 2.2f,
                            center = center
                        )
                    }
            ) {
                LeafLogo(modifier = Modifier.fillMaxSize())
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("Retail") }
                    withStyle(SpanStyle(color = EmeraldBright)) { append("Sync") }
                },
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Smarter Stock. Bigger Sales.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.weight(0.81f))

            Text(
                text = "Your shop. In your hands.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldBright,
                    contentColor = TextOnPrimary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LeafLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val upper = Path().apply {
            moveTo(w * 0.92f, 0f)
            cubicTo(w * 1.00f, h * 0.30f, w * 0.62f, h * 0.55f, w * 0.08f, h * 0.50f)
            cubicTo(w * 0.02f, h * 0.22f, w * 0.40f, h * 0.04f, w * 0.92f, 0f)
            close()
        }
        val lower = Path().apply {
            moveTo(w * 0.98f, h * 0.52f)
            cubicTo(w * 1.00f, h * 0.82f, w * 0.62f, h * 1.00f, w * 0.22f, h * 0.99f)
            cubicTo(w * 0.00f, h * 0.82f, w * 0.20f, h * 0.58f, w * 0.98f, h * 0.52f)
            close()
        }

        val brush = Brush.linearGradient(
            colors = listOf(lerp(WelcomeBgBottom, EmeraldBright, 0.7f), EmeraldBright),
            start = Offset(0f, h),
            end = Offset(w, 0f)
        )
        drawPath(upper, brush)
        drawPath(lower, brush)
    }
}

// ---------- Silk waves ----------

@Composable
private fun SilkWaves(phase: State<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawSilk(phase.value)
    }
}

private fun DrawScope.drawSilk(phase: Float) {
    val w = size.width
    val h = size.height
    val tau = (2 * PI).toFloat()
    val steps = 64
    val lines = 28

    // Spine of the ribbon and how wide the thread bundle is at each x
    fun spineY(u: Float) =
        0.50f + 0.20f * sin(tau * 0.8f * u + phase) +
                0.07f * sin(tau * 1.6f * u - 2f * phase + 1f)

    fun spread(u: Float) =
        0.10f + 0.20f * (0.5f + 0.5f * sin(tau * 0.8f * u + phase + 1.4f))

    // 1) Lime glow on the left slope (pulses gently with the loop)
    val pulse = 0.85f + 0.15f * sin(phase)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                WaveLime.copy(alpha = 0.55f * pulse),
                WaveLime.copy(alpha = 0.14f * pulse),
                Color.Transparent
            ),
            center = Offset(w * (0.36f + 0.06f * sin(phase)), h * 0.40f),
            radius = w * 0.50f
        )
    )

    // 2) Soft body under the spine for depth
    val body = Path().apply {
        for (s in 0..steps) {
            val u = s / steps.toFloat()
            val x = w * u
            val y = h * spineY(u)
            if (s == 0) moveTo(x, y) else lineTo(x, y)
        }
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        body,
        Brush.verticalGradient(
            listOf(EmeraldBright.copy(alpha = 0.10f), Color.Transparent)
        )
    )

    // 3) The silk threads
    for (i in 0 until lines) {
        val t = i / (lines - 1f)
        val path = Path()
        for (s in 0..steps) {
            val u = s / steps.toFloat()
            val x = w * u
            val y = h * (spineY(u) + spread(u) * (t - 0.5f))
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        val center = sin(PI.toFloat() * t)                       // 0 at the edges, 1 in the middle
        val limeMix = (1f - abs(t - 0.38f) * 3.2f).coerceIn(0f, 1f) * 0.85f
        val base = lerp(lerp(WelcomeBgBottom, EmeraldBright, 0.30f), EmeraldBright, center)
        val c = lerp(base, WaveLime, limeMix)
        val a = (0.25f + 0.65f * center).coerceIn(0f, 1f)

        val brush = Brush.horizontalGradient(
            listOf(
                c.copy(alpha = 0f),
                c.copy(alpha = a),
                lerp(c, WaveLime, 0.35f).copy(alpha = a),
                c.copy(alpha = a * 0.8f),
                c.copy(alpha = 0f)
            )
        )

        drawPath(path, brush, alpha = 0.18f, style = Stroke(width = 5.dp.toPx()))        // glow
        drawPath(path, brush, style = Stroke(width = (0.7f + 0.6f * center).dp.toPx()))  // crisp
    }

    // 4) Sparkles
    Sparks.forEachIndexed { idx, p ->
        val twinkle = 0.5f + 0.5f * sin(phase + idx * 1.7f)
        drawCircle(
            color = WaveLime.copy(alpha = 0.10f + 0.45f * twinkle),
            radius = (0.8f + 0.8f * twinkle).dp.toPx(),
            center = Offset(p.x * w, p.y * h)
        )
    }
}