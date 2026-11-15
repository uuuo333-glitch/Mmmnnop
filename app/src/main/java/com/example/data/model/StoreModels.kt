package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val phoneNumber: String = "",
    val countryCode: String = "+966",
    val phoneNumber2: String = "",
    val countryCode2: String = "+966",
    val phoneNumber3: String = "",
    val countryCode3: String = "+966",
    val displayName: String = "",
    val role: String = "customer",
    val subscriptionActive: Boolean = true,
    val subscriptionExpiresAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "phoneNumber" to phoneNumber,
        "countryCode" to countryCode,
        "phoneNumber2" to phoneNumber2,
        "countryCode2" to countryCode2,
        "phoneNumber3" to phoneNumber3,
        "countryCode3" to countryCode3,
        "displayName" to displayName,
        "role" to role,
        "subscriptionActive" to subscriptionActive,
        "subscriptionExpiresAtEpochMs" to subscriptionExpiresAtEpochMs,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "phoneNumber" to phoneNumber,
        "countryCode" to countryCode,
        "phoneNumber2" to phoneNumber2,
        "countryCode2" to countryCode2,
        "phoneNumber3" to phoneNumber3,
        "countryCode3" to countryCode3,
        "displayName" to displayName,
        "role" to role,
        "subscriptionActive" to subscriptionActive,
        "subscriptionExpiresAtEpochMs" to subscriptionExpiresAtEpochMs,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class StoreConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val storeName: String = "متجر تيربو الذكي",
    val tagline: String = "قطع غيار وإكسسوارات احترافية بتوصيل فوري",
    val badgeLabel: String = "واتساب الأعمال • المتجر",
    val isVerified: Boolean = true,
    val storeIconKey: String = "storefront",
    val storeLogoFile: String = "",
    val orderButtonText: String = "اطلب الآن",
    val chatInputPlaceholder: String = "اكتب رسالتك للمتجر الذكي...",
    val catalogHeaderTitle: String = "",
    val tickerText: String = "⚡ عروض اليوم: خصم 20% على التيربو والشكمانات الرياضية • شحن سريع خلال 24 ساعة • رد فوري بالذكاء الاصطناعي 24/7",
    val themeMode: String = "dark",
    val accentColorHex: String = "#00C896",
    val chatBackgroundId: String = "neon_speed",
    val activeAlertSound: String = "truck_horn",
    val disappearingMode: String = "24h",
    val blockScreenshots: Boolean = false,
    val blockMediaCopy: Boolean = true,
    val monthlySubscriptionPrice: Double = 49.0,
    val subscriptionCurrency: String = "ر.س",
    val subscriptionMonths: Int = 1,
    val subscriptionDays: Int = 0,
    val subscriptionAccessCode: String = "",
    val subscriptionRequired: Boolean = false,
    val hideOwnerLogin: Boolean = true,
    val ownerAccessPin: String = "",
    val chatOfflineMode: Boolean = false,
    val chatBgMediaType: String = "animated",
    val chatBgMediaUrl: String = "",
    val chatBgAutoRotate: Boolean = false,
    val chatBgRotateIntervalSec: Int = 5,
    val chatBgPlaylist: String = "",
    val lastOwnerAlertSoundId: String = "",
    val lastOwnerAlertSeq: Long = 0L,
    val instagramHandle: String = "@turbo_matjar",
    val instagramBio: String = "المتجر الرسمي المعتمد • شحن لجميع المناطق",
    val facebookUrl: String = "fb.com/turbomatjar",
    val snapchatHandle: String = "turbo.matjar",
    val seoKeywords: String = "متجر سيارات, تيربو رياضي, شكمان قزوز, إضاءات LED, شحن سريع, متجر واتساب",
    val customField1Label: String = "السجل التجاري",
    val customField1Value: String = "",
    val customField2Label: String = "ساعات العمل",
    val customField2Value: String = "",
    val customField3Label: String = "عنوان الفرع / الخدمة",
    val customField3Value: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "storeName" to storeName,
        "tagline" to tagline,
        "badgeLabel" to badgeLabel,
        "isVerified" to isVerified,
        "storeIconKey" to storeIconKey,
        "storeLogoFile" to storeLogoFile,
        "orderButtonText" to orderButtonText,
        "chatInputPlaceholder" to chatInputPlaceholder,
        "catalogHeaderTitle" to catalogHeaderTitle,
        "tickerText" to tickerText,
        "themeMode" to themeMode,
        "accentColorHex" to accentColorHex,
        "chatBackgroundId" to chatBackgroundId,
        "activeAlertSound" to activeAlertSound,
        "disappearingMode" to disappearingMode,
        "blockScreenshots" to blockScreenshots,
        "blockMediaCopy" to blockMediaCopy,
        "monthlySubscriptionPrice" to monthlySubscriptionPrice,
        "subscriptionCurrency" to subscriptionCurrency,
        "subscriptionMonths" to subscriptionMonths,
        "subscriptionDays" to subscriptionDays,
        "subscriptionAccessCode" to subscriptionAccessCode,
        "subscriptionRequired" to subscriptionRequired,
        "hideOwnerLogin" to hideOwnerLogin,
        "ownerAccessPin" to ownerAccessPin,
        "chatOfflineMode" to chatOfflineMode,
        "chatBgMediaType" to chatBgMediaType,
        "chatBgMediaUrl" to chatBgMediaUrl,
        "chatBgAutoRotate" to chatBgAutoRotate,
        "chatBgRotateIntervalSec" to chatBgRotateIntervalSec.coerceIn(5, 300),
        "chatBgPlaylist" to chatBgPlaylist,
        "lastOwnerAlertSoundId" to lastOwnerAlertSoundId,
        "lastOwnerAlertSeq" to lastOwnerAlertSeq,
        "instagramHandle" to instagramHandle,
        "instagramBio" to instagramBio,
        "facebookUrl" to facebookUrl,
        "snapchatHandle" to snapchatHandle,
        "seoKeywords" to seoKeywords,
        "customField1Label" to customField1Label,
        "customField1Value" to customField1Value,
        "customField2Label" to customField2Label,
        "customField2Value" to customField2Value,
        "customField3Label" to customField3Label,
        "customField3Value" to customField3Value,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "storeName" to storeName,
        "tagline" to tagline,
        "badgeLabel" to badgeLabel,
        "isVerified" to isVerified,
        "storeIconKey" to storeIconKey,
        "storeLogoFile" to storeLogoFile,
        "orderButtonText" to orderButtonText,
        "chatInputPlaceholder" to chatInputPlaceholder,
        "catalogHeaderTitle" to catalogHeaderTitle,
        "tickerText" to tickerText,
        "themeMode" to themeMode,
        "accentColorHex" to accentColorHex,
        "chatBackgroundId" to chatBackgroundId,
        "activeAlertSound" to activeAlertSound,
        "disappearingMode" to disappearingMode,
        "blockScreenshots" to blockScreenshots,
        "blockMediaCopy" to blockMediaCopy,
        "monthlySubscriptionPrice" to monthlySubscriptionPrice,
        "subscriptionCurrency" to subscriptionCurrency,
        "subscriptionMonths" to subscriptionMonths,
        "subscriptionDays" to subscriptionDays,
        "subscriptionAccessCode" to subscriptionAccessCode,
        "subscriptionRequired" to subscriptionRequired,
        "hideOwnerLogin" to hideOwnerLogin,
        "ownerAccessPin" to ownerAccessPin,
        "chatOfflineMode" to chatOfflineMode,
        "chatBgMediaType" to chatBgMediaType,
        "chatBgMediaUrl" to chatBgMediaUrl,
        "chatBgAutoRotate" to chatBgAutoRotate,
        "chatBgRotateIntervalSec" to chatBgRotateIntervalSec.coerceIn(5, 300),
        "chatBgPlaylist" to chatBgPlaylist,
        "lastOwnerAlertSoundId" to lastOwnerAlertSoundId,
        "lastOwnerAlertSeq" to lastOwnerAlertSeq,
        "instagramHandle" to instagramHandle,
        "instagramBio" to instagramBio,
        "facebookUrl" to facebookUrl,
        "snapchatHandle" to snapchatHandle,
        "seoKeywords" to seoKeywords,
        "customField1Label" to customField1Label,
        "customField1Value" to customField1Value,
        "customField2Label" to customField2Label,
        "customField2Value" to customField2Value,
        "customField3Label" to customField3Label,
        "customField3Value" to customField3Value,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class StoreProduct(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val currency: String = "ر.س",
    val category: String = "عام",
    val imageResKey: String = "turbo",
    val inStock: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title,
        "description" to description,
        "price" to price,
        "currency" to currency,
        "category" to category,
        "imageResKey" to imageResKey,
        "inStock" to inStock,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "title" to title,
        "description" to description,
        "price" to price,
        "currency" to currency,
        "category" to category,
        "imageResKey" to imageResKey,
        "inStock" to inStock,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val senderType: String = "customer",
    val text: String = "",
    val productId: String = "",
    val shipmentId: String = "",
    val soundEffectId: String = "",
    val mediaType: String = "",
    val mediaLabel: String = "",
    val expiresAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "senderType" to senderType,
            "text" to text,
            "expiresAtEpochMs" to expiresAtEpochMs,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (productId.isNotBlank()) map["productId"] = productId
        if (shipmentId.isNotBlank()) map["shipmentId"] = shipmentId
        if (soundEffectId.isNotBlank()) map["soundEffectId"] = soundEffectId
        if (mediaType.isNotBlank()) map["mediaType"] = mediaType
        if (mediaLabel.isNotBlank()) map["mediaLabel"] = mediaLabel
        return map
    }
}

@IgnoreExtraProperties
data class StoreStatus(
    @DocumentId val id: String = "",
    val userId: String = "",
    val headline: String = "",
    val caption: String = "",
    val seoKeywords: String = "",
    val imagePrompt: String = "",
    val imageResKey: String = "banner",
    val validUntilEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "headline" to headline,
        "caption" to caption,
        "seoKeywords" to seoKeywords,
        "imagePrompt" to imagePrompt,
        "imageResKey" to imageResKey,
        "validUntilEpochMs" to validUntilEpochMs,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ShipmentOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackingCode: String = "",
    val customerName: String = "",
    val carrier: String = "",
    val currentStep: Int = 0,
    val statusLabel: String = "",
    val etaText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "trackingCode" to trackingCode,
        "customerName" to customerName,
        "carrier" to carrier,
        "currentStep" to currentStep,
        "statusLabel" to statusLabel,
        "etaText" to etaText,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "trackingCode" to trackingCode,
        "customerName" to customerName,
        "carrier" to carrier,
        "currentStep" to currentStep,
        "statusLabel" to statusLabel,
        "etaText" to etaText,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

enum class SoundEffectType(
    val id: String,
    val arabicName: String,
    val shortBadge: String
) {
    SPEEDING_CAR("speeding_car", "سيارة مسرعة", "سيارة"),
    TURBO_SPOOL("turbo_spool", "صوت تيربو", "تيربو"),
    ENGINE_REV("engine_rev", "صوت ماكينة", "ماكينة"),
    EXHAUST_POPS("exhaust_pops", "فرقعة شكمان", "شكمان"),
    PASSING_ROCKET("passing_rocket", "صاروخ متعدي", "صاروخ"),
    JET_AIRPLANE("jet_airplane", "صوت طائرة", "طائرة"),
    MOTORCYCLE("motorcycle", "دراجة نارية مسرعة", "دراجة"),
    TRUCK_HORN("truck_horn", "بواري شاحنة مسرعة", "شاحنة"),
    POLICE_SIREN("police_siren", "صوت شرطة", "شرطة"),
    FLY_BUZZ("fly_buzz", "صوت ذبابة", "ذبابة"),
    GLASS_SHATTER("glass_shatter", "صوت قزازة", "قزازة");

    companion object {
        fun fromId(id: String): SoundEffectType =
            entries.firstOrNull { it.id == id } ?: TRUCK_HORN
    }
}

enum class ChatBackgroundStyle(val id: String, val arabicName: String) {
    NEON_SPEED("neon_speed", "خطوط السرعة المتحركة"),
    EMERALD_AURORA("emerald_aurora", "أمواج الزمرد الليلية"),
    CARBON_STARS("carbon_stars", "كربون ونجوم متلألئة"),
    TURBO_PULSE("turbo_pulse", "نبض التيربو الحراري"),
    WHATSAPP_CLASSIC("whatsapp_classic", "نمط واتساب الكلاسيكي");

    companion object {
        fun fromId(id: String): ChatBackgroundStyle =
            entries.firstOrNull { it.id == id } ?: NEON_SPEED
    }
}

enum class DisappearingMode(val id: String, val arabicName: String, val durationMs: Long?) {
    SIX_HOURS("6h", "6 ساعات", 6L * 60L * 60L * 1000L),
    TWENTY_FOUR_HOURS("24h", "24 ساعة", 24L * 60L * 60L * 1000L),
    UNLIMITED("unlimited", "غير محدود", null);

    companion object {
        fun fromId(id: String): DisappearingMode =
            entries.firstOrNull { it.id == id } ?: TWENTY_FOUR_HOURS
    }
}
