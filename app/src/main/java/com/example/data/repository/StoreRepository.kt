package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.ShipmentOrder
import com.example.data.model.StoreConfig
import com.example.data.model.StoreProduct
import com.example.data.model.StoreStatus
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userDoc(userId: String) = db.collection("users").document(userId)

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        userDoc(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.GET, "users/$userId")
                throw error
            }

    suspend fun saveUserProfile(userId: String, profile: UserProfile): Result<Unit> {
        val docRef = userDoc(userId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(profile.copy(userId = userId).toUpdateMap()).await()
            } else {
                docRef.set(profile.copy(userId = userId).toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeStoreConfig(userId: String): Flow<StoreConfig?> =
        userDoc(userId).collection("storeConfig").document(MAIN_CONFIG_ID)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        StoreConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.GET, "users/$userId/storeConfig/$MAIN_CONFIG_ID")
                throw error
            }

    suspend fun saveStoreConfig(userId: String, config: StoreConfig): Result<Unit> {
        val docRef = userDoc(userId).collection("storeConfig").document(MAIN_CONFIG_ID)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(config.copy(userId = userId).toUpdateMap()).await()
            } else {
                docRef.set(config.copy(userId = userId).toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeProducts(userId: String): Flow<List<StoreProduct>> =
        userDoc(userId).collection("products")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StoreProduct::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, "users/$userId/products")
                throw error
            }

    suspend fun addProduct(userId: String, product: StoreProduct): Result<String> {
        val docRef = userDoc(userId).collection("products").document()
        return try {
            docRef.set(product.copy(userId = userId).toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProduct(userId: String, product: StoreProduct): Result<Unit> {
        require(product.id.isNotBlank()) { "Product ID must not be blank" }
        val docRef = userDoc(userId).collection("products").document(product.id)
        return try {
            docRef.update(product.copy(userId = userId).toUpdateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(userId: String, productId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("products").document(productId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeMessages(userId: String): Flow<List<ChatMessage>> =
        userDoc(userId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, "users/$userId/messages")
                throw error
            }

    suspend fun sendMessage(userId: String, message: ChatMessage): Result<String> {
        val docRef = userDoc(userId).collection("messages").document()
        return try {
            docRef.set(message.copy(userId = userId).toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(userId: String, messageId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("messages").document(messageId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeStatuses(userId: String): Flow<List<StoreStatus>> =
        userDoc(userId).collection("statuses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StoreStatus::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, "users/$userId/statuses")
                throw error
            }

    suspend fun addStatus(userId: String, status: StoreStatus): Result<String> {
        val docRef = userDoc(userId).collection("statuses").document()
        return try {
            docRef.set(status.copy(userId = userId).toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeShipments(userId: String): Flow<List<ShipmentOrder>> =
        userDoc(userId).collection("shipments")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ShipmentOrder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, "users/$userId/shipments")
                throw error
            }

    suspend fun addShipment(userId: String, shipment: ShipmentOrder): Result<String> {
        val docRef = userDoc(userId).collection("shipments").document()
        return try {
            docRef.set(shipment.copy(userId = userId).toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateShipment(userId: String, shipment: ShipmentOrder): Result<Unit> {
        require(shipment.id.isNotBlank()) { "Shipment ID must not be blank" }
        val docRef = userDoc(userId).collection("shipments").document(shipment.id)
        return try {
            docRef.update(shipment.copy(userId = userId).toUpdateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedInitialStoreIfEmpty(userId: String, ownerName: String): Result<Unit> {
        return try {
            val configRef = userDoc(userId).collection("storeConfig").document(MAIN_CONFIG_ID)
            if (!configRef.get().await().exists()) {
                val initialConfig = StoreConfig(
                    userId = userId,
                    storeName = if (ownerName.isNotBlank()) "متجر $ownerName" else "متجر تيربو الذكي"
                )
                configRef.set(initialConfig.toCreateMap()).await()
            }

            val productsSnap = userDoc(userId).collection("products").limit(1).get().await()
            var firstProdId = ""
            if (productsSnap.isEmpty) {
                val starterProducts = listOf(
                    StoreProduct(
                        userId = userId,
                        title = "تيربو ألمنيوم سباقات GT-35",
                        description = "تيربو احترافي عالي التحمل مع صوت تصفير تيربو نقي واستجابة فورية.",
                        price = 1850.0,
                        currency = "ر.س",
                        category = "تيربو وأداء",
                        imageResKey = "turbo",
                        inStock = true
                    ),
                    StoreProduct(
                        userId = userId,
                        title = "شكمان قزوز تيتانيوم بفرقعات رياضية",
                        description = "نظام عادم تيتانيوم مزدوج بأطراف زرقاء وصوت فرقعات قزوز مميز.",
                        price = 1290.0,
                        currency = "ر.س",
                        category = "شكمان وقزوز",
                        imageResKey = "exhaust",
                        inStock = true
                    ),
                    StoreProduct(
                        userId = userId,
                        title = "شمعات LED ماتريكس أزرق جليدي",
                        description = "إضاءة أمامية فائقة السطوع بتقنية ماتريكس LED مقاومة للماء.",
                        price = 940.0,
                        currency = "ر.س",
                        category = "إضاءة",
                        imageResKey = "headlight",
                        inStock = true
                    )
                )
                for (prod in starterProducts) {
                    val ref = userDoc(userId).collection("products").document()
                    if (firstProdId.isEmpty()) firstProdId = ref.id
                    ref.set(prod.toCreateMap()).await()
                }
            }

            val shipSnap = userDoc(userId).collection("shipments").limit(1).get().await()
            var firstShipId = ""
            if (shipSnap.isEmpty) {
                val ref = userDoc(userId).collection("shipments").document()
                firstShipId = ref.id
                val starterShipment = ShipmentOrder(
                    userId = userId,
                    trackingCode = "MW-88421",
                    customerName = ownerName.ifBlank { "عميل المتجر" },
                    carrier = "أرامكس إكسبرس",
                    currentStep = 3,
                    statusLabel = "في الطريق للتسليم",
                    etaText = "اليوم • 5:30 مساءً"
                )
                ref.set(starterShipment.toCreateMap()).await()
            }

            val msgSnap = userDoc(userId).collection("messages").limit(1).get().await()
            if (msgSnap.isEmpty) {
                val welcomeMsg = ChatMessage(
                    userId = userId,
                    senderType = "ai_store",
                    text = "أهلاً بك في متجرنا المعتمد! أنا المساعد الذكي للمتجر، يمكنني عرض المنتجات، الرد على استفساراتك، تشغيل أصوات التنبيه (تيربو، قزوز، بواري شاحنة)، وعرض تتبع شحنتك مباشرة هنا.",
                    productId = firstProdId,
                    shipmentId = firstShipId,
                    soundEffectId = "turbo_spool",
                    mediaType = "reel",
                    mediaLabel = "فيديو استعراضي محمي للمتجر",
                    expiresAtEpochMs = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
                )
                userDoc(userId).collection("messages").document()
                    .set(welcomeMsg.toCreateMap()).await()
            }

            val statusSnap = userDoc(userId).collection("statuses").limit(1).get().await()
            if (statusSnap.isEmpty) {
                val starterStatus = StoreStatus(
                    userId = userId,
                    headline = "وصل حديثاً: بكج التيربو والقزوز الرياضي",
                    caption = "احصل على أداء رياضي فائق مع شحن سريع ومراقبة مباشرة للشحنة داخل المحادثة.",
                    seoKeywords = "متجر سيارات, تيربو رياضي, قزوز فرقعات, إضاءات LED, توصيل سريع",
                    imagePrompt = "Sleek sports car turbocharger and titanium exhaust in neon emerald showroom",
                    imageResKey = "banner",
                    validUntilEpochMs = System.currentTimeMillis() + 6L * 60L * 60L * 1000L
                )
                userDoc(userId).collection("statuses").document()
                    .set(starterStatus.toCreateMap()).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId/seed")
            Result.failure(e)
        }
    }

    companion object {
        const val MAIN_CONFIG_ID = "main"
    }
}
