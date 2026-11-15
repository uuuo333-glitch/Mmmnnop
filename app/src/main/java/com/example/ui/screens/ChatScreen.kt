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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatMessage
import com.example.data.model.ShipmentOrder
import com.example.data.model.SoundEffectType
import com.example.data.model.StoreConfig
import com.example.data.model.StoreProduct
import com.example.ui.UiState
import com.example.ui.components.AnimatedStoreVideoReelCard
import com.example.ui.components.ChatDynamicBackdrop
import com.example.ui.components.DeviceChatVideoCard
import com.example.ui.components.InlineChatProductCard
import com.example.ui.components.InlineShipmentTrackingCard
import com.example.ui.components.VerifiedStoreHeader
import com.example.ui.components.YouTubeChatVideoCard
import com.example.ui.components.extractYouTubeVideoId
import com.example.ui.theme.VerifiedBlue

@Composable
fun ChatScreen(
    storeConfig: StoreConfig,
    messages: List<ChatMessage>,
    products: List<StoreProduct>,
    shipments: List<ShipmentOrder>,
    aiReplyState: UiState<Unit>,
    aiBgGenState: UiState<Int> = UiState.Idle,
    bellPulseCount: Int,
    isOwnerMode: Boolean = false,
    onBellClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onOrderProduct: (StoreProduct) -> Unit,
    onPlaySound: (SoundEffectType) -> Unit,
    onSendSoundToChat: (SoundEffectType) -> Unit,
    onTriggerOwnerAlert: (SoundEffectType) -> Unit = {},
    onSendShipmentUpdate: (ShipmentOrder) -> Unit,
    onChangeShipmentStatus: (ShipmentOrder, Int, String) -> Unit = { _, _, _ -> },
    onUpdateBgRotation: (Boolean, Int) -> Unit = { _, _ -> },
    onGenerateAiRotatingBackgrounds: (String, Int, Int) -> Unit = { _, _, _ -> },
    onToggleChatInternetSignal: () -> Unit = {},
    onBlockedMediaAttempt: () -> Unit = {},
    onSaveTickerText: (String) -> Unit = {},
    onOpenOwnerStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showSoundDrawer by remember { mutableStateOf(false) }
    var showEditTickerDialog by remember { mutableStateOf(false) }
    var editingShipment by remember { mutableStateOf<ShipmentOrder?>(null) }
    var showBgQuickDialog by remember { mutableStateOf(false) }
    var tickerDraft by remember(storeConfig.tickerText) { mutableStateOf(storeConfig.tickerText) }
    var showPinnedYouTube by remember(storeConfig.chatBgMediaType, storeConfig.chatBgMediaUrl) {
        mutableStateOf(
            storeConfig.chatBgMediaType == "youtube" &&
                extractYouTubeVideoId(storeConfig.chatBgMediaUrl) != null
        )
    }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isLight = storeConfig.themeMode == "light"

    Column(modifier = modifier.fillMaxSize()) {
        VerifiedStoreHeader(
            storeConfig = storeConfig,
            bellPulseCount = bellPulseCount,
            onBellClick = onBellClick,
            onStoreHeaderClick = onOpenOwnerStudio,
            isOwnerMode = isOwnerMode,
            onEditTickerClick = {
                tickerDraft = storeConfig.tickerText
                showEditTickerDialog = true
            }
        )

        if (storeConfig.subscriptionRequired) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "اشتراك المتجر المحدد من المالك: ${storeConfig.monthlySubscriptionPrice.toInt()} ${storeConfig.subscriptionCurrency} لمدة ${storeConfig.subscriptionMonths} شهر و ${storeConfig.subscriptionDays} يوم",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        if (storeConfig.chatOfflineMode) {
            Surface(
                color = Color(0xFF92400E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WifiOff,
                            contentDescription = "انقطاع الإنترنت عن المحادثة",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "إشارة الإنترنت مقطوعة عن المحادثة مؤقتاً (بدون حظر)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                    OutlinedButton(
                        onClick = onToggleChatInternetSignal,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("إعادة الإشارة", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            ChatDynamicBackdrop(
                storeConfig = storeConfig,
                accentColor = MaterialTheme.colorScheme.primary,
                isLightTheme = isLight,
                onBlockedMediaAttempt = onBlockedMediaAttempt
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Quick Actions Strip: Chat Internet Signal + Sound FX + Shipment Tracking + Quick AI Prompts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = onToggleChatInternetSignal,
                        label = {
                            Text(
                                if (storeConfig.chatOfflineMode) "إعادة النت للمحادثة"
                                else "قطع النت عن المحادثة"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (storeConfig.chatOfflineMode) Icons.Rounded.WifiOff else Icons.Rounded.Wifi,
                                contentDescription = null,
                                tint = if (storeConfig.chatOfflineMode) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ),
                        modifier = Modifier.testTag("toggle_chat_offline_chip")
                    )

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

                    if (isOwnerMode && shipments.isNotEmpty()) {
                        AssistChip(
                            onClick = { editingShipment = shipments.first() },
                            label = { Text("تبديل حالة الطلب") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.SwapHoriz,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
                            ),
                            modifier = Modifier.testTag("owner_switch_order_status_chip")
                        )
                    }

                    if (isOwnerMode) {
                        AssistChip(
                            onClick = { showBgQuickDialog = true },
                            label = {
                                Text(
                                    if (storeConfig.chatBgAutoRotate) {
                                        "خلفيات متغيرة (${storeConfig.chatBgRotateIntervalSec.coerceAtLeast(5)}ث) + AI"
                                    } else {
                                        "خلفيات متغيرة كل 5ث + AI"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Wallpaper,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                            ),
                            modifier = Modifier.testTag("owner_chat_bg_studio_chip")
                        )
                    }

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

                // Expandable Sound Effects & Owner Alert Bar
                AnimatedVisibility(visible = showSoundDrawer) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isOwnerMode) {
                                        "اضغط أي صوت لتنبيه العميل فوراً:"
                                    } else {
                                        "اضغط للتشغيل أو الإرسال في المحادثة:"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isOwnerMode) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.NotificationsActive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "وضع تنبيه المالك",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
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
                                            .clickable {
                                                if (isOwnerMode) {
                                                    onTriggerOwnerAlert(fx)
                                                } else {
                                                    onSendSoundToChat(fx)
                                                }
                                            }
                                            .testTag("chat_sound_${fx.id}")
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

                // Pinned YouTube Video Player when owner configures a YouTube chat backdrop
                if (storeConfig.chatBgMediaType == "youtube" &&
                    extractYouTubeVideoId(storeConfig.chatBgMediaUrl) != null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "فيديو يوتيوب المحادثة (مضاف من المالك)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { showPinnedYouTube = !showPinnedYouTube },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(if (showPinnedYouTube) "تصغير" else "إظهار الفيديو")
                            }
                        }
                        AnimatedVisibility(visible = showPinnedYouTube) {
                            YouTubeChatVideoCard(
                                youtubeUrlOrId = storeConfig.chatBgMediaUrl,
                                copyBlocked = storeConfig.blockMediaCopy,
                                onBlockedMediaAttempt = onBlockedMediaAttempt
                            )
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
                            isOwnerMode = isOwnerMode,
                            product = products.firstOrNull { it.id == msg.productId },
                            shipment = shipments.firstOrNull { it.id == msg.shipmentId },
                            onOrderProduct = onOrderProduct,
                            onPlaySound = onPlaySound,
                            onChangeShipmentStatus = onChangeShipmentStatus,
                            onOpenStatusDialog = { ship -> editingShipment = ship },
                            onBlockedMediaAttempt = onBlockedMediaAttempt
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
                                    text = if (storeConfig.chatOfflineMode) {
                                        "إشارة الإنترنت مقطوعة مؤقتاً (بدون حظر)..."
                                    } else {
                                        storeConfig.chatInputPlaceholder.ifBlank { "اكتب رسالتك للمتجر الذكي..." }
                                    },
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
                            color = if (storeConfig.chatOfflineMode) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        onSendMessage(inputText)
                                        if (!storeConfig.chatOfflineMode) {
                                            inputText = ""
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("send_chat_button")
                            ) {
                                Icon(
                                    imageVector = if (storeConfig.chatOfflineMode) Icons.Rounded.WifiOff else Icons.AutoMirrored.Rounded.Send,
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

    if (showEditTickerDialog) {
        AlertDialog(
            onDismissRequest = { showEditTickerDialog = false },
            title = { Text("تعديل الشريط العلوي المتحرك") },
            text = {
                OutlinedTextField(
                    value = tickerDraft,
                    onValueChange = { tickerDraft = it },
                    label = { Text("نص الشريط المتحرك للمالك") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inline_ticker_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveTickerText(tickerDraft.trim())
                        showEditTickerDialog = false
                    }
                ) {
                    Text("حفظ ونشر")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTickerDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
    editingShipment?.let { targetShipment ->
        OrderStatusSwitcherDialog(
            initialShipment = targetShipment,
            allShipments = shipments,
            onDismiss = { editingShipment = null },
            onApplyStatus = { ship, step, customLabel ->
                onChangeShipmentStatus(ship, step, customLabel)
                editingShipment = null
            }
        )
    }

    if (showBgQuickDialog) {
        OwnerChatBackdropQuickDialog(
            storeConfig = storeConfig,
            aiBgGenState = aiBgGenState,
            onDismiss = { showBgQuickDialog = false },
            onUpdateBgRotation = onUpdateBgRotation,
            onGenerateAiBackgrounds = { prompt, intervalSec ->
                onGenerateAiRotatingBackgrounds(prompt, 3, intervalSec)
            }
        )
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    storeConfig: StoreConfig,
    isOwnerMode: Boolean,
    product: StoreProduct?,
    shipment: ShipmentOrder?,
    onOrderProduct: (StoreProduct) -> Unit,
    onPlaySound: (SoundEffectType) -> Unit,
    onChangeShipmentStatus: (ShipmentOrder, Int, String) -> Unit,
    onOpenStatusDialog: (ShipmentOrder) -> Unit,
    onBlockedMediaAttempt: () -> Unit = {}
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

                // Protected Animated Video Reel, Uploaded Device Video, or YouTube Card if attached
                when (message.mediaType) {
                    "reel" -> {
                        AnimatedStoreVideoReelCard(
                            title = message.mediaLabel.ifBlank { "استعراض منتجات المتجر" },
                            accentColor = MaterialTheme.colorScheme.primary,
                            copyBlocked = storeConfig.blockMediaCopy,
                            onBlockedMediaAttempt = onBlockedMediaAttempt
                        )
                    }
                    "youtube" -> {
                        YouTubeChatVideoCard(
                            youtubeUrlOrId = message.mediaLabel,
                            copyBlocked = storeConfig.blockMediaCopy,
                            onBlockedMediaAttempt = onBlockedMediaAttempt
                        )
                    }
                    "video" -> {
                        DeviceChatVideoCard(
                            fileName = message.mediaLabel,
                            copyBlocked = storeConfig.blockMediaCopy,
                            onBlockedMediaAttempt = onBlockedMediaAttempt
                        )
                    }
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
                        onOrderClick = onOrderProduct,
                        onBlockedMediaAttempt = onBlockedMediaAttempt
                    )
                }

                // Inline Shipment Tracking Stepper Card + Owner Live Status Switcher
                if (shipment != null) {
                    InlineShipmentTrackingCard(shipment = shipment)

                    if (isOwnerMode) {
                        val quickStages = listOf(
                            "تأكيد",
                            "تجهيز",
                            "شحن",
                            "بالطريق",
                            "تم التسليم"
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "تبديل حالة الطلب مباشرة (للمالك):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickStages.forEachIndexed { idx, label ->
                                    FilterChip(
                                        selected = shipment.currentStep == idx,
                                        onClick = { onChangeShipmentStatus(shipment, idx, "") },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.testTag("in_chat_stage_${shipment.id}_$idx")
                                    )
                                }
                                AssistChip(
                                    onClick = { onOpenStatusDialog(shipment) },
                                    label = { Text("نص مخصص", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
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

@Composable
private fun OrderStatusSwitcherDialog(
    initialShipment: ShipmentOrder,
    allShipments: List<ShipmentOrder>,
    onDismiss: () -> Unit,
    onApplyStatus: (ShipmentOrder, Int, String) -> Unit
) {
    var selectedShipment by remember(initialShipment.id) { mutableStateOf(initialShipment) }
    var selectedStep by remember(selectedShipment.id) { mutableIntStateOf(selectedShipment.currentStep) }
    var customStatusLabel by remember(selectedShipment.id) { mutableStateOf("") }

    val stageLabels = listOf(
        "تم تأكيد الطلب",
        "قيد التجهيز والتغليف",
        "تم التسليم لشركة الشحن",
        "في الطريق إليك",
        "تم التسليم بنجاح"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("تبديل حالة الطلب في المحادثة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (allShipments.size > 1) {
                    Text("اختر الطلب:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allShipments.forEach { item ->
                            FilterChip(
                                selected = selectedShipment.id == item.id,
                                onClick = {
                                    selectedShipment = item
                                    selectedStep = item.currentStep
                                },
                                label = { Text(item.trackingCode) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "الطلب: ${selectedShipment.trackingCode} (${selectedShipment.customerName})",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Text("اختر المرحلة الجديدة:", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    stageLabels.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedStep == idx,
                            onClick = { selectedStep = idx },
                            label = { Text("${idx + 1}. $label") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_order_stage_$idx")
                        )
                    }
                }

                OutlinedTextField(
                    value = customStatusLabel,
                    onValueChange = { customStatusLabel = it },
                    label = { Text("نص حالة مخصص (اختياري)") },
                    placeholder = { Text(stageLabels[selectedStep.coerceIn(0, 4)]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyStatus(selectedShipment, selectedStep, customStatusLabel) },
                modifier = Modifier.testTag("confirm_switch_order_status_button")
            ) {
                Text("تحديث الحالة بالمحادثة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun OwnerChatBackdropQuickDialog(
    storeConfig: StoreConfig,
    aiBgGenState: UiState<Int>,
    onDismiss: () -> Unit,
    onUpdateBgRotation: (Boolean, Int) -> Unit,
    onGenerateAiBackgrounds: (String, Int) -> Unit
) {
    var autoRotate by remember(storeConfig.chatBgAutoRotate) { mutableStateOf(storeConfig.chatBgAutoRotate) }
    var intervalText by remember(storeConfig.chatBgRotateIntervalSec) {
        mutableStateOf(storeConfig.chatBgRotateIntervalSec.coerceAtLeast(5).toString())
    }
    var aiThemePrompt by remember { mutableStateOf("أجواء نيون وتيربو فاخرة ومتحركة ليلاً") }
    val safeInterval = (intervalText.toIntOrNull() ?: 5).coerceIn(5, 300)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Rounded.Wallpaper, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("تبديل خلفيات المحادثة + الذكاء الاصطناعي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تبديل الخلفيات تلقائياً", style = MaterialTheme.typography.labelLarge)
                    Switch(
                        checked = autoRotate,
                        onCheckedChange = {
                            autoRotate = it
                            onUpdateBgRotation(it, safeInterval)
                        }
                    )
                }

                Text("المدة بين كل خلفية (5 ثوانٍ فأكثر):", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 8, 10, 15, 30).forEach { sec ->
                        FilterChip(
                            selected = safeInterval == sec,
                            onClick = {
                                intervalText = sec.toString()
                                autoRotate = true
                                onUpdateBgRotation(true, sec)
                            },
                            label = { Text("$sec ث") }
                        )
                    }
                }
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { intervalText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("مدة التبديل بالثواني (الحد الأدنى 5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = aiThemePrompt,
                    onValueChange = { aiThemePrompt = it },
                    label = { Text("وصف الخلفيات للذكاء الاصطناعي (AI)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onGenerateAiBackgrounds(aiThemePrompt, safeInterval) },
                    enabled = aiBgGenState !is UiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_quick_ai_bg_generate_button")
                ) {
                    if (aiBgGenState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الذكاء الاصطناعي يصمم الخلفيات...")
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("توليد 3 خلفيات متحركة بالذكاء الاصطناعي")
                    }
                }

                if (aiBgGenState is UiState.Error) {
                    Text(
                        text = aiBgGenState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateBgRotation(autoRotate, safeInterval)
                    onDismiss()
                }
            ) {
                Text("حفظ المدة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
