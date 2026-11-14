package com.example.ui

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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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
        role: String
    ) {
        viewModelScope.launch {
            val profile = UserProfile(
                userId = currentUserId,
                phoneNumber = phoneNumber.trim(),
                countryCode = countryCode.trim(),
                displayName = displayName.trim(),
                role = role,
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

    fun toggleUserRole(newRole: String) {
        val current = (userProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.saveUserProfile(currentUserId, current.copy(role = newRole))
        }
    }

    fun toggleSubscriptionActive(active: Boolean) {
        val current = (userProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.saveUserProfile(currentUserId, current.copy(subscriptionActive = active))
            _toastBanner.value = if (active) "تم تفعيل الاشتراك الشهري" else "تم إيقاف الاشتراك مؤقتاً"
        }
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

    fun sendSoundMessageToChat(effect: SoundEffectType) {
        soundEngine.play(effect)
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
        val labels = listOf(
            "تم تأكيد الطلب",
            "قيد التجهيز والتغليف",
            "تم التسليم لشركة الشحن",
            "في الطريق إليك",
            "تم التسليم بنجاح"
        )
        viewModelScope.launch {
            repository.updateShipment(
                currentUserId,
                shipment.copy(currentStep = nextStep, statusLabel = labels[nextStep])
            )
            _toastBanner.value = "تم تحديث حالة الشحنة إلى: ${labels[nextStep]}"
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
