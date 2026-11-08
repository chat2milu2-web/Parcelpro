package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Default colourful gradient colors for subtle animated border
val DefaultGradientBorderColors = listOf(
    Color(0xFF2563EB), // Royal Blue
    Color(0xFF06B6D4), // Cyan
    Color(0xFF8B5CF6), // Vivid Purple
    Color(0xFFF43F5E), // Coral Rose
    Color(0xFF10B981), // Emerald Green
    Color(0xFF2563EB)  // Loop
)

/**
 * A clean card component featuring a subtle, rotating animated gradient border.
 * The card interior preserves the standard theme surface color, ensuring the UI
 * remains clean, uncluttered, and readable in both Light and Dark modes while satisfying
 * the colourful border requirement.
 */
@Composable
fun AnimatedGradientBorderCard(
    modifier: Modifier = Modifier,
    borderWidth: Dp = 2.dp,
    outerCornerRadius: Dp = 16.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    gradientColors: List<Color> = DefaultGradientBorderColors,
    animationDurationMillis: Int = 6000,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "animated_gradient_border_transition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_angle"
    )

    val sweepGradient = Brush.sweepGradient(gradientColors)
    val outerShape = RoundedCornerShape(outerCornerRadius)
    val innerShape = RoundedCornerShape((outerCornerRadius - borderWidth).coerceAtLeast(0.dp))

    Box(
        modifier = modifier
            .clip(outerShape)
            .drawBehind {
                rotate(degrees = angle) {
                    drawCircle(
                        brush = sweepGradient,
                        radius = size.maxDimension
                    )
                }
            }
            .padding(borderWidth)
            .clip(innerShape)
            .background(containerColor)
    ) {
        content()
    }
}
