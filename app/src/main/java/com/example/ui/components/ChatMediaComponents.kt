package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.VideoView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.model.ChatBackgroundStyle
import com.example.data.model.StoreConfig
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class BgPlaylistEntry(val type: String, val ref: String)

fun parseBgPlaylist(raw: String): List<BgPlaylistEntry> =
    raw.split("|")
        .mapNotNull { token ->
            val idx = token.indexOf(':')
            if (idx <= 0 || idx >= token.lastIndex) null
            else BgPlaylistEntry(
                type = token.substring(0, idx).trim(),
                ref = token.substring(idx + 1).trim()
            )
        }

fun serializeBgPlaylist(items: List<BgPlaylistEntry>): String =
    items.joinToString("|") { item ->
        val safeRef = item.ref.replace("|", "")
        "${item.type}:$safeRef"
    }

fun extractYouTubeVideoId(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
        return trimmed
    }
    val patterns = listOf(
        Regex("(?:youtube\\.com/watch\\?(?:.*&)?v=)([a-zA-Z0-9_-]{11})"),
        Regex("(?:youtu\\.be/)([a-zA-Z0-9_-]{11})"),
        Regex("(?:youtube\\.com/shorts/)([a-zA-Z0-9_-]{11})"),
        Regex("(?:youtube\\.com/embed/)([a-zA-Z0-9_-]{11})")
    )
    for (pattern in patterns) {
        val match = pattern.find(trimmed)
        if (match != null) return match.groupValues[1]
    }
    return null
}

suspend fun copyUriToAppMediaFile(
    context: Context,
    sourceUri: Uri,
    targetFileName: String
): Result<String> = withContext(Dispatchers.IO) {
    try {
        val destFile = File(context.filesDir, targetFileName)
        val input = context.contentResolver.openInputStream(sourceUri)
            ?: return@withContext Result.failure(IllegalStateException("تعذر فتح الملف المختار"))
        input.use { inStream ->
            destFile.outputStream().use { outStream ->
                inStream.copyTo(outStream)
            }
        }
        Result.success(targetFileName)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

@Composable
fun AnimatedMotionPhotoBackdrop(
    file: File?,
    isLightTheme: Boolean,
    fallbackStyle: ChatBackgroundStyle,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (file != null && file.exists()) {
        val motionTransition = rememberInfiniteTransition(label = "bg_photo_motion")
        val zoom by motionTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.13f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bg_photo_zoom"
        )
        val panX by motionTransition.animateFloat(
            initialValue = -14f,
            targetValue = 14f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bg_photo_panx"
        )
        Box(modifier = modifier.fillMaxSize()) {
            AsyncImage(
                model = file,
                contentDescription = "خلفية صورة متحركة للمحادثة",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = panX
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isLightTheme) Color.White.copy(alpha = 0.56f)
                        else Color.Black.copy(alpha = 0.52f)
                    )
            )
        }
    } else {
        AnimatedChatBackground(
            style = fallbackStyle,
            accentColor = accentColor,
            isLightTheme = isLightTheme,
            modifier = modifier
        )
    }
}

@Composable
fun ChatDynamicBackdrop(
    storeConfig: StoreConfig,
    accentColor: Color,
    isLightTheme: Boolean,
    onBlockedMediaAttempt: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bgStyle = ChatBackgroundStyle.fromId(storeConfig.chatBackgroundId)
    val mediaType = storeConfig.chatBgMediaType
    val mediaUrl = storeConfig.chatBgMediaUrl
    val parsedPlaylist = remember(storeConfig.chatBgPlaylist) {
        parseBgPlaylist(storeConfig.chatBgPlaylist)
    }
    val visualPlaylist = remember(parsedPlaylist) {
        parsedPlaylist.filter { it.type != "youtube" }
    }

    val effectiveSlides = remember(
        storeConfig.chatBgAutoRotate,
        mediaType,
        mediaUrl,
        storeConfig.chatBackgroundId,
        visualPlaylist
    ) {
        when {
            visualPlaylist.isNotEmpty() -> {
                if (storeConfig.chatBgAutoRotate && visualPlaylist.size == 1) {
                    visualPlaylist + ChatBackgroundStyle.entries.take(2).map {
                        BgPlaylistEntry("animated", it.id)
                    }
                } else {
                    visualPlaylist
                }
            }
            storeConfig.chatBgAutoRotate -> {
                val base = mutableListOf<BgPlaylistEntry>()
                if ((mediaType == "photo" || mediaType == "video") && mediaUrl.isNotBlank()) {
                    base.add(BgPlaylistEntry(mediaType, mediaUrl))
                }
                ChatBackgroundStyle.entries.forEach { st ->
                    base.add(BgPlaylistEntry("animated", st.id))
                }
                base
            }
            else -> emptyList()
        }
    }

    var currentSlideIndex by remember(effectiveSlides, storeConfig.chatBgAutoRotate) {
        mutableIntStateOf(0)
    }
    val safeIntervalSec = storeConfig.chatBgRotateIntervalSec.coerceAtLeast(5)

    LaunchedEffect(storeConfig.chatBgAutoRotate, safeIntervalSec, effectiveSlides) {
        if (storeConfig.chatBgAutoRotate && effectiveSlides.size > 1) {
            while (true) {
                delay(safeIntervalSec * 1000L)
                currentSlideIndex = (currentSlideIndex + 1) % effectiveSlides.size
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(storeConfig.blockMediaCopy) {
                if (storeConfig.blockMediaCopy) {
                    detectTapGestures(onLongPress = { onBlockedMediaAttempt() })
                }
            }
    ) {
        if (storeConfig.chatBgAutoRotate && effectiveSlides.isNotEmpty()) {
            val activeSlide = effectiveSlides[currentSlideIndex % effectiveSlides.size]
            Crossfade(
                targetState = activeSlide,
                animationSpec = tween(durationMillis = 850),
                label = "chat_bg_rotate_xfade"
            ) { slide ->
                when (slide.type) {
                    "photo", "ai" -> {
                        val file = remember(slide.ref) {
                            if (slide.ref.isNotBlank()) File(context.filesDir, slide.ref) else null
                        }
                        AnimatedMotionPhotoBackdrop(
                            file = file,
                            isLightTheme = isLightTheme,
                            fallbackStyle = bgStyle,
                            accentColor = accentColor
                        )
                    }
                    "video" -> {
                        val videoFile = remember(slide.ref) {
                            if (slide.ref.isNotBlank()) File(context.filesDir, slide.ref) else null
                        }
                        if (videoFile != null && videoFile.exists()) {
                            DeviceVideoLoopLayer(
                                file = videoFile,
                                muted = true,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isLightTheme) Color.White.copy(alpha = 0.45f)
                                        else Color.Black.copy(alpha = 0.48f)
                                    )
                            )
                        } else {
                            AnimatedChatBackground(style = bgStyle, accentColor = accentColor, isLightTheme = isLightTheme)
                        }
                    }
                    else -> {
                        AnimatedChatBackground(
                            style = ChatBackgroundStyle.fromId(slide.ref),
                            accentColor = accentColor,
                            isLightTheme = isLightTheme
                        )
                    }
                }
            }
        } else {
            when (mediaType) {
                "playlist" -> {
                    val firstSlide = visualPlaylist.firstOrNull()
                    if (firstSlide != null && (firstSlide.type == "photo" || firstSlide.type == "ai")) {
                        AnimatedMotionPhotoBackdrop(
                            file = File(context.filesDir, firstSlide.ref),
                            isLightTheme = isLightTheme,
                            fallbackStyle = bgStyle,
                            accentColor = accentColor
                        )
                    } else if (firstSlide != null && firstSlide.type == "video") {
                        val vf = File(context.filesDir, firstSlide.ref)
                        if (vf.exists()) {
                            DeviceVideoLoopLayer(file = vf, muted = true, modifier = Modifier.fillMaxSize())
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isLightTheme) Color.White.copy(alpha = 0.45f)
                                        else Color.Black.copy(alpha = 0.48f)
                                    )
                            )
                        } else {
                            AnimatedChatBackground(style = bgStyle, accentColor = accentColor, isLightTheme = isLightTheme)
                        }
                    } else {
                        AnimatedChatBackground(
                            style = firstSlide?.let { ChatBackgroundStyle.fromId(it.ref) } ?: bgStyle,
                            accentColor = accentColor,
                            isLightTheme = isLightTheme
                        )
                    }
                }
                "photo" -> {
                    val photoFile = remember(mediaUrl) {
                        if (mediaUrl.isNotBlank()) File(context.filesDir, mediaUrl) else null
                    }
                    AnimatedMotionPhotoBackdrop(
                        file = photoFile,
                        isLightTheme = isLightTheme,
                        fallbackStyle = bgStyle,
                        accentColor = accentColor
                    )
                }
                "video" -> {
                    val videoFile = remember(mediaUrl) {
                        if (mediaUrl.isNotBlank()) File(context.filesDir, mediaUrl) else null
                    }
                    if (videoFile != null && videoFile.exists()) {
                        DeviceVideoLoopLayer(
                            file = videoFile,
                            muted = true,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isLightTheme) Color.White.copy(alpha = 0.45f)
                                    else Color.Black.copy(alpha = 0.48f)
                                )
                        )
                    } else {
                        AnimatedChatBackground(style = bgStyle, accentColor = accentColor, isLightTheme = isLightTheme)
                    }
                }
                else -> {
                    AnimatedChatBackground(
                        style = bgStyle,
                        accentColor = accentColor,
                        isLightTheme = isLightTheme
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceVideoLoopLayer(
    file: File,
    muted: Boolean = false,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setVideoPath(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    if (muted) mp.setVolume(0f, 0f)
                    start()
                }
            }
        },
        update = { view ->
            if (!view.isPlaying) {
                view.start()
            }
        },
        modifier = modifier
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeChatVideoCard(
    youtubeUrlOrId: String,
    copyBlocked: Boolean,
    onBlockedMediaAttempt: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val videoId = remember(youtubeUrlOrId) { extractYouTubeVideoId(youtubeUrlOrId) }
    if (videoId == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .pointerInput(copyBlocked) {
                    if (copyBlocked) {
                        detectTapGestures(onLongPress = { onBlockedMediaAttempt() })
                    }
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        val embedHtml = """
                            <!DOCTYPE html>
                            <html>
                              <body style="margin:0;padding:0;background:#000;">
                                <iframe
                                  width="100%"
                                  height="100%"
                                  style="position:fixed;inset:0;width:100%;height:100%;border:0;"
                                  src="https://www.youtube.com/embed/$videoId?playsinline=1&rel=0&modestbranding=1"
                                  allow="autoplay; encrypted-media; picture-in-picture"
                                  allowfullscreen>
                                </iframe>
                              </body>
                            </html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "utf-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (copyBlocked) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Text("فيديو محمي", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            Text(
                text = "فيديو يوتيوب مضاف من المالك",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun DeviceChatVideoCard(
    fileName: String,
    copyBlocked: Boolean,
    onBlockedMediaAttempt: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember(fileName) { File(context.filesDir, fileName) }
    if (!file.exists()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .pointerInput(copyBlocked) {
                if (copyBlocked) {
                    detectTapGestures(onLongPress = { onBlockedMediaAttempt() })
                }
            }
    ) {
        DeviceVideoLoopLayer(file = file, muted = false, modifier = Modifier.fillMaxSize())
        if (copyBlocked) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Text("محمي ضد الحفظ", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }
}
