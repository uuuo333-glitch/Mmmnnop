package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DisappearingMode
import com.example.data.model.SoundEffectType
import com.example.data.model.StoreConfig
import com.example.ui.theme.VerifiedBlue

@Composable
fun VerifiedStoreHeader(
    storeConfig: StoreConfig,
    bellPulseCount: Int,
    onBellClick: () -> Unit,
    onStoreHeaderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bellScale = remember { Animatable(1f) }
    LaunchedEffect(bellPulseCount) {
        if (bellPulseCount > 0) {
            bellScale.snapTo(1.28f)
            bellScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val activeSound = SoundEffectType.fromId(storeConfig.activeAlertSound)
    val disappearingMode = DisappearingMode.fromId(storeConfig.disappearingMode)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            // Top WhatsApp-style Store Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Store Avatar with verified indicator
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onStoreHeaderClick)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_store_banner_1794675291139),
                        contentDescription = "صورة المتجر",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    if (storeConfig.isVerified) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "شارة التوثيق الزرقاء",
                            tint = VerifiedBlue,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .padding(1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Store Name + Blue Badge + "المتجر" Subtitle directly underneath
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onStoreHeaderClick)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = storeConfig.storeName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (storeConfig.isVerified) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "موثق",
                                tint = VerifiedBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Blue badge pill + "المتجر" + Disappearing & Privacy indicators
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(
                            color = VerifiedBlue.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Storefront,
                                    contentDescription = null,
                                    tint = VerifiedBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = storeConfig.badgeLabel.ifBlank { "المتجر" },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = VerifiedBlue
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = "مؤقت اختفاء الرسائل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = disappearingMode.arabicName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (storeConfig.blockScreenshots) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = "حجب تصوير الشاشة مفعل",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Customer Alert Bell Button (blasts Truck Horn / chosen sound!)
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .scale(bellScale.value)
                        .testTag("bell_alert_button")
                ) {
                    IconButton(
                        onClick = onBellClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "جرس التنبيه: ${activeSound.arabicName}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Animated Top Marquee Ticker Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                VerifiedBlue.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            )
                        )
                    )
                    .padding(vertical = 6.dp, horizontal = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = storeConfig.tickerText.ifBlank {
                            "أهلاً بكم في ${storeConfig.storeName} • ردود فورية بالذكاء الاصطناعي • تتبع الشحن مباشر"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(iterations = Int.MAX_VALUE)
                    )
                }
            }
        }
    }
}
