package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatBackgroundStyle
import com.example.data.model.DisappearingMode
import com.example.data.model.ShipmentOrder
import com.example.data.model.SoundEffectType
import com.example.data.model.StoreConfig
import com.example.data.model.UserProfile
import com.example.ui.theme.AccentColorOptions
import com.example.ui.theme.VerifiedBlue
import com.example.ui.theme.parseAccentColor

@Composable
fun OwnerDashboardScreen(
    userProfile: UserProfile?,
    storeConfig: StoreConfig,
    shipments: List<ShipmentOrder>,
    onSaveConfig: (StoreConfig) -> Unit,
    onPlaySound: (SoundEffectType) -> Unit,
    onAdvanceShipment: (ShipmentOrder) -> Unit,
    onCreateShipment: (trackingCode: String, customerName: String, carrier: String, etaText: String) -> Unit,
    onToggleRole: (String) -> Unit,
    onToggleSubscription: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember(storeConfig) { mutableStateOf(storeConfig) }
    var subPriceText by remember(storeConfig.monthlySubscriptionPrice) {
        mutableStateOf(storeConfig.monthlySubscriptionPrice.toInt().toString())
    }
    var newShipCustomer by remember { mutableStateOf("") }
    var newShipCarrier by remember { mutableStateOf("سمسا السريع") }

    LaunchedEffect(storeConfig) {
        draft = storeConfig
        subPriceText = storeConfig.monthlySubscriptionPrice.toInt().toString()
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
                        text = "${userProfile?.countryCode.orEmpty()} ${userProfile?.phoneNumber.orEmpty()} • وضع ${if (userProfile?.role == "customer") "العميل" else "المالك"}",
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
                        label = { Text("النص تحت الشارة الزرقاء (مثال: المتجر)") },
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
                        value = draft.tickerText,
                        onValueChange = { draft = draft.copy(tickerText = it) },
                        label = { Text("الشريط العلوي المتحرك في الدردشة") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                                selected = draft.chatBackgroundId == bg.id,
                                onClick = {
                                    draft = draft.copy(chatBackgroundId = bg.id)
                                    onSaveConfig(draft)
                                },
                                label = { Text(bg.arabicName) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 3. Sound Effects & Customer Bell Horn Selector
            item(key = "section_sounds") {
                DashboardCard(title = "جرس التنبيه وصوتيات المحادثة", icon = Icons.Rounded.NotificationsActive) {
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

            // 4. Privacy: Screenshot Blocking, Media Copy Blocking & Disappearing Timer
            item(key = "section_privacy") {
                DashboardCard(title = "الحماية واختفاء الكلام (6س / 24س / غير محدود)", icon = Icons.Rounded.Security) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("حجب تصوير الشاشة (FLAG_SECURE)", style = MaterialTheme.typography.labelLarge)
                            Text("يمنع لقطات الشاشة وتسجيل الفيديو على الجوال", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text("منع نسخ الفيديو والصور", style = MaterialTheme.typography.labelLarge)
                            Text("يقفل حفظ أو نسخ وسائط المتجر والمنتجات", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = draft.blockMediaCopy,
                            onCheckedChange = {
                                draft = draft.copy(blockMediaCopy = it)
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

            // 5. Monthly Subscription Configured by Owner
            item(key = "section_subscription") {
                DashboardCard(title = "الاشتراك الشهري (يحدده المالك)", icon = Icons.Rounded.CreditCard) {
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
                            label = { Text("قيمة الاشتراك الشهري") },
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (userProfile?.subscriptionActive == true) "الاشتراك الشهري مفعّل" else "الاشتراك متوقف",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Switch(
                            checked = userProfile?.subscriptionActive ?: true,
                            onCheckedChange = onToggleSubscription
                        )
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

                    shipments.forEach { ship ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${ship.trackingCode} • ${ship.customerName}", style = MaterialTheme.typography.labelLarge)
                                    Text("${ship.statusLabel} (${ship.currentStep + 1}/5)", style = MaterialTheme.typography.bodySmall)
                                }
                                if (ship.currentStep < 4) {
                                    Button(onClick = { onAdvanceShipment(ship) }) {
                                        Text("تقديم المرحلة")
                                    }
                                } else {
                                    Text("مكتمل ✓", color = MaterialTheme.colorScheme.primary)
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
