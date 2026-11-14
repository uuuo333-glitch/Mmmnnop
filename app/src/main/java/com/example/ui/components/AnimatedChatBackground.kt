package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatBackgroundStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedChatBackground(
    style: ChatBackgroundStyle,
    accentColor: Color,
    isLightTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chat_bg_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_phase"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_pulse"
    )

    val baseBg = if (isLightTheme) Color(0xFFEFEAE2) else MaterialTheme.colorScheme.background

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        val w = size.width
        val h = size.height
        when (style) {
            ChatBackgroundStyle.NEON_SPEED -> {
                // High-speed diagonal light streaks
                for (i in 0..8) {
                    val progress = (phase + i / 9f) % 1f
                    val y = h * progress
                    val startX = -w * 0.2f + (i % 3) * (w * 0.25f)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                accentColor.copy(alpha = 0.16f * pulse),
                                Color(0xFF1D9BF0).copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(startX, y),
                        end = Offset(startX + w * 0.85f, y - h * 0.18f),
                        strokeWidth = (2.5f + (i % 3)) * density
                    )
                }
            }
            ChatBackgroundStyle.EMERALD_AURORA -> {
                val path = Path()
                for (band in 0..2) {
                    path.reset()
                    val baseY = h * (0.25f + band * 0.25f)
                    path.moveTo(0f, baseY)
                    var x = 0f
                    while (x <= w) {
                        val rad = ((x / w) * 2f * PI + phase * 2f * PI + band).toFloat()
                        val y = baseY + sin(rad) * (42f * density)
                        path.lineTo(x, y)
                        x += 24f
                    }
                    drawPath(
                        path = path,
                        color = accentColor.copy(alpha = 0.10f + band * 0.04f),
                        style = Stroke(width = 18f * density)
                    )
                }
            }
            ChatBackgroundStyle.CARBON_STARS -> {
                for (i in 0..28) {
                    val fx = ((i * 73) % 100) / 100f
                    val fy = (((i * 41) % 100) / 100f + phase) % 1f
                    val alpha = (0.12f + 0.22f * absSin(phase * PI.toFloat() * 2f + i))
                    drawCircle(
                        color = accentColor.copy(alpha = alpha),
                        radius = (1.5f + (i % 3)) * density,
                        center = Offset(w * fx, h * fy)
                    )
                }
            }
            ChatBackgroundStyle.TURBO_PULSE -> {
                val cx = w * 0.5f
                val cy = h * 0.45f
                for (ring in 1..5) {
                    val r = ((phase + ring * 0.2f) % 1f) * (w.coerceAtLeast(h) * 0.65f)
                    val fade = 1f - (r / (w.coerceAtLeast(h) * 0.65f))
                    drawCircle(
                        color = accentColor.copy(alpha = fade * 0.18f),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.5f * density)
                    )
                }
            }
            ChatBackgroundStyle.WHATSAPP_CLASSIC -> {
                val step = 64f * density
                val shift = phase * step
                var y = -step + shift
                while (y < h + step) {
                    var x = 0f
                    while (x < w + step) {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.07f),
                            radius = 6f * density,
                            center = Offset(x, y),
                            style = Stroke(width = 1.5f * density)
                        )
                        x += step
                    }
                    y += step
                }
            }
        }
    }
}

private fun absSin(angle: Float): Float {
    val s = sin(angle)
    return if (s < 0f) -s else s
}

/**
 * Animated motion showcase clip embedded inside chat messages (copy/download protected).
 */
@Composable
fun AnimatedStoreVideoReelCard(
    title: String,
    accentColor: Color,
    copyBlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "reel_anim")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reel_sweep"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF07131A),
                        accentColor.copy(alpha = 0.35f),
                        Color(0xFF0A2233)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Animated racing telemetry waves inside the video preview
            for (k in 0..4) {
                val offset = (sweep + k * 0.2f) % 1f
                val x = w * offset
                drawLine(
                    color = accentColor.copy(alpha = 0.45f),
                    start = Offset(x, h * 0.85f),
                    end = Offset(x + 26f, h * 0.25f),
                    strokeWidth = 3f
                )
            }
            val cx = w * 0.5f + cos(sweep * 2f * PI).toFloat() * 18f
            val cy = h * 0.5f + sin(sweep * 2f * PI).toFloat() * 6f
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = 38f,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = "مقطع فيديو متحرك",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "مقطع تفاعلي متحرك",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                if (copyBlocked) {
                    Surface(
                        color = Color(0xFFD97706).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "ممنوع النسخ",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "محمي من النسخ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}
