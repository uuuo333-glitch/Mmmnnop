package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.StoreConfig
import com.example.data.model.StoreStatus
import com.example.ui.UiState
import com.example.ui.theme.VerifiedBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun scaleDownBitmap(source: Bitmap, maxDimension: Int = 1024): Bitmap {
    val w = source.width
    val h = source.height
    if (w <= maxDimension && h <= maxDimension) return source
    val scale = maxDimension.toFloat() / maxOf(w, h).toFloat()
    val newW = (w * scale).toInt().coerceAtLeast(1)
    val newH = (h * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, newW, newH, true)
}

private suspend fun decodeScaledBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val src = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            raw?.let { scaleDownBitmap(it) }
        } catch (_: Exception) {
            null
        }
    }

@Composable
fun StatusesScreen(
    storeConfig: StoreConfig,
    statuses: List<StoreStatus>,
    aiStatusState: UiState<Unit>,
    generatedPromoBitmap: UiState<Bitmap?>,
    onGenerateStatusAndSeo: (withAiImage: Boolean) -> Unit,
    onGenerateImageOnly: (String) -> Unit,
    onEditImage: (Bitmap, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMs = System.currentTimeMillis()
        }
    }

    var customImagePrompt by remember { mutableStateOf("") }
    var selectedSourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedSourceLabel by remember { mutableStateOf<String?>(null) }
    var promptValidationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = decodeScaledBitmapFromUri(context, uri)
                if (bmp != null) {
                    selectedSourceBitmap = bmp
                    selectedSourceLabel = "صورة من الاستوديو"
                    promptValidationError = null
                } else {
                    promptValidationError = "تعذر قراءة الصورة المختارة من الجهاز"
                }
            }
        }
    }

    val latestStatus = statuses.firstOrNull()
    val remainingSec = if (latestStatus != null) {
        ((latestStatus.validUntilEpochMs - nowMs) / 1000L).coerceAtLeast(0L)
    } else {
        6L * 3600L
    }
    val hrs = remainingSec / 3600
    val mins = (remainingSec % 3600) / 60
    val secs = remainingSec % 60
    val countdownText = String.format("%02d:%02d:%02d", hrs, mins, secs)

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
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الحالات الذكية والسيو (جوجل)",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "تحديث كل 6س: $countdownText",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Text(
                    text = "تغيير تلقائي كل 6 ساعات لجذب العملاء مع كلمات مفتاحية وإنشاء وتعديل الصور بالذكاء الاصطناعي.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // AI Control Card
            item(key = "ai_status_controls") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "محرك الجذب والتصدر في جوجل (AI)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onGenerateStatusAndSeo(false) },
                                enabled = aiStatusState !is UiState.Loading,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_status_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (aiStatusState is UiState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("تحديث الحالة والسيو")
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    val prompt = latestStatus?.imagePrompt
                                        ?: "Sleek modern e-commerce product showcase banner for ${storeConfig.storeName}, studio lighting, no text"
                                    onGenerateImageOnly(prompt)
                                },
                                enabled = generatedPromoBitmap !is UiState.Loading,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_ai_image_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("بنر AI سريع")
                            }
                        }

                        if (aiStatusState is UiState.Error) {
                            Text(
                                text = aiStatusState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Custom Prompt Create & Edit Image Studio (gemini-nano-banana-2.1)
            item(key = "ai_image_studio") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "استوديو إنشاء وتعديل الصور بالذكاء الاصطناعي",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "اكتب وصفاً لإنشاء صورة جديدة من الصفر، أو اختر صورة من جهازك أو منتجات المتجر لتعديلها بأمر نصي.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = customImagePrompt,
                            onValueChange = {
                                customImagePrompt = it
                                promptValidationError = null
                            },
                            label = { Text("اكتب الوصف أو التعديل المطلوب...") },
                            placeholder = { Text("مثال: أضف إضاءة نيون خضراء وخلفية صحراوية ليلاً") },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_image_prompt_input")
                        )

                        Text(
                            text = "اختيار صورة للتعديل (اختياري):",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                label = { Text("من الاستوديو") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.testTag("pick_gallery_image_chip")
                            )
                            val quickAssets = listOf(
                                Triple("تيربو", R.drawable.img_product_turbo_1794675292480, "صورة تيربو المتجر"),
                                Triple("قزوز", R.drawable.img_product_exhaust_1794675292148, "صورة شكمان قزوز"),
                                Triple("شمعات LED", R.drawable.img_product_headlight_1794675293054, "صورة شمعات LED")
                            )
                            quickAssets.forEach { (chipLabel, resId, labelName) ->
                                AssistChip(
                                    onClick = {
                                        scope.launch {
                                            val decoded = withContext(Dispatchers.IO) {
                                                BitmapFactory.decodeResource(context.resources, resId)
                                                    ?.let { scaleDownBitmap(it) }
                                            }
                                            if (decoded != null) {
                                                selectedSourceBitmap = decoded
                                                selectedSourceLabel = labelName
                                                promptValidationError = null
                                            }
                                        }
                                    },
                                    label = { Text(chipLabel) }
                                )
                            }
                            val currentResultBmp = (generatedPromoBitmap as? UiState.Success)?.data
                            if (currentResultBmp != null) {
                                AssistChip(
                                    onClick = {
                                        selectedSourceBitmap = scaleDownBitmap(currentResultBmp)
                                        selectedSourceLabel = "الصورة المولدة الحالية"
                                        promptValidationError = null
                                    },
                                    label = { Text("تعديل النتيجة الحالية") }
                                )
                            }
                        }

                        selectedSourceBitmap?.let { sourceBmp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    bitmap = sourceBmp.asImageBitmap(),
                                    contentDescription = "الصورة المختارة للتعديل",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedSourceLabel ?: "صورة جاهزة للتعديل",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "اكتب التعديل أعلاه واضغط تعديل الصورة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        selectedSourceBitmap = null
                                        selectedSourceLabel = null
                                    }
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "إزالة الصورة المختارة")
                                }
                            }
                        }

                        if (!promptValidationError.isNullOrBlank()) {
                            Text(
                                text = promptValidationError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clean = customImagePrompt.trim()
                                    if (clean.isEmpty()) {
                                        promptValidationError = "اكتب وصفاً للصورة المطلوب إنشاؤها أولاً"
                                    } else {
                                        promptValidationError = null
                                        onGenerateImageOnly(clean)
                                    }
                                },
                                enabled = generatedPromoBitmap !is UiState.Loading,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("create_prompt_image_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("إنشاء صورة")
                            }

                            OutlinedButton(
                                onClick = {
                                    val clean = customImagePrompt.trim()
                                    val bmp = selectedSourceBitmap
                                    when {
                                        bmp == null -> promptValidationError = "اختر صورة من الاستوديو أو المنتجات لتعديلها"
                                        clean.isEmpty() -> promptValidationError = "اكتب وصف التعديل المطلوب على الصورة"
                                        else -> {
                                            promptValidationError = null
                                            onEditImage(bmp, clean)
                                        }
                                    }
                                },
                                enabled = generatedPromoBitmap !is UiState.Loading,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_prompt_image_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("تعديل الصورة")
                            }
                        }
                    }
                }
            }

            // AI Generated / Edited Image Preview Card (gemini-nano-banana-2.1)
            if (generatedPromoBitmap !is UiState.Idle) {
                item(key = "ai_image_preview") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "نتيجة الاستوديو بالذكاء الاصطناعي (gemini-nano-banana-2.1)",
                                style = MaterialTheme.typography.labelLarge
                            )
                            when (generatedPromoBitmap) {
                                is UiState.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(170.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator()
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("جاري إنشاء/تعديل الصورة عبر الذكاء الاصطناعي...")
                                        }
                                    }
                                }
                                is UiState.Success -> {
                                    generatedPromoBitmap.data?.let { bmp ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(210.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                        ) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "صورة بالذكاء الاصطناعي",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.matchParentSize()
                                            )
                                            if (storeConfig.blockMediaCopy) {
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.6f),
                                                    shape = RoundedCornerShape(50),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                        Text("محمي", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                selectedSourceBitmap = scaleDownBitmap(bmp)
                                                selectedSourceLabel = "الصورة المولدة الحالية"
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تعديل هذه الصورة بأمر نصي جديد")
                                        }
                                    }
                                }
                                is UiState.Error -> {
                                    Text(
                                        text = generatedPromoBitmap.message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                UiState.Idle -> {}
                            }
                        }
                    }
                }
            }

            // Google Search SEO Preview Card
            item(key = "google_seo_card") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = VerifiedBlue)
                            Text(
                                text = "معاينة الظهور في بحث جوجل (SEO)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Rounded.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "https://matjar.app/${storeConfig.instagramHandle.removePrefix("@")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${storeConfig.storeName} | ${storeConfig.badgeLabel} المعتمد",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = VerifiedBlue
                                )
                                Text(
                                    text = latestStatus?.caption ?: storeConfig.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "كلمات مفتاحية: ${latestStatus?.seoKeywords?.ifBlank { storeConfig.seoKeywords } ?: storeConfig.seoKeywords}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Statuses History Feed
            items(statuses, key = { it.id }) { status ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_store_banner_1794675291139),
                                contentDescription = status.headline,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "حالة كل 6 ساعات",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = status.headline,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = status.caption,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "SEO: ${status.seoKeywords}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
