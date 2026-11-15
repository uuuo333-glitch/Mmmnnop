package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatBackgroundStyle
import com.example.data.model.DisappearingMode
import com.example.data.model.ShipmentOrder
import com.example.data.model.SoundEffectType
import com.example.data.model.StoreConfig
import com.example.data.model.UserProfile
import com.example.ui.UiState
import com.example.ui.components.BgPlaylistEntry
import com.example.ui.components.copyUriToAppMediaFile
import com.example.ui.components.extractYouTubeVideoId
import com.example.ui.components.parseBgPlaylist
import com.example.ui.components.resolveStoreIcon
import com.example.ui.theme.AccentColorOptions
import com.example.ui.theme.VerifiedBlue
import com.example.ui.theme.parseAccentColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnerDashboardScreen(
    userProfile: UserProfile?,
    storeConfig: StoreConfig,
    shipments: List<ShipmentOrder>,
    aiBgGenState: UiState<Int> = UiState.Idle,
    onSaveConfig: (StoreConfig) -> Unit,
    onSaveThreeNumbers: (p1: String, c1: String, p2: String, c2: String, p3: String, c3: String) -> Unit,
    onPlaySound: (SoundEffectType) -> Unit,
    onTriggerOwnerAlert: (SoundEffectType) -> Unit = {},
    onSaveChatMedia: (mediaType: String, mediaUrl: String) -> Unit = { _, _ -> },
    onAddMultipleBgMedia: (List<BgPlaylistEntry>) -> Unit = {},
    onRemoveBgPlaylistItem: (Int) -> Unit = {},
    onClearBgPlaylist: () -> Unit = {},
    onUpdateBgRotation: (Boolean, Int) -> Unit = { _, _ -> },
    onGenerateAiRotatingBackgrounds: (themePrompt: String, frameCount: Int, intervalSec: Int) -> Unit = { _, _, _ -> },
    onAdvanceShipment: (ShipmentOrder) -> Unit,
    onChangeShipmentStatus: (ShipmentOrder, Int, String) -> Unit = { _, _, _ -> },
    onCreateShipment: (trackingCode: String, customerName: String, carrier: String, etaText: String) -> Unit,
    onToggleRole: (String) -> Unit,
    onToggleSubscription: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var draft by remember(storeConfig) { mutableStateOf(storeConfig) }
    var subPriceText by remember(storeConfig.monthlySubscriptionPrice) {
        mutableStateOf(storeConfig.monthlySubscriptionPrice.toInt().toString())
    }
    var subMonthsText by remember(storeConfig.subscriptionMonths) {
        mutableStateOf(storeConfig.subscriptionMonths.toString())
    }
    var subDaysText by remember(storeConfig.subscriptionDays) {
        mutableStateOf(storeConfig.subscriptionDays.toString())
    }
    var youtubeInput by remember(storeConfig.chatBgMediaUrl, storeConfig.chatBgMediaType) {
        mutableStateOf(if (storeConfig.chatBgMediaType == "youtube") storeConfig.chatBgMediaUrl else "")
    }
    var bgIntervalText by remember(storeConfig.chatBgRotateIntervalSec) {
        mutableStateOf(storeConfig.chatBgRotateIntervalSec.coerceAtLeast(5).toString())
    }
    var aiBgPrompt by remember { mutableStateOf("أجواء نيون وتيربو فاخرة ومتحركة ليلاً") }
    var mediaStatusMessage by remember { mutableStateOf<String?>(null) }

    var phone1 by remember(userProfile?.phoneNumber) { mutableStateOf(userProfile?.phoneNumber.orEmpty()) }
    var code1 by remember(userProfile?.countryCode) { mutableStateOf(userProfile?.countryCode.orEmpty().ifBlank { "+966" }) }
    var phone2 by remember(userProfile?.phoneNumber2) { mutableStateOf(userProfile?.phoneNumber2.orEmpty()) }
    var code2 by remember(userProfile?.countryCode2) { mutableStateOf(userProfile?.countryCode2.orEmpty().ifBlank { "+966" }) }
    var phone3 by remember(userProfile?.phoneNumber3) { mutableStateOf(userProfile?.phoneNumber3.orEmpty()) }
    var code3 by remember(userProfile?.countryCode3) { mutableStateOf(userProfile?.countryCode3.orEmpty().ifBlank { "+966" }) }
    var newShipCustomer by remember { mutableStateOf("") }
    var newShipCarrier by remember { mutableStateOf("سمسا السريع") }

    val bgPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(6)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val ts = System.currentTimeMillis()
                val added = mutableListOf<BgPlaylistEntry>()
                uris.forEachIndexed { idx, uri ->
                    val res = copyUriToAppMediaFile(context, uri, "chat_bg_p_${ts}_$idx.jpg")
                    res.onSuccess { fileName ->
                        added.add(BgPlaylistEntry("photo", fileName))
                    }
                }
                if (added.isNotEmpty()) {
                    onAddMultipleBgMedia(added)
                    mediaStatusMessage = "تمت إضافة ${added.size} صور متحركة تدور تلقائياً كل ${bgIntervalText.toIntOrNull()?.coerceAtLeast(5) ?: 5} ثوانٍ"
                } else {
                    mediaStatusMessage = "تعذر تحميل الصور المختارة"
                }
            }
        }
    }

    val bgVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(6)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val ts = System.currentTimeMillis()
                val added = mutableListOf<BgPlaylistEntry>()
                uris.forEachIndexed { idx, uri ->
                    val res = copyUriToAppMediaFile(context, uri, "chat_bg_v_${ts}_$idx.mp4")
                    res.onSuccess { fileName ->
                        added.add(BgPlaylistEntry("video", fileName))
                    }
                }
                if (added.isNotEmpty()) {
                    onAddMultipleBgMedia(added)
                    mediaStatusMessage = "تمت إضافة ${added.size} مقاطع فيديو لقائمة خلفيات المحادثة"
                } else {
                    mediaStatusMessage = "تعذر تحميل مقاطع الفيديو المختارة"
                }
            }
        }
    }

    val storeLogoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = copyUriToAppMediaFile(context, uri, "store_logo.jpg")
                res.onSuccess { fileName ->
                    val updated = draft.copy(storeLogoFile = fileName)
                    draft = updated
                    onSaveConfig(updated)
                    mediaStatusMessage = "تم تحديث شعار المتجر بنجاح"
                }.onFailure { err ->
                    mediaStatusMessage = err.localizedMessage ?: "تعذر تحميل الشعار"
                }
            }
        }
    }

    LaunchedEffect(storeConfig) {
        draft = storeConfig
        subPriceText = storeConfig.monthlySubscriptionPrice.toInt().toString()
        subMonthsText = storeConfig.subscriptionMonths.toString()
        subDaysText = storeConfig.subscriptionDays.toString()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "لوحة المالك والهوية",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = VerifiedBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = if (draft.hideOwnerLogin) {
                            "حساب المالك محمي • بيانات الدخول مخفية عن الآخرين"
                        } else {
                            "${userProfile?.countryCode.orEmpty()} ${userProfile?.phoneNumber.orEmpty()} • وضع المالك"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(onClick = onSignOut) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "خروج", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("خروج")
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Store Name, Badge & Ticker Customization
            item(key = "section_identity") {
                DashboardCard(title = "هوية المتجر والشريط المتحرك", icon = Icons.Rounded.Storefront) {
                    OutlinedTextField(
                        value = draft.storeName,
                        onValueChange = { draft = draft.copy(storeName = it) },
                        label = { Text("اسم المتجر (يظهر بالشارة الزرقاء)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_store_name_input")
                    )
                    OutlinedTextField(
                        value = draft.badgeLabel,
                        onValueChange = { draft = draft.copy(badgeLabel = it) },
                        label = { Text("الوسم تحت إشارة التوثيق (مثال: واتساب الأعمال • المتجر)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.tagline,
                        onValueChange = { draft = draft.copy(tagline = it) },
                        label = { Text("وصف المتجر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.catalogHeaderTitle,
                        onValueChange = { draft = draft.copy(catalogHeaderTitle = it) },
                        label = { Text("عنوان صفحة المتجر (اتركه فارغاً لاستخدام اسم المتجر)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.orderButtonText,
                        onValueChange = { draft = draft.copy(orderButtonText = it) },
                        label = { Text("نص زر الطلب داخل المتجر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.chatInputPlaceholder,
                        onValueChange = { draft = draft.copy(chatInputPlaceholder = it) },
                        label = { Text("نص مربع الكتابة داخل المحادثة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("أيقونة المتجر والوسم:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val iconChoices = listOf(
                            "storefront" to "متجر",
                            "turbo" to "تيربو",
                            "rocket" to "صاروخ",
                            "diamond" to "ماسة",
                            "bag" to "حقيبة",
                            "shield" to "درع موثق"
                        )
                        iconChoices.forEach { (key, label) ->
                            FilterChip(
                                selected = draft.storeIconKey == key,
                                onClick = {
                                    draft = draft.copy(storeIconKey = key)
                                    onSaveConfig(draft)
                                },
                                leadingIcon = {
                                    Icon(resolveStoreIcon(key), contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            storeLogoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_store_logo_button")
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تغيير صورة/شعار المتجر من الجهاز")
                    }

                    OutlinedTextField(
                        value = draft.tickerText,
                        onValueChange = { draft = draft.copy(tickerText = it) },
                        label = { Text("الشريط العلوي المتحرك في الدردشة") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_ticker_input")
                    )
                    OutlinedButton(
                        onClick = { onSaveConfig(draft) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_ticker_now_button")
                    ) {
                        Text("تطبيق النص على الشريط المتحرك الآن")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل الشارة الزرقاء تحت الاسم")
                        Switch(
                            checked = draft.isVerified,
                            onCheckedChange = { draft = draft.copy(isVerified = it) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = userProfile?.role != "customer",
                            onClick = { onToggleRole("owner") },
                            label = { Text("وضع المالك") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = userProfile?.role == "customer",
                            onClick = { onToggleRole("customer") },
                            label = { Text("معاينة كعميل") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Theme, Accent Color & Animated Chat Background
            item(key = "section_theme") {
                DashboardCard(title = "الثيم والألوان والخلفيات المتحركة", icon = Icons.Rounded.ColorLens) {
                    Text("نمط الثيم:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("dark" to "داكن", "midnight" to "ليلي فاخر", "light" to "فاتح").forEach { (mode, label) ->
                            FilterChip(
                                selected = draft.themeMode == mode,
                                onClick = {
                                    draft = draft.copy(themeMode = mode)
                                    onSaveConfig(draft)
                                },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("اللون المميز للمتجر:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AccentColorOptions.forEach { (hex, label) ->
                            val col = parseAccentColor(hex)
                            val selected = draft.accentColorHex.equals(hex, ignoreCase = true)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    draft = draft.copy(accentColorHex = hex)
                                    onSaveConfig(draft)
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                )
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Text("الخلفية المتحركة للمحادثة:", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChatBackgroundStyle.entries.forEach { bg ->
                            FilterChip(
                                selected = draft.chatBgMediaType == "animated" && draft.chatBackgroundId == bg.id,
                                onClick = {
                                    draft = draft.copy(chatBackgroundId = bg.id, chatBgMediaType = "animated")
                                    onSaveConfig(draft)
                                },
                                label = { Text(bg.arabicName) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "خلفيات متغيرة كل 5 ثوانٍ أو أكثر (صور متحركة / فيديو / ذكاء اصطناعي):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val safeBgSec = (bgIntervalText.toIntOrNull() ?: 5).coerceIn(5, 300)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تبديل الخلفيات تلقائياً", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "يدور بين الصور المتحركة والفيديوهات كل $safeBgSec ثوانٍ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = draft.chatBgAutoRotate,
                            onCheckedChange = { enabled ->
                                draft = draft.copy(
                                    chatBgAutoRotate = enabled,
                                    chatBgRotateIntervalSec = safeBgSec
                                )
                                onUpdateBgRotation(enabled, safeBgSec)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 8, 10, 15, 30).forEach { sec ->
                            FilterChip(
                                selected = safeBgSec == sec,
                                onClick = {
                                    bgIntervalText = sec.toString()
                                    draft = draft.copy(chatBgAutoRotate = true, chatBgRotateIntervalSec = sec)
                                    onUpdateBgRotation(true, sec)
                                },
                                label = { Text("كل $sec ثوانٍ") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = bgIntervalText,
                            onValueChange = { bgIntervalText = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = { Text("المدة بالثواني (5 فأكثر)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("owner_bg_interval_input")
                        )
                        OutlinedButton(
                            onClick = {
                                draft = draft.copy(chatBgAutoRotate = true, chatBgRotateIntervalSec = safeBgSec)
                                onUpdateBgRotation(true, safeBgSec)
                            }
                        ) {
                            Text("تطبيق المدة")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                bgPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_chat_bg_photo_button")
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("صور متحركة (متعدد)")
                        }
                        OutlinedButton(
                            onClick = {
                                bgVideoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_chat_bg_video_button")
                        ) {
                            Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فيديوهات (متعدد)")
                        }
                    }

                    // AI Rotating Chat Background Generator
                    OutlinedTextField(
                        value = aiBgPrompt,
                        onValueChange = { aiBgPrompt = it },
                        label = { Text("وصف الخلفيات المتحركة للذكاء الاصطناعي (AI)") },
                        placeholder = { Text("مثال: سيارات تيربو وإضاءة نيون ليلية متغيرة") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_ai_bg_prompt_input")
                    )
                    Button(
                        onClick = { onGenerateAiRotatingBackgrounds(aiBgPrompt, 3, safeBgSec) },
                        enabled = aiBgGenState !is UiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_generate_ai_bg_playlist_button")
                    ) {
                        if (aiBgGenState is UiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جاري توليد 3 خلفيات بالذكاء الاصطناعي...")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("توليد خلفيات تتغير كل $safeBgSec ثوانٍ بالذكاء الاصطناعي")
                        }
                    }
                    if (aiBgGenState is UiState.Error) {
                        Text(
                            text = aiBgGenState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    val playlistEntries = remember(storeConfig.chatBgPlaylist) {
                        parseBgPlaylist(storeConfig.chatBgPlaylist)
                    }
                    if (playlistEntries.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "قائمة الخلفيات الدوارة (${playlistEntries.size}):",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                TextButton(onClick = onClearBgPlaylist) {
                                    Text("مسح القائمة")
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                playlistEntries.forEachIndexed { idx, entry ->
                                    val label = when (entry.type) {
                                        "ai" -> "خلفية AI #${idx + 1}"
                                        "video" -> "فيديو #${idx + 1}"
                                        "photo" -> "صورة #${idx + 1}"
                                        else -> "حركة #${idx + 1}"
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(label, style = MaterialTheme.typography.labelMedium)
                                            IconButton(
                                                onClick = { onRemoveBgPlaylistItem(idx) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "حذف",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = youtubeInput,
                        onValueChange = {
                            youtubeInput = it
                            mediaStatusMessage = null
                        },
                        label = { Text("رابط فيديو يوتيوب (YouTube URL أو ID)") },
                        placeholder = { Text("https://youtu.be/...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_youtube_url_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val vidId = extractYouTubeVideoId(youtubeInput)
                                if (vidId == null) {
                                    mediaStatusMessage = "أدخل رابط يوتيوب أو معرف فيديو صحيح"
                                } else {
                                    val cleanUrl = youtubeInput.trim()
                                    draft = draft.copy(chatBgMediaType = "youtube", chatBgMediaUrl = cleanUrl)
                                    onSaveChatMedia("youtube", cleanUrl)
                                    mediaStatusMessage = "تم تشغيل فيديو يوتيوب داخل المحادثة"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("apply_youtube_chat_button")
                        ) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة يوتيوب")
                        }
                        TextButton(
                            onClick = {
                                draft = draft.copy(chatBgMediaType = "animated", chatBgMediaUrl = "", chatBgAutoRotate = false, chatBgPlaylist = "")
                                onClearBgPlaylist()
                                mediaStatusMessage = "تمت استعادة الخلفية المتحركة الافتراضية"
                            }
                        ) {
                            Text("إعادة الافتراضي")
                        }
                    }
                    if (!mediaStatusMessage.isNullOrBlank()) {
                        Text(
                            text = mediaStatusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 3A. Instant Owner Customer Alert Pad (1-tap sound alerts to notify customer)
            item(key = "section_owner_instant_alerts") {
                DashboardCard(title = "أصوات تنبيهات المالك الفورية للعميل", icon = Icons.Rounded.Campaign) {
                    Text(
                        text = "اضغط على أي صوت لإطلاقه وتنبيه العميل فوراً (سيارة مسرعة، تيربو، ماكينة، فرقعة شكمان، صاروخ متعدي، طائرة، دراجة نارية مسرعة):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SoundEffectType.entries.forEach { fx ->
                            Button(
                                onClick = { onTriggerOwnerAlert(fx) },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("owner_alert_btn_${fx.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(fx.arabicName, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // 3B. Default Customer Bell Horn Selector
            item(key = "section_sounds") {
                DashboardCard(title = "جرس التنبيه الافتراضي وصوتيات المحادثة", icon = Icons.Rounded.NotificationsActive) {
                    Text(
                        text = "اختر الصوت الذي ينطلق عند ضغط العميل على جرس التنبيه:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SoundEffectType.entries.forEach { fx ->
                            val selected = draft.activeAlertSound == fx.id
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        draft = draft.copy(activeAlertSound = fx.id)
                                        onPlaySound(fx)
                                        onSaveConfig(draft)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.GraphicEq,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(fx.arabicName, style = MaterialTheme.typography.labelLarge)
                                    }
                                    Text(
                                        text = if (selected) "✓ نشط" else "تجربة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Privacy: Screen Recording, Screenshot Blocking, Media Copy Blocking, Offline Chat & Disappearing Timer
            item(key = "section_privacy") {
                DashboardCard(title = "الحماية ومنع التسجيل وحفظ الوسائط", icon = Icons.Rounded.Security) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("منع تسجيل الشاشة ولقطات الشاشة", style = MaterialTheme.typography.labelLarge)
                            Text("يحجب تسجيل الشاشة الفيديو ولقطات الشاشة (FLAG_SECURE)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = draft.blockScreenshots,
                            onCheckedChange = {
                                draft = draft.copy(blockScreenshots = it)
                                onSaveConfig(draft)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("منع حفظ الصور ومقاطع الفيديو", style = MaterialTheme.typography.labelLarge)
                            Text("يمنع حفظ أو نسخ الصور ومقاطع الفيديو داخل المتجر والدردشة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = draft.blockMediaCopy,
                            onCheckedChange = {
                                draft = draft.copy(blockMediaCopy = it)
                                onSaveConfig(draft)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("قطع إشارة الإنترنت عن المحادثة (بدون حظر)", style = MaterialTheme.typography.labelLarge)
                            Text("يوقف إرسال/استقبال الرسائل مؤقتاً دون حظر العميل", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = draft.chatOfflineMode,
                            onCheckedChange = {
                                draft = draft.copy(chatOfflineMode = it)
                                onSaveConfig(draft)
                            }
                        )
                    }

                    Text("مؤقت اختفاء الكلام:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DisappearingMode.entries.forEach { mode ->
                            FilterChip(
                                selected = draft.disappearingMode == mode.id,
                                onClick = {
                                    draft = draft.copy(disappearingMode = mode.id)
                                    onSaveConfig(draft)
                                },
                                label = { Text(mode.arabicName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4B. 3 WhatsApp Business Phone Numbers Manager
            item(key = "section_three_numbers") {
                DashboardCard(title = "أرقام الواتساب أعمال (حتى 3 أرقام)", icon = Icons.Rounded.PhoneAndroid) {
                    Text(
                        text = "أضف حتى 3 أرقام أعمال للمتجر وخدمة العملاء (مثل واتساب أعمال):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = code1,
                            onValueChange = { code1 = it },
                            label = { Text("الرمز") },
                            singleLine = true,
                            modifier = Modifier.width(88.dp)
                        )
                        OutlinedTextField(
                            value = phone1,
                            onValueChange = { phone1 = it.filter { ch -> ch.isDigit() }.take(15) },
                            label = { Text("الرقم 1 (الأساسي)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = code2,
                            onValueChange = { code2 = it },
                            label = { Text("الرمز") },
                            singleLine = true,
                            modifier = Modifier.width(88.dp)
                        )
                        OutlinedTextField(
                            value = phone2,
                            onValueChange = { phone2 = it.filter { ch -> ch.isDigit() }.take(15) },
                            label = { Text("الرقم 2 (المبيعات)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = code3,
                            onValueChange = { code3 = it },
                            label = { Text("الرمز") },
                            singleLine = true,
                            modifier = Modifier.width(88.dp)
                        )
                        OutlinedTextField(
                            value = phone3,
                            onValueChange = { phone3 = it.filter { ch -> ch.isDigit() }.take(15) },
                            label = { Text("الرقم 3 (الدعم والشحن)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = { onSaveThreeNumbers(phone1, code1, phone2, code2, phone3, code3) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ الأرقام الثلاثة")
                    }
                }
            }

            // 5. Owner-Configured Subscription Duration (Months & Days), 4-Digit Code & Hide Owner Login
            item(key = "section_subscription") {
                DashboardCard(title = "مدة الاشتراك والرمز (4 أرقام) وإخفاء بيانات المالك", icon = Icons.Rounded.CreditCard) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = subPriceText,
                            onValueChange = {
                                subPriceText = it.filter { ch -> ch.isDigit() || ch == '.' }
                                draft = draft.copy(
                                    monthlySubscriptionPrice = subPriceText.toDoubleOrNull() ?: 0.0
                                )
                            },
                            label = { Text("قيمة الاشتراك") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = draft.subscriptionCurrency,
                            onValueChange = { draft = draft.copy(subscriptionCurrency = it) },
                            label = { Text("العملة") },
                            singleLine = true,
                            modifier = Modifier.width(110.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = subMonthsText,
                            onValueChange = {
                                subMonthsText = it.filter { ch -> ch.isDigit() }.take(3)
                                draft = draft.copy(
                                    subscriptionMonths = subMonthsText.toIntOrNull()?.coerceIn(0, 1200) ?: 0
                                )
                            },
                            label = { Text("عدد الأشهر") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("owner_sub_months_input")
                        )
                        OutlinedTextField(
                            value = subDaysText,
                            onValueChange = {
                                subDaysText = it.filter { ch -> ch.isDigit() }.take(4)
                                draft = draft.copy(
                                    subscriptionDays = subDaysText.toIntOrNull()?.coerceIn(0, 36500) ?: 0
                                )
                            },
                            label = { Text("عدد الأيام") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("owner_sub_days_input")
                        )
                    }
                    OutlinedTextField(
                        value = draft.subscriptionAccessCode,
                        onValueChange = {
                            draft = draft.copy(subscriptionAccessCode = it.filter { ch -> ch.isDigit() }.take(4))
                        },
                        label = { Text("رمز الاشتراك للعميل (4 أرقام يحددها المالك)") },
                        placeholder = { Text("مثال: 1234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_sub_code_input")
                    )
                    Button(
                        onClick = { onSaveConfig(draft) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_subscription_duration_code_button")
                    ) {
                        Text("حفظ مدة الاشتراك (${draft.subscriptionMonths} شهر و ${draft.subscriptionDays} يوم) والرمز")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إلزام الاشتراك المحدد من المالك", style = MaterialTheme.typography.labelLarge)
                            Text("يعرض للعملاء قيمة الاشتراك ومدته والرمز الذي يحدده المالك فقط", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = draft.subscriptionRequired,
                            onCheckedChange = {
                                draft = draft.copy(subscriptionRequired = it)
                                onSaveConfig(draft)
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (userProfile?.subscriptionActive == true) {
                                "الاشتراك مفعّل (${draft.subscriptionMonths} شهر و ${draft.subscriptionDays} يوم)"
                            } else {
                                "الاشتراك متوقف"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                        Switch(
                            checked = userProfile?.subscriptionActive ?: true,
                            onCheckedChange = onToggleSubscription
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إخفاء بيانات دخول المالك للآخرين", style = MaterialTheme.typography.labelLarge)
                            Text("يخفي رقم الدخول ولوحة المالك عند تحميل شخص آخر للتطبيق", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = draft.hideOwnerLogin,
                            onCheckedChange = {
                                draft = draft.copy(hideOwnerLogin = it)
                                onSaveConfig(draft)
                            }
                        )
                    }

                    OutlinedTextField(
                        value = draft.ownerAccessPin,
                        onValueChange = { draft = draft.copy(ownerAccessPin = it.filter { ch -> ch.isDigit() }.take(4)) },
                        label = { Text("رمز المالك السري لفتح اللوحة (4 أرقام)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_pin_setting_input")
                    )

                    OutlinedButton(
                        onClick = { onToggleRole("customer") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إخفاء لوحة المالك والعودة لواجهة العميل")
                    }
                }
            }

            // 6. Full Social Integrations (Instagram, Facebook, Snapchat) & Empty Business Fields
            item(key = "section_social_fields") {
                DashboardCard(title = "التواصل وحقول الأعمال الفارغة والسيو", icon = Icons.Rounded.Share) {
                    OutlinedTextField(
                        value = draft.instagramHandle,
                        onValueChange = { draft = draft.copy(instagramHandle = it) },
                        label = { Text("حساب إنستقرام بالكامل (Instagram)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.instagramBio,
                        onValueChange = { draft = draft.copy(instagramBio = it) },
                        label = { Text("بايو إنستقرام المتكامل") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.facebookUrl,
                        onValueChange = { draft = draft.copy(facebookUrl = it) },
                        label = { Text("رابط أو حساب فيسبوك (Facebook)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.snapchatHandle,
                        onValueChange = { draft = draft.copy(snapchatHandle = it) },
                        label = { Text("حساب سناب شات (Snapchat)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.seoKeywords,
                        onValueChange = { draft = draft.copy(seoKeywords = it) },
                        label = { Text("كلمات جوجل المفتاحية (SEO)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "خانات فارغة لتعبئة تفاصيل أعمالك:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = draft.customField1Label,
                        onValueChange = { draft = draft.copy(customField1Label = it) },
                        label = { Text("عنوان الخانة 1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.customField1Value,
                        onValueChange = { draft = draft.copy(customField1Value = it) },
                        label = { Text("بيانات الخانة 1 (أدخل تفاصيل عملك)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.customField2Label,
                        onValueChange = { draft = draft.copy(customField2Label = it) },
                        label = { Text("عنوان الخانة 2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.customField2Value,
                        onValueChange = { draft = draft.copy(customField2Value = it) },
                        label = { Text("بيانات الخانة 2 (أدخل ساعات العمل / السياسة)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.customField3Label,
                        onValueChange = { draft = draft.copy(customField3Label = it) },
                        label = { Text("عنوان الخانة 3") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.customField3Value,
                        onValueChange = { draft = draft.copy(customField3Value = it) },
                        label = { Text("بيانات الخانة 3 (أدخل عنوان الفرع / الدفع)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 7. Shipment Tracking Management
            item(key = "section_shipments") {
                DashboardCard(title = "إدارة وتتبع الشحنات", icon = Icons.Rounded.LocalShipping) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newShipCustomer,
                            onValueChange = { newShipCustomer = it },
                            label = { Text("اسم العميل") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newShipCarrier,
                            onValueChange = { newShipCarrier = it },
                            label = { Text("شركة الشحن") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            onCreateShipment("", newShipCustomer, newShipCarrier, "خلال 24 ساعة")
                            newShipCustomer = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إنشاء شحنة تتبع جديدة")
                    }

                    val stageChipLabels = listOf("تأكيد", "تجهيز", "شحن", "في الطريق", "تم التسليم")
                    shipments.forEach { ship ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${ship.trackingCode} • ${ship.customerName}", style = MaterialTheme.typography.labelLarge)
                                        Text("${ship.statusLabel} (${ship.currentStep + 1}/5)", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (ship.currentStep >= 4) {
                                        Text("مكتمل ✓", color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        TextButton(onClick = { onAdvanceShipment(ship) }) {
                                            Text("التالي")
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    stageChipLabels.forEachIndexed { idx, stageLabel ->
                                        FilterChip(
                                            selected = ship.currentStep == idx,
                                            onClick = { onChangeShipmentStatus(ship, idx, "") },
                                            label = { Text(stageLabel, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.testTag("owner_dash_ship_stage_${ship.id}_$idx")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Save All Button
            item(key = "save_all_btn") {
                Button(
                    onClick = { onSaveConfig(draft) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_owner_settings_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ جميع إعدادات المتجر والاسم والثيم", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            content()
        }
    }
}
