package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatBackgroundStyle
import com.example.data.model.ChatMessage
import com.example.data.model.ShipmentOrder
import com.example.data.model.SoundEffectType
import com.example.data.model.StoreConfig
import com.example.data.model.StoreProduct
import com.example.ui.UiState
import com.example.ui.components.AnimatedChatBackground
import com.example.ui.components.AnimatedStoreVideoReelCard
import com.example.ui.components.InlineChatProductCard
import com.example.ui.components.InlineShipmentTrackingCard
import com.example.ui.components.VerifiedStoreHeader
import com.example.ui.theme.VerifiedBlue

@Composable
fun ChatScreen(
    storeConfig: StoreConfig,
    messages: List<ChatMessage>,
    products: List<StoreProduct>,
    shipments: List<ShipmentOrder>,
    aiReplyState: UiState<Unit>,
    bellPulseCount: Int,
    onBellClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onOrderProduct: (StoreProduct) -> Unit,
    onPlaySound: (SoundEffectType) -> Unit,
    onSendSoundToChat: (SoundEffectType) -> Unit,
    onSendShipmentUpdate: (ShipmentOrder) -> Unit,
    onOpenOwnerStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showSoundDrawer by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val bgStyle = ChatBackgroundStyle.fromId(storeConfig.chatBackgroundId)
    val isLight = storeConfig.themeMode == "light"

    Column(modifier = modifier.fillMaxSize()) {
        VerifiedStoreHeader(
            storeConfig = storeConfig,
            bellPulseCount = bellPulseCount,
            onBellClick = onBellClick,
            onStoreHeaderClick = onOpenOwnerStudio
        )

        Box(modifier = Modifier.weight(1f)) {
            AnimatedChatBackground(
                style = bgStyle,
                accentColor = MaterialTheme.colorScheme.primary,
                isLightTheme = isLight
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Quick Actions Strip: Shipment tracking + Sound FX + Quick AI Prompts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { showSoundDrawer = !showSoundDrawer },
                        label = { Text("أصوات التنبيه والتيربو") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ),
                        modifier = Modifier.testTag("toggle_sound_drawer_chip")
                    )

                    shipments.firstOrNull()?.let { latestShipment ->
                        AssistChip(
                            onClick = { onSendShipmentUpdate(latestShipment) },
                            label = { Text("تتبع الشحنة ${latestShipment.trackingCode}") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.LocalShipping,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                            )
                        )
                    }

                    AssistChip(
                        onClick = { onSendMessage("اعرض لي أفضل المنتجات المتوفرة وأسعارها") },
                        label = { Text("اسأل AI عن المنتجات") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    )
                }

                // Expandable Sound Effects Bar (8 requested vehicle & FX sounds)
                AnimatedVisibility(visible = showSoundDrawer) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "اضغط للتشغيل أو الإرسال في المحادثة:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SoundEffectType.entries.forEach { fx ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { onSendSoundToChat(fx) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.GraphicEq,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = fx.arabicName,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            storeConfig = storeConfig,
                            product = products.firstOrNull { it.id == msg.productId },
                            shipment = shipments.firstOrNull { it.id == msg.shipmentId },
                            onOrderProduct = onOrderProduct,
                            onPlaySound = onPlaySound
                        )
                    }

                    if (aiReplyState is UiState.Loading) {
                        item(key = "ai_typing") {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                tonalElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "الذكاء الاصطناعي يجهز الرد...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    if (aiReplyState is UiState.Error) {
                        item(key = "ai_error") {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = aiReplyState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Chat Input Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "اكتب رسالتك للمتجر الذكي...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        onSendMessage(inputText)
                                        inputText = ""
                                    }
                                },
                                modifier = Modifier.testTag("send_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "إرسال الرسالة",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    storeConfig: StoreConfig,
    product: StoreProduct?,
    shipment: ShipmentOrder?,
    onOrderProduct: (StoreProduct) -> Unit,
    onPlaySound: (SoundEffectType) -> Unit
) {
    val isCustomer = message.senderType == "customer"
    val alignment = if (isCustomer) Alignment.End else Alignment.Start
    val bubbleColor = if (isCustomer) {
        if (storeConfig.themeMode == "light") Color(0xFFD9FDD3) else Color(0xFF005C4B)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isCustomer && storeConfig.themeMode != "light") Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isCustomer) 18.dp else 4.dp,
                bottomEnd = if (isCustomer) 4.dp else 18.dp
            ),
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isCustomer) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = storeConfig.storeName,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (storeConfig.isVerified) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "موثق",
                                tint = VerifiedBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Surface(
                            color = VerifiedBlue.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = storeConfig.badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = VerifiedBlue,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Protected Animated Video Reel Card if attached
                if (message.mediaType == "reel") {
                    AnimatedStoreVideoReelCard(
                        title = message.mediaLabel.ifBlank { "استعراض منتجات المتجر" },
                        accentColor = MaterialTheme.colorScheme.primary,
                        copyBlocked = storeConfig.blockMediaCopy
                    )
                }

                // Text body (non-selectable when copy protection is enabled)
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )

                // Sound effect playable chip
                if (message.soundEffectId.isNotBlank()) {
                    val fx = SoundEffectType.fromId(message.soundEffectId)
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onPlaySound(fx) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "تشغيل الصوت",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "تشغيل: ${fx.arabicName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Inline recommended Product Card
                if (product != null) {
                    InlineChatProductCard(
                        product = product,
                        copyBlocked = storeConfig.blockMediaCopy,
                        onOrderClick = onOrderProduct
                    )
                }

                // Inline Shipment Tracking Stepper Card
                if (shipment != null) {
                    InlineShipmentTrackingCard(shipment = shipment)
                }

                // Expiry footer indicator
                if (message.expiresAtEpochMs > 0L) {
                    val remainingHours = ((message.expiresAtEpochMs - System.currentTimeMillis()) / (1000 * 60 * 60)).coerceAtLeast(0)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.65f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "يختفي خلال ~$remainingHours ساعة",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
    }
}
