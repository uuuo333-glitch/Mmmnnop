package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ChatMessage
import com.example.data.model.ShipmentOrder
import com.example.data.model.StoreConfig
import com.example.data.model.StoreProduct
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveProfileAndStoreConfig_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StoreRepository(firestore)

        val profileRes = withTimeout(TIMEOUT_MS) {
            repository.saveUserProfile(
                uid,
                UserProfile(
                    userId = uid,
                    phoneNumber = "551234567",
                    countryCode = "+966",
                    displayName = "متجر السرعة",
                    role = "owner",
                    subscriptionActive = true
                )
            )
        }
        assertTrue(profileRes.isSuccess)

        val configRes = withTimeout(TIMEOUT_MS) {
            repository.saveStoreConfig(
                uid,
                StoreConfig(userId = uid, storeName = "متجر النخبة")
            )
        }
        assertTrue(configRes.isSuccess)

        val loadedConfig = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeStoreConfig(uid).first { it != null }
        }
        assertEquals("متجر النخبة", loadedConfig?.storeName)
    }

    @Test
    fun addProductAndSendMessage_authenticatedOwner_emitsUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StoreRepository(firestore)

        val productId = withTimeout(TIMEOUT_MS) {
            repository.addProduct(
                uid,
                StoreProduct(
                    userId = uid,
                    title = "تيربو احترافي",
                    description = "تيربو عالي الأداء",
                    price = 1500.0,
                    currency = "ر.س",
                    category = "تيربو",
                    imageResKey = "turbo",
                    inStock = true
                )
            ).getOrThrow()
        }
        assertTrue(productId.isNotBlank())

        val msgId = withTimeout(TIMEOUT_MS) {
            repository.sendMessage(
                uid,
                ChatMessage(
                    userId = uid,
                    senderType = "customer",
                    text = "بكم التيربو؟",
                    expiresAtEpochMs = 0L
                )
            ).getOrThrow()
        }
        assertTrue(msgId.isNotBlank())

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages(uid).first { list -> list.any { it.id == msgId } }
        }
        assertTrue(messages.any { it.id == msgId })
    }

    @Test
    fun addAndUpdateShipment_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StoreRepository(firestore)

        val shipId = withTimeout(TIMEOUT_MS) {
            repository.addShipment(
                uid,
                ShipmentOrder(
                    userId = uid,
                    trackingCode = "MW-1001",
                    customerName = "أحمد",
                    carrier = "سمسا",
                    currentStep = 1,
                    statusLabel = "قيد التجهيز",
                    etaText = "غداً"
                )
            ).getOrThrow()
        }

        val shipments = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeShipments(uid).first { list -> list.any { it.id == shipId } }
        }
        val created = shipments.first { it.id == shipId }
        val updateRes = withTimeout(TIMEOUT_MS) {
            repository.updateShipment(
                uid,
                created.copy(currentStep = 2, statusLabel = "خرج للتوصيل")
            )
        }
        assertTrue(updateRes.isSuccess)
    }

    @Test
    fun crossUserProductAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StoreRepository(firestore)
        withTimeout(TIMEOUT_MS) {
            aliceRepo.addProduct(
                aliceUid,
                StoreProduct(
                    userId = aliceUid,
                    title = "منتج أليس",
                    description = "وصف",
                    price = 100.0,
                    currency = "ر.س",
                    category = "عام",
                    imageResKey = "turbo",
                    inStock = true
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = StoreRepository(firestore)
        var denied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeProducts(aliceUid).first()
            }
        } catch (e: Exception) {
            val fsErr = e.unwrapFirestoreException()
            denied = fsErr?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Expected PERMISSION_DENIED on cross-user read", denied)
    }

    @Test
    fun unauthenticatedObserveProducts_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = StoreRepository(firestore)
        var denied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProducts("some_user_123").first()
            }
        } catch (e: Exception) {
            val fsErr = e.unwrapFirestoreException()
            denied = fsErr?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Expected PERMISSION_DENIED for unauthenticated caller", denied)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_store@test.com"
        const val BOB_EMAIL = "bob_store@test.com"
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
