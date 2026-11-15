package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.audio.SoundEffectEngine
import com.example.data.ai.StoreAiRepository
import com.example.data.model.ChatMessage
import com.example.data.model.DisappearingMode
import com.example.data.model.ShipmentOrder
import com.example.data.model.SoundEffectType
import com.example.data.model.StoreConfig
import com.example.data.model.StoreProduct
import com.example.data.model.StoreStatus
import com.example.data.model.UserProfile
import com.example.data.repository.StoreRepository
import com.example.ui.components.BgPlaylistEntry
import com.example.ui.components.parseBgPlaylist
import com.example.ui.components.serializeBgPlaylist
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MatjarViewModel(
    private val repository: StoreRepository,
    private val aiRepository: StoreAiRepository,
    private val soundEngine: SoundEffectEngine,
    private val currentUserId: String
) : ViewModel() {

    private val _clockNowMs = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            while (true) {
                delay(15_000L)
                _clockNowMs.value = System.currentTimeMillis()
            }
        }
    }

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { err ->
            val root = unwrapFirestoreError(err) ?: err
            if (root is CancellationException) throw root
            Log.w(TAG, "Error observing profile", root)
            emit(UiState.Error(root.message ?: "تعذر تحميل الملف الشخصي"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val storeConfig: StateFlow<StoreConfig> = repository.observeStoreConfig(currentUserId)
        .map { it ?: StoreConfig(userId = currentUserId) }
        .catch { err ->
            val root = unwrapFirestoreError(err) ?: err
            if (root is CancellationException) throw root
            Log.w(TAG, "Error observing storeConfig", root)
            emit(StoreConfig(userId = currentUserId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StoreConfig(userId = currentUserId))

    val products: StateFlow<List<StoreProduct>> = repository.observeProducts(currentUserId)
        .catch { err ->
            val root = unwrapFirestoreError(err) ?: err
            if (root is CancellationException) throw root
            Log.w(TAG, "Error observing products", root)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val messages: StateFlow<List<ChatMessage>> = combine(
        repository.observeMessages(currentUserId),
        _clockNowMs
    ) { list, now ->
        list.filter { msg -> msg.expiresAtEpochMs == 0L || msg.expiresAtEpochMs > now }
    }
        .catch { err ->
            val root = unwrapFirestoreError(err) ?: err
            if (root is CancellationException) throw root
            Log.w(TAG, "Error observing messages", root)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val statuses: StateFlow<List<StoreStatus>> = repository.observeStatuses(currentUserId)
        .catch { err ->
            val root = unwrapFirestoreError(err) ?: err
            if (root is CancellationException) throw root
            Log.w(TAG, "Error observing statuses", root)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val shipments: StateFlow<List<ShipmentOrder>> = repository.observeShipments(currentUserId)
        .catch { err ->
            val root = unwrapFirestoreError(err) ?: err
            if (root is CancellationException) throw root
            Log.w(TAG, "Error observing shipments", root)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _aiReplyState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val aiReplyState: StateFlow<UiState<Unit>> = _aiReplyState.asStateFlow()

    private val _aiStatusState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val aiStatusState: StateFlow<UiState<Unit>> = _aiStatusState.asStateFlow()

    private val _generatedPromoBitmap = MutableStateFlow<UiState<Bitmap?>>(UiState.Idle)
    val generatedPromoBitmap: StateFlow<UiState<Bitmap?>> = _generatedPromoBitmap.asStateFlow()

    private val _aiBgGenState = MutableStateFlow<UiState<Int>>(UiState.Idle)
    val aiBgGenState: StateFlow<UiState<Int>> = _aiBgGenState.asStateFlow()

    private val _bellPulseCount = MutableStateFlow(0)
    val bellPulseCount: StateFlow<Int> = _bellPulseCount.asStateFlow()

    private val _toastBanner = MutableStateFlow<String?>(null)
    val toastBanner: StateFlow<String?> = _toastBanner.asStateFlow()

    fun clearToastBanner() {
        _toastBanner.value = null
    }

    fun registerPhoneAndProfile(
        phoneNumber: String,
        countryCode: String,
        displayName: String,
        role: String = "customer",
        phoneNumber2: String = "",
        countryCode2: String = "+966",
        phoneNumber3: String = "",
        countryCode3: String = "+966"
    ) {
        viewModelScope.launch {
            val safeRole = if (role == "owner") "owner" else "customer"
            val profile = UserProfile(
                userId = currentUserId,
                phoneNumber = phoneNumber.trim(),
                countryCode = countryCode.trim(),
                phoneNumber2 = phoneNumber2.trim(),
                countryCode2 = countryCode2.trim(),
                phoneNumber3 = phoneNumber3.trim(),
                countryCode3 = countryCode3.trim(),
                displayName = displayName.trim(),
                role = safeRole,
                subscriptionActive = true
            )
            val result = repository.saveUserProfile(currentUserId, profile)
            if (result.isSuccess) {
                repository.seedInitialStoreIfEmpty(currentUserId, displayName.trim())
                _toastBanner.value = "تم تسجيل الرقم وربط المتجر بنجاح"
            } else {
                _toastBanner.value = result.exceptionOrNull()?.localizedMessage ?: "تعذر حفظ الحساب"
            }
        }
    }

    fun updateBusinessPhones(
        phoneNumber1: String,
        countryCode1: String,
        phoneNumber2: String,
        countryCode2: String,
        phoneNumber3: String,
        countryCode3: String
    ) {
        val current = (userProfile.value as? UiState.Success)?.data ?: return
        val p1 = phoneNumber1.filter { it.isDigit() }.trimStart('0')
        if (p1.length < 6) {
            _toastBanner.value = "يجب أن يكون الرقم الأساسي 6 أرقام على الأقل"
            return
        }
        viewModelScope.launch {
            val updated = current.copy(
                phoneNumber = p1,
                countryCode = countryCode1.ifBlank { "+966" },
                phoneNumber2 = phoneNumber2.filter { it.isDigit() }.trimStart('0'),
                countryCode2 = countryCode2.ifBlank { "+966" },
                phoneNumber3 = phoneNumber3.filter { it.isDigit() }.trimStart('0'),
                countryCode3 = countryCode3.ifBlank { "+966" }
            )
            val res = repository.saveUserProfile(currentUserId, updated)
            _toastBanner.value = if (res.isSuccess) {
                "تم حفظ أرقام واتساب أعمال (حتى 3 أرقام) بنجاح"
            } else {
                "تعذر تحديث الأرقام"
            }
        }
    }

    fun unlockOwnerModeWithPin(enteredPin: String, onResult: (Boolean, String?) -> Unit) {
        val cleanPin = enteredPin.trim()
        val currentProfile = (userProfile.value as? UiState.Success)?.data
        if (currentProfile == null) {
            onResult(false, "يرجى إكمال التسجيل أولاً")
            return
        }
        if (cleanPin.length != 4 || !cleanPin.all { it.isDigit() }) {
            onResult(false, "يرجى إدخال رمز مكون من 4 أرقام")
            return
        }
        val configuredPin = storeConfig.value.ownerAccessPin.trim()
        if (configuredPin.isBlank()) {
            viewModelScope.launch {
                repository.saveStoreConfig(
                    currentUserId,
                    storeConfig.value.copy(ownerAccessPin = cleanPin, hideOwnerLogin = true)
                )
                repository.saveUserProfile(currentUserId, currentProfile.copy(role = "owner"))
                _toastBanner.value = "تم تعيين رمز المالك السري (4 أرقام) وتفعيل لوحة المالك"
                onResult(true, null)
            }
        } else if (cleanPin == configuredPin) {
            viewModelScope.launch {
                repository.saveUserProfile(currentUserId, currentProfile.copy(role = "owner"))
                _toastBanner.value = "تم فتح لوحة المالك بنجاح"
                onResult(true, null)
            }
        } else {
            onResult(false, "رمز المالك السري المكون من 4 أرقام غير صحيح")
        }
    }

    fun toggleUserRole(newRole: String) {
        val current = (userProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.saveUserProfile(currentUserId, current.copy(role = newRole))
            if (newRole == "customer") {
                _toastBanner.value = "تم إخفاء لوحة وبيانات المالك والتبديل لوضع العميل"
            }
        }
    }

    fun toggleSubscriptionActive(active: Boolean) {
        val current = (userProfile.value as? UiState.Success)?.data ?: return
        val months = storeConfig.value.subscriptionMonths.coerceAtLeast(0)
        val days = storeConfig.value.subscriptionDays.coerceAtLeast(0)
        val expiryMs = if (active) computeSubscriptionExpiryMillis(months, days) else 0L
        viewModelScope.launch {
            repository.saveUserProfile(
                currentUserId,
                current.copy(subscriptionActive = active, subscriptionExpiresAtEpochMs = expiryMs)
            )
            _toastBanner.value = if (active) {
                "تم تفعيل الاشتراك لمدة $months شهر و $days يوم"
            } else {
                "تم إيقاف الاشتراك مؤقتاً"
            }
        }
    }

    fun redeemSubscriptionCode(enteredCode: String, onResult: (Boolean, String?) -> Unit) {
        val current = (userProfile.value as? UiState.Success)?.data
        if (current == null) {
            onResult(false, "يرجى إكمال التسجيل أولاً")
            return
        }
        val expected = storeConfig.value.subscriptionAccessCode.trim()
        val candidate = enteredCode.trim()
        if (expected.isBlank()) {
            onResult(false, "لم يحدد المالك رمز الاشتراك (4 أرقام) بعد")
            return
        }
        if (candidate.length != 4 || !candidate.all { it.isDigit() }) {
            onResult(false, "يرجى إدخال رمز اشتراك مكون من 4 أرقام")
            return
        }
        if (candidate != expected) {
            onResult(false, "رمز الاشتراك غير صحيح، تأكد من الرمز المكون من 4 أرقام المحدد من المالك")
            return
        }
        val months = storeConfig.value.subscriptionMonths.coerceAtLeast(0)
        val days = storeConfig.value.subscriptionDays.coerceAtLeast(0)
        val expiryMs = computeSubscriptionExpiryMillis(months, days)
        viewModelScope.launch {
            val res = repository.saveUserProfile(
                currentUserId,
                current.copy(subscriptionActive = true, subscriptionExpiresAtEpochMs = expiryMs)
            )
            if (res.isSuccess) {
                _toastBanner.value = "تم تفعيل اشتراكك بنجاح لمدة $months شهر و $days يوم"
                onResult(true, null)
            } else {
                onResult(false, "تعذر تفعيل الاشتراك")
            }
        }
    }

    private fun computeSubscriptionExpiryMillis(months: Int, days: Int): Long {
        val safeMonths = if (months == 0 && days == 0) 1 else months
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MONTH, safeMonths)
        cal.add(java.util.Calendar.DAY_OF_MONTH, days)
        return cal.timeInMillis
    }

    fun triggerBellHorn() {
        val sound = SoundEffectType.fromId(storeConfig.value.activeAlertSound)
        soundEngine.play(sound)
        _bellPulseCount.value += 1
        _toastBanner.value = "تنبيه صوتي: ${sound.arabicName}"
    }

    fun playSoundEffect(effect: SoundEffectType) {
        soundEngine.play(effect)
        _toastBanner.value = "تشغيل صوت: ${effect.arabicName}"
    }

    fun notifyMediaProtectionTriggered() {
        _toastBanner.value = "محمي: يمنع حفظ الصور ومقاطع الفيديو أو تسجيل الشاشة"
    }

    fun triggerOwnerCustomerAlert(effect: SoundEffectType) {
        soundEngine.play(effect)
        val current = storeConfig.value
        val nextSeq = current.lastOwnerAlertSeq + 1L
        viewModelScope.launch {
            repository.saveStoreConfig(
                currentUserId,
                current.copy(
                    activeAlertSound = effect.id,
                    lastOwnerAlertSoundId = effect.id,
                    lastOwnerAlertSeq = nextSeq
                )
            )
            if (!current.chatOfflineMode) {
                repository.sendMessage(
                    currentUserId,
                    ChatMessage(
                        userId = currentUserId,
                        senderType = "owner",
                        text = "🚨 تنبيه فوري من المالك للعميل: ${effect.arabicName}",
                        soundEffectId = effect.id,
                        expiresAtEpochMs = computeExpiryMillis()
                    )
                )
            }
            _toastBanner.value = "تم إطلاق تنبيه العميل: ${effect.arabicName}"
        }
    }

    fun saveChatBackgroundMedia(mediaType: String, mediaUrl: String) {
        val current = storeConfig.value
        viewModelScope.launch {
            val res = repository.saveStoreConfig(
                currentUserId,
                current.copy(chatBgMediaType = mediaType, chatBgMediaUrl = mediaUrl)
            )
            _toastBanner.value = if (res.isSuccess) {
                "تم تحديث خلفية/فيديو المحادثة بنجاح"
            } else {
                "تعذر تحديث خلفية المحادثة"
            }
        }
    }

    fun addMultipleMediaToBgPlaylist(entries: List<BgPlaylistEntry>) {
        if (entries.isEmpty()) return
        val current = storeConfig.value
        val existing = parseBgPlaylist(current.chatBgPlaylist)
        val combined = (existing + entries).takeLast(12)
        val safeInterval = current.chatBgRotateIntervalSec.coerceIn(5, 300)
        viewModelScope.launch {
            val res = repository.saveStoreConfig(
                currentUserId,
                current.copy(
                    chatBgMediaType = "playlist",
                    chatBgAutoRotate = true,
                    chatBgRotateIntervalSec = safeInterval,
                    chatBgPlaylist = serializeBgPlaylist(combined)
                )
            )
            _toastBanner.value = if (res.isSuccess) {
                "تمت إضافة ${entries.size} إلى قائمة الخلفيات المتغيرة (كل $safeInterval ثانية)"
            } else {
                "تعذر تحديث قائمة الخلفيات"
            }
        }
    }

    fun removeBgPlaylistItem(index: Int) {
        val current = storeConfig.value
        val existing = parseBgPlaylist(current.chatBgPlaylist).toMutableList()
        if (index !in existing.indices) return
        existing.removeAt(index)
        val nextType = if (existing.isEmpty() && current.chatBgMediaType == "playlist") "animated" else current.chatBgMediaType
        viewModelScope.launch {
            repository.saveStoreConfig(
                currentUserId,
                current.copy(
                    chatBgMediaType = nextType,
                    chatBgPlaylist = serializeBgPlaylist(existing)
                )
            )
            _toastBanner.value = "تم حذف العنصر من قائمة الخلفيات المتغيرة"
        }
    }

    fun clearBgPlaylist() {
        val current = storeConfig.value
        viewModelScope.launch {
            repository.saveStoreConfig(
                currentUserId,
                current.copy(
                    chatBgMediaType = "animated",
                    chatBgPlaylist = "",
                    chatBgAutoRotate = false
                )
            )
            _toastBanner.value = "تم مسح قائمة الخلفيات واستعادة الافتراضي"
        }
    }

    fun updateBgRotationSettings(autoRotate: Boolean, intervalSec: Int) {
        val current = storeConfig.value
        val safeSec = intervalSec.coerceIn(5, 300)
        viewModelScope.launch {
            val res = repository.saveStoreConfig(
                currentUserId,
                current.copy(
                    chatBgAutoRotate = autoRotate,
                    chatBgRotateIntervalSec = safeSec
                )
            )
            if (res.isSuccess) {
                _toastBanner.value = if (autoRotate) {
                    "تم تفعيل التبديل التلقائي للخلفيات كل $safeSec ثوانٍ"
                } else {
                    "تم إيقاف التبديل التلقائي للخلفيات"
                }
            }
        }
    }

    fun generateAiRotatingBackgrounds(
        context: Context,
        themePrompt: String,
        frameCount: Int = 3,
        intervalSec: Int = 5
    ) {
        viewModelScope.launch {
            _aiBgGenState.value = UiState.Loading
            val res = aiRepository.generateAnimatedBgScenes(
                themePrompt = themePrompt,
                storeConfig = storeConfig.value,
                frameCount = frameCount
            )
            res.onSuccess { bitmaps ->
                val ts = System.currentTimeMillis()
                val newEntries = withContext(Dispatchers.IO) {
                    bitmaps.mapIndexed { idx, bmp ->
                        val fileName = "ai_chat_bg_${ts}_$idx.jpg"
                        val file = File(context.filesDir, fileName)
                        file.outputStream().use { out ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
                        }
                        BgPlaylistEntry("ai", fileName)
                    }
                }
                val current = storeConfig.value
                val combined = (parseBgPlaylist(current.chatBgPlaylist) + newEntries).takeLast(12)
                val safeSec = intervalSec.coerceIn(5, 300)
                repository.saveStoreConfig(
                    currentUserId,
                    current.copy(
                        chatBgMediaType = "playlist",
                        chatBgAutoRotate = true,
                        chatBgRotateIntervalSec = safeSec,
                        chatBgPlaylist = serializeBgPlaylist(combined)
                    )
                )
                _aiBgGenState.value = UiState.Success(newEntries.size)
                _toastBanner.value = "الذكاء الاصطناعي أنشأ ${newEntries.size} خلفيات متحركة تتغير كل $safeSec ثوانٍ!"
            }.onFailure { err ->
                _aiBgGenState.value = UiState.Error(
                    err.localizedMessage ?: "تعذر توليد خلفيات المحادثة بالذكاء الاصطناعي"
                )
            }
        }
    }

    fun toggleChatInternetSignal() {
        val current = storeConfig.value
        val nextOffline = !current.chatOfflineMode
        viewModelScope.launch {
            val res = repository.saveStoreConfig(currentUserId, current.copy(chatOfflineMode = nextOffline))
            if (res.isSuccess) {
                _toastBanner.value = if (nextOffline) {
                    "تم قطع إشارة الإنترنت عن المحادثة مؤقتاً (بدون حظر)"
                } else {
                    "تمت إعادة إشارة الإنترنت للمحادثة بنجاح"
                }
            }
        }
    }

    fun sendSoundMessageToChat(effect: SoundEffectType) {
        soundEngine.play(effect)
        if (storeConfig.value.chatOfflineMode) {
            _toastBanner.value = "إشارة الإنترنت مقطوعة عن المحادثة (بدون حظر) — تم تشغيل الصوت محلياً فقط"
            return
        }
        viewModelScope.launch {
            val expiresAt = computeExpiryMillis()
            repository.sendMessage(
                currentUserId,
                ChatMessage(
                    userId = currentUserId,
                    senderType = "owner",
                    text = "مؤثر صوتي للمحادثة: ${effect.arabicName}",
                    soundEffectId = effect.id,
                    expiresAtEpochMs = expiresAt
                )
            )
        }
    }

    fun sendCustomerMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (storeConfig.value.chatOfflineMode) {
            _toastBanner.value = "إشارة الإنترنت مقطوعة عن المحادثة مؤقتاً (بدون حظر). أعد الإشارة للإرسال والاستقبال."
            return
        }
        viewModelScope.launch {
            val expiresAt = computeExpiryMillis()
            val sendRes = repository.sendMessage(
                currentUserId,
                ChatMessage(
                    userId = currentUserId,
                    senderType = "customer",
                    text = trimmed,
                    expiresAtEpochMs = expiresAt
                )
            )
            if (sendRes.isFailure) {
                _toastBanner.value = "فشل إرسال الرسالة"
                return@launch
            }

            _aiReplyState.value = UiState.Loading
            val aiResult = aiRepository.generateCustomerReply(
                customerMessage = trimmed,
                storeConfig = storeConfig.value,
                products = products.value,
                shipments = shipments.value
            )
            aiResult.onSuccess { reply ->
                val aiExpiresAt = computeExpiryMillis()
                repository.sendMessage(
                    currentUserId,
                    ChatMessage(
                        userId = currentUserId,
                        senderType = "ai_store",
                        text = reply.replyText,
                        productId = reply.recommendedProductId,
                        shipmentId = reply.relatedShipmentId,
                        soundEffectId = reply.suggestedSoundId,
                        expiresAtEpochMs = aiExpiresAt
                    )
                )
                if (reply.suggestedSoundId.isNotBlank()) {
                    soundEngine.play(SoundEffectType.fromId(reply.suggestedSoundId))
                }
                _aiReplyState.value = UiState.Success(Unit)
            }.onFailure { err ->
                _aiReplyState.value = UiState.Error(err.localizedMessage ?: "تعذر رد الذكاء الاصطناعي")
            }
        }
    }

    fun sendProductOrderToChat(product: StoreProduct) {
        if (storeConfig.value.chatOfflineMode) {
            _toastBanner.value = "إشارة الإنترنت مقطوعة عن المحادثة (بدون حظر) — أعد الإشارة لإرسال الطلب"
            return
        }
        viewModelScope.launch {
            val expiresAt = computeExpiryMillis()
            repository.sendMessage(
                currentUserId,
                ChatMessage(
                    userId = currentUserId,
                    senderType = "customer",
                    text = "أرغب بطلب المنتج: ${product.title} بسعر ${product.price.toInt()} ${product.currency}",
                    productId = product.id,
                    expiresAtEpochMs = expiresAt
                )
            )
            val newCode = "MW-${(10000..99999).random()}"
            val shipId = repository.addShipment(
                currentUserId,
                ShipmentOrder(
                    userId = currentUserId,
                    trackingCode = newCode,
                    customerName = (userProfile.value as? UiState.Success)?.data?.displayName ?: "عميل",
                    carrier = "سمسا إكسبرس",
                    currentStep = 0,
                    statusLabel = "تم تأكيد الطلب: ${product.title}",
                    etaText = "خلال 24 ساعة"
                )
            ).getOrNull().orEmpty()

            repository.sendMessage(
                currentUserId,
                ChatMessage(
                    userId = currentUserId,
                    senderType = "ai_store",
                    text = "تم إنشاء طلبك بنجاح! رقم التتبع الخاص بك $newCode ويمكنك متابعة مسار الشحنة مباشرة أدناه.",
                    shipmentId = shipId,
                    soundEffectId = storeConfig.value.activeAlertSound,
                    expiresAtEpochMs = computeExpiryMillis()
                )
            )
            soundEngine.play(SoundEffectType.fromId(storeConfig.value.activeAlertSound))
        }
    }

    fun sendShipmentCardToChat(shipment: ShipmentOrder) {
        if (storeConfig.value.chatOfflineMode) {
            _toastBanner.value = "إشارة الإنترنت مقطوعة عن المحادثة (بدون حظر) — أعد الإشارة لتحديث الشحنة"
            return
        }
        viewModelScope.launch {
            repository.sendMessage(
                currentUserId,
                ChatMessage(
                    userId = currentUserId,
                    senderType = "ai_store",
                    text = "تحديث تتبع الشحنة ${shipment.trackingCode}: ${shipment.statusLabel}",
                    shipmentId = shipment.id,
                    expiresAtEpochMs = computeExpiryMillis()
                )
            )
        }
    }

    fun generateNewSixHourStatus(withAiImage: Boolean) {
        viewModelScope.launch {
            _aiStatusState.value = UiState.Loading
            val res = aiRepository.generateSixHourStatus(storeConfig.value, products.value)
            res.onSuccess { generated ->
                val validUntil = System.currentTimeMillis() + 6L * 60L * 60L * 1000L
                repository.addStatus(
                    currentUserId,
                    StoreStatus(
                        userId = currentUserId,
                        headline = generated.headline,
                        caption = generated.caption,
                        seoKeywords = generated.seoKeywords,
                        imagePrompt = generated.imagePrompt,
                        imageResKey = "banner",
                        validUntilEpochMs = validUntil
                    )
                )
                // Also sync SEO keywords to StoreConfig so Google SEO card stays fresh
                repository.saveStoreConfig(
                    currentUserId,
                    storeConfig.value.copy(seoKeywords = generated.seoKeywords)
                )
                _aiStatusState.value = UiState.Success(Unit)

                if (withAiImage) {
                    generatePromoImageForPrompt(generated.imagePrompt)
                }
            }.onFailure { err ->
                _aiStatusState.value = UiState.Error(err.localizedMessage ?: "تعذر توليد الحالة الذكية")
            }
        }
    }

    fun generatePromoImageForPrompt(prompt: String) {
        viewModelScope.launch {
            _generatedPromoBitmap.value = UiState.Loading
            val imgRes = aiRepository.generatePromoImage(prompt)
            imgRes.onSuccess { bmp ->
                _generatedPromoBitmap.value = UiState.Success(bmp)
            }.onFailure { err ->
                _generatedPromoBitmap.value = UiState.Error(
                    err.localizedMessage ?: "تعذر توليد الصورة بالذكاء الاصطناعي"
                )
            }
        }
    }

    fun editPromoImageWithPrompt(inputBitmap: Bitmap, editInstruction: String) {
        viewModelScope.launch {
            _generatedPromoBitmap.value = UiState.Loading
            val imgRes = aiRepository.editPromoImage(inputBitmap, editInstruction)
            imgRes.onSuccess { bmp ->
                _generatedPromoBitmap.value = UiState.Success(bmp)
            }.onFailure { err ->
                _generatedPromoBitmap.value = UiState.Error(
                    err.localizedMessage ?: "تعذر تعديل الصورة بالذكاء الاصطناعي"
                )
            }
        }
    }

    fun updateStoreConfig(updated: StoreConfig) {
        viewModelScope.launch {
            val res = repository.saveStoreConfig(currentUserId, updated)
            _toastBanner.value = if (res.isSuccess) {
                "تم حفظ إعدادات المتجر بنجاح"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "تعذر حفظ الإعدادات"
            }
        }
    }

    fun saveProduct(product: StoreProduct) {
        viewModelScope.launch {
            val res = if (product.id.isBlank()) {
                repository.addProduct(currentUserId, product.copy(userId = currentUserId))
            } else {
                repository.updateProduct(currentUserId, product)
            }
            _toastBanner.value = if (res.isSuccess) "تم حفظ المنتج" else "تعذر حفظ المنتج"
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(currentUserId, productId)
        }
    }

    fun advanceShipmentStep(shipment: ShipmentOrder) {
        val nextStep = (shipment.currentStep + 1).coerceAtMost(4)
        changeShipmentStatus(shipment, nextStep, "")
    }

    fun changeShipmentStatus(
        shipment: ShipmentOrder,
        targetStep: Int,
        customLabel: String = "",
        notifyInChat: Boolean = true
    ) {
        val safeStep = targetStep.coerceIn(0, 4)
        val defaultLabels = listOf(
            "تم تأكيد الطلب",
            "قيد التجهيز والتغليف",
            "تم التسليم لشركة الشحن",
            "في الطريق إليك",
            "تم التسليم بنجاح"
        )
        val resolvedLabel = customLabel.trim().ifBlank { defaultLabels[safeStep] }
        viewModelScope.launch {
            repository.updateShipment(
                currentUserId,
                shipment.copy(currentStep = safeStep, statusLabel = resolvedLabel)
            )
            if (notifyInChat && !storeConfig.value.chatOfflineMode) {
                repository.sendMessage(
                    currentUserId,
                    ChatMessage(
                        userId = currentUserId,
                        senderType = "owner",
                        text = "🔄 تحديث حالة الطلب (${shipment.trackingCode}): $resolvedLabel",
                        shipmentId = shipment.id,
                        expiresAtEpochMs = computeExpiryMillis()
                    )
                )
            }
            _toastBanner.value = "تم تبديل حالة الطلب إلى: $resolvedLabel"
        }
    }

    fun createShipment(trackingCode: String, customerName: String, carrier: String, etaText: String) {
        viewModelScope.launch {
            repository.addShipment(
                currentUserId,
                ShipmentOrder(
                    userId = currentUserId,
                    trackingCode = trackingCode.ifBlank { "MW-${(10000..99999).random()}" },
                    customerName = customerName.ifBlank { "عميل المتجر" },
                    carrier = carrier.ifBlank { "أرامكس السريع" },
                    currentStep = 0,
                    statusLabel = "تم تأكيد الطلب",
                    etaText = etaText.ifBlank { "خلال 24 ساعة" }
                )
            )
            _toastBanner.value = "تمت إضافة الشحنة بنجاح"
        }
    }

    private fun computeExpiryMillis(): Long {
        val duration = DisappearingMode.fromId(storeConfig.value.disappearingMode).durationMs
        return if (duration == null) 0L else System.currentTimeMillis() + duration
    }

    private fun unwrapFirestoreError(error: Throwable): Throwable? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    companion object {
        private const val TAG = "MatjarViewModel"

        fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MatjarViewModel(
                    repository = StoreRepository(db),
                    aiRepository = StoreAiRepository(),
                    soundEngine = SoundEffectEngine(),
                    currentUserId = currentUserId
                )
            }
        }
    }
}
