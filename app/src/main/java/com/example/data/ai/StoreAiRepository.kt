package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.ShipmentOrder
import com.example.data.model.SoundEffectType
import com.example.data.model.StoreConfig
import com.example.data.model.StoreProduct
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiChatReplyResult(
    val replyText: String,
    val recommendedProductId: String,
    val relatedShipmentId: String,
    val suggestedSoundId: String
)

data class AiStatusResult(
    val headline: String,
    val caption: String,
    val seoKeywords: String,
    val imagePrompt: String
)

class StoreAiRepository {

    private val chatReplySchema = Schema.obj(
        properties = mapOf(
            "replyText" to Schema.string(description = "Friendly persuasive Arabic store reply answering the customer."),
            "recommendedProductId" to Schema.string(description = "Matching product ID from the catalog or empty string."),
            "relatedShipmentId" to Schema.string(description = "Matching shipment ID if asking about order/tracking or empty string."),
            "suggestedSoundId" to Schema.enumeration(
                values = listOf("") + SoundEffectType.entries.map { it.id },
                description = "Optional sound effect ID to trigger if relevant, or empty string."
            )
        )
    )

    private val statusSchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string(description = "Catchy Arabic 6-hour store status headline under 70 chars."),
            "caption" to Schema.string(description = "Engaging Arabic promotional caption to attract customers."),
            "seoKeywords" to Schema.string(description = "Comma-separated Google SEO keywords in Arabic."),
            "imagePrompt" to Schema.string(description = "Detailed English prompt for a promotional product banner photo.")
        )
    )

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = chatReplySchema
            }
        )
    }

    private val statusModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = statusSchema
            }
        )
    }

    private val bgScenesSchema = Schema.obj(
        properties = mapOf(
            "prompts" to Schema.array(
                items = Schema.string(description = "Visual scene prompt in English for a dark moody chat wallpaper frame, rich contrast suitable behind chat bubbles, no text or watermarks."),
                description = "3 cohesive evolving scene prompts forming a dynamic rotating chat backdrop sequence."
            )
        )
    )

    private val bgScenesModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = bgScenesSchema
            }
        )
    }

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    suspend fun generateCustomerReply(
        customerMessage: String,
        storeConfig: StoreConfig,
        products: List<StoreProduct>,
        shipments: List<ShipmentOrder>
    ): Result<AiChatReplyResult> {
        return try {
            val catalogSummary = products.joinToString("\n") {
                "- id=${it.id} | ${it.title} | ${it.price} ${it.currency} | ${it.category} | ${it.description}"
            }
            val shipmentSummary = shipments.joinToString("\n") {
                "- id=${it.id} | code=${it.trackingCode} | ${it.carrier} | ${it.statusLabel} | ETA=${it.etaText}"
            }
            val customInfo = buildString {
                if (storeConfig.customField1Value.isNotBlank()) append("${storeConfig.customField1Label}: ${storeConfig.customField1Value}\n")
                if (storeConfig.customField2Value.isNotBlank()) append("${storeConfig.customField2Label}: ${storeConfig.customField2Value}\n")
                if (storeConfig.customField3Value.isNotBlank()) append("${storeConfig.customField3Label}: ${storeConfig.customField3Value}\n")
            }

            val prompt = """
                أنت مساعد المتجر الذكي لمتجر "${storeConfig.storeName}" (${storeConfig.tagline}).
                ترد على العملاء باللغة العربية بأسلوب ودود وسريع واحترافي داخل محادثة شبيهة بالواتساب.
                حسابات المتجر: إنستقرام ${storeConfig.instagramHandle}، فيسبوك ${storeConfig.facebookUrl}، سناب ${storeConfig.snapchatHandle}.
                $customInfo
                المنتجات المتوفرة:
                $catalogSummary
                الشحنات الحالية:
                $shipmentSummary

                رسالة العميل: "$customerMessage"

                أعد الإجابة بصيغة JSON مطابقة للـ schema:
                - إذا سأل العميل عن منتج أو سعر أو طلب، رشّح أنسب منتج وضع معرفه في recommendedProductId.
                - إذا سأل عن الشحن أو التتبع، ضع معرف الشحنة في relatedShipmentId.
                - إذا طلب تجربة صوت أو ذكر تيربو/قزوز/شرطة/سيارة/دراجة/شاحنة، ضع الصوت المناسب في suggestedSoundId.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                chatModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty AI response")
            val json = JSONObject(jsonText)
            Result.success(
                AiChatReplyResult(
                    replyText = json.optString("replyText", "أهلاً بك! يسعدنا خدمتك في ${storeConfig.storeName}."),
                    recommendedProductId = json.optString("recommendedProductId", ""),
                    relatedShipmentId = json.optString("relatedShipmentId", ""),
                    suggestedSoundId = json.optString("suggestedSoundId", "")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI reply timed out", e)
            Result.failure(Exception("انتهت مهلة الرد الذكي (60 ثانية). حاول مجدداً."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI reply failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateSixHourStatus(
        storeConfig: StoreConfig,
        products: List<StoreProduct>
    ): Result<AiStatusResult> {
        return try {
            val productTitles = products.joinToString("، ") { "${it.title} (${it.price} ${it.currency})" }
            val prompt = """
                أنشئ حالة تسويقية جذابة تتجدد كل 6 ساعات لمتجر "${storeConfig.storeName}" لجذب العملاء على واتساب وإنستقرام وتصدر نتائج بحث جوجل.
                المنتجات الحالية: $productTitles
                الكلمات المفتاحية الأساسية: ${storeConfig.seoKeywords}
                أعد النتيجة باللغة العربية لـ headline و caption و seoKeywords، وبوصف بصري إنجليزي احترافي لـ imagePrompt.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                statusModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty status AI response")
            val json = JSONObject(jsonText)
            Result.success(
                AiStatusResult(
                    headline = json.optString("headline", "عروض الـ 6 ساعات من ${storeConfig.storeName}"),
                    caption = json.optString("caption", "اكتشف أحدث المنتجات الآن مع الشحن السريع وتتبع الطلب لحظة بلحظة."),
                    seoKeywords = json.optString("seoKeywords", storeConfig.seoKeywords),
                    imagePrompt = json.optString(
                        "imagePrompt",
                        "Luxury modern e-commerce automotive performance showroom banner with glowing emerald lights, no text"
                    )
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI status generation timed out", e)
            Result.failure(Exception("انتهت مهلة توليد الحالة الذكية."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI status generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generatePromoImage(promptText: String): Result<Bitmap> {
        return try {
            val response = withTimeout(60_000L) {
                imageModel.generateContent(promptText)
            }
            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(Exception("لم يُرجع نموذج الصور أي صورة."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            Result.failure(Exception("انتهت مهلة توليد الصورة (60 ثانية)."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun editPromoImage(inputBitmap: Bitmap, editInstruction: String): Result<Bitmap> {
        return try {
            val response = withTimeout(60_000L) {
                imageModel.generateContent(
                    content {
                        image(inputBitmap)
                        text(editInstruction)
                    }
                )
            }
            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(Exception("لم يُرجع نموذج تعديل الصور أي صورة."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image editing timed out", e)
            Result.failure(Exception("انتهت مهلة تعديل الصورة (60 ثانية)."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image editing failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateAnimatedBgScenes(
        themePrompt: String,
        storeConfig: StoreConfig,
        frameCount: Int = 3
    ): Result<List<Bitmap>> {
        return try {
            val safeCount = frameCount.coerceIn(2, 4)
            val cleanHint = themePrompt.trim().ifBlank {
                "Luxury dark automotive turbo neon motion night atmosphere for ${storeConfig.storeName}"
            }
            val plannerPrompt = """
                Create $safeCount distinct yet cohesive visual scene prompts in English for rotating animated-style chat wallpaper backgrounds for the store "${storeConfig.storeName}".
                User theme request: "$cleanHint"
                Each prompt must describe atmospheric lighting, cinematic depth, deep contrast suitable behind messaging bubbles, and NO text, NO logos, NO watermarks.
                Make each scene feel like a smooth progression (e.g., frame 1 wide cinematic neon angle, frame 2 glowing speed light trails, frame 3 close-up reflections & aurora glow).
            """.trimIndent()

            val scenePlanResp = withTimeout(60_000L) {
                bgScenesModel.generateContent(plannerPrompt)
            }
            val scenePrompts = mutableListOf<String>()
            val jsonObj = scenePlanResp.text?.let { runCatching { JSONObject(it) }.getOrNull() }
            val arr = jsonObj?.optJSONArray("prompts")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.optString(i).trim()
                    if (item.isNotEmpty()) scenePrompts.add(item)
                }
            }
            while (scenePrompts.size < safeCount) {
                val idx = scenePrompts.size + 1
                scenePrompts.add("Cinematic dark atmospheric wallpaper frame #$idx for $cleanHint, glowing neon highlights, sleek motion blur, no text")
            }

            val bitmaps = mutableListOf<Bitmap>()
            for (scenePrompt in scenePrompts.take(safeCount)) {
                val bmpResp = withTimeout(60_000L) {
                    imageModel.generateContent(scenePrompt)
                }
                val bmp = bmpResp.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()?.image
                if (bmp != null) {
                    bitmaps.add(bmp)
                }
            }
            if (bitmaps.isNotEmpty()) {
                Result.success(bitmaps)
            } else {
                Result.failure(Exception("تعذر توليد صور الخلفية من نموذج الذكاء الاصطناعي."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Animated bg generation timed out", e)
            Result.failure(Exception("انتهت مهلة توليد خلفيات المحادثة بالذكاء الاصطناعي."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Animated bg generation failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "StoreAiRepository"
    }
}
