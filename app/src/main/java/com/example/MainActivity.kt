package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firebase.configureAppCheck
import com.example.ui.MatjarViewModel
import com.example.ui.UiState
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.PendingRegistrationStore
import com.example.ui.auth.PhoneRegistrationScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.OwnerDashboardScreen
import com.example.ui.screens.StatusesScreen
import com.example.ui.theme.MatjarTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

private enum class StoreTab(val label: String) {
    CHAT("الدردشة"),
    CATALOG("المتجر"),
    STATUSES("الحالات والسيو"),
    OWNER("لوحة المالك")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge (FLAG_SECURE is controlled dynamically by owner privacy config)
        enableEdgeToEdge()

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MatjarTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppRoot(
                            onToggleScreenshotProtection = { blockScreenshots ->
                                if (blockScreenshots) {
                                    window.setFlags(
                                        WindowManager.LayoutParams.FLAG_SECURE,
                                        WindowManager.LayoutParams.FLAG_SECURE
                                    )
                                } else {
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRoot(
    onToggleScreenshotProtection: (Boolean) -> Unit
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            AuthenticatedStoreShell(
                currentUserId = user.uid,
                defaultDisplayName = user.displayName.orEmpty(),
                onToggleScreenshotProtection = onToggleScreenshotProtection
            )
        }
    }
}

@Composable
private fun AuthenticatedStoreShell(
    currentUserId: String,
    defaultDisplayName: String,
    onToggleScreenshotProtection: (Boolean) -> Unit
) {
    val viewModel: MatjarViewModel = viewModel(
        key = currentUserId,
        factory = MatjarViewModel.provideFactory(currentUserId)
    )

    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    val shipments by viewModel.shipments.collectAsStateWithLifecycle()
    val aiReplyState by viewModel.aiReplyState.collectAsStateWithLifecycle()
    val aiStatusState by viewModel.aiStatusState.collectAsStateWithLifecycle()
    val promoBitmapState by viewModel.generatedPromoBitmap.collectAsStateWithLifecycle()
    val bellPulseCount by viewModel.bellPulseCount.collectAsStateWithLifecycle()
    val toastBanner by viewModel.toastBanner.collectAsStateWithLifecycle()

    LaunchedEffect(storeConfig.blockScreenshots) {
        onToggleScreenshotProtection(storeConfig.blockScreenshots)
    }

    MatjarTheme(
        themeMode = storeConfig.themeMode,
        accentColorHex = storeConfig.accentColorHex
    ) {
        when (val pState = profileState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                PhoneRegistrationScreen(
                    initialName = defaultDisplayName,
                    monthlyPrice = storeConfig.monthlySubscriptionPrice,
                    currency = storeConfig.subscriptionCurrency,
                    onRegisterSubmit = { phone, code, name, role ->
                        viewModel.registerPhoneAndProfile(phone, code, name, role)
                    }
                )
            }
            is UiState.Success -> {
                val profile = pState.data
                val contextForReg = LocalContext.current
                LaunchedEffect(profile) {
                    if (profile == null || profile.phoneNumber.isBlank()) {
                        val pending = PendingRegistrationStore.consume(contextForReg)
                        if (pending != null) {
                            viewModel.registerPhoneAndProfile(
                                phoneNumber = pending.phoneNumber,
                                countryCode = pending.countryCode,
                                displayName = pending.displayName.ifBlank { defaultDisplayName },
                                role = pending.role
                            )
                        }
                    }
                }
                if (profile == null || profile.phoneNumber.isBlank()) {
                    PhoneRegistrationScreen(
                        initialName = defaultDisplayName,
                        monthlyPrice = storeConfig.monthlySubscriptionPrice,
                        currency = storeConfig.subscriptionCurrency,
                        onRegisterSubmit = { phone, code, name, role ->
                            viewModel.registerPhoneAndProfile(phone, code, name, role)
                        }
                    )
                } else {
                    var selectedTab by remember { mutableStateOf(StoreTab.CHAT) }
                    val snackbarHostState = remember { SnackbarHostState() }
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val credentialManager = remember(context) { CredentialManager.create(context) }

                    LaunchedEffect(toastBanner) {
                        val msg = toastBanner
                        if (!msg.isNullOrBlank()) {
                            snackbarHostState.showSnackbar(msg)
                            viewModel.clearToastBanner()
                        }
                    }

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            NavigationBar(
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                StoreTab.entries.forEach { tab ->
                                    val selected = selectedTab == tab
                                    val icon = when (tab) {
                                        StoreTab.CHAT -> if (selected) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat
                                        StoreTab.CATALOG -> if (selected) Icons.Filled.Storefront else Icons.Outlined.Storefront
                                        StoreTab.STATUSES -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
                                        StoreTab.OWNER -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
                                    }
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { selectedTab = tab },
                                        icon = { Icon(icon, contentDescription = tab.label) },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (selectedTab) {
                                StoreTab.CHAT -> ChatScreen(
                                    storeConfig = storeConfig,
                                    messages = messages,
                                    products = products,
                                    shipments = shipments,
                                    aiReplyState = aiReplyState,
                                    bellPulseCount = bellPulseCount,
                                    onBellClick = { viewModel.triggerBellHorn() },
                                    onSendMessage = { viewModel.sendCustomerMessage(it) },
                                    onOrderProduct = { viewModel.sendProductOrderToChat(it) },
                                    onPlaySound = { viewModel.playSoundEffect(it) },
                                    onSendSoundToChat = { viewModel.sendSoundMessageToChat(it) },
                                    onSendShipmentUpdate = { viewModel.sendShipmentCardToChat(it) },
                                    onOpenOwnerStudio = { selectedTab = StoreTab.OWNER }
                                )
                                StoreTab.CATALOG -> CatalogScreen(
                                    storeConfig = storeConfig,
                                    products = products,
                                    isOwnerMode = profile.role != "customer",
                                    onOrderProductInChat = { prod ->
                                        viewModel.sendProductOrderToChat(prod)
                                        selectedTab = StoreTab.CHAT
                                    },
                                    onSaveProduct = { viewModel.saveProduct(it) },
                                    onDeleteProduct = { viewModel.deleteProduct(it) }
                                )
                                StoreTab.STATUSES -> StatusesScreen(
                                    storeConfig = storeConfig,
                                    statuses = statuses,
                                    aiStatusState = aiStatusState,
                                    generatedPromoBitmap = promoBitmapState,
                                    onGenerateStatusAndSeo = { withImg ->
                                        viewModel.generateNewSixHourStatus(withImg)
                                    },
                                    onGenerateImageOnly = { prompt ->
                                        viewModel.generatePromoImageForPrompt(prompt)
                                    }
                                )
                                StoreTab.OWNER -> OwnerDashboardScreen(
                                    userProfile = profile,
                                    storeConfig = storeConfig,
                                    shipments = shipments,
                                    onSaveConfig = { viewModel.updateStoreConfig(it) },
                                    onPlaySound = { viewModel.playSoundEffect(it) },
                                    onAdvanceShipment = { viewModel.advanceShipmentStep(it) },
                                    onCreateShipment = { code, cust, carrier, eta ->
                                        viewModel.createShipment(code, cust, carrier, eta)
                                    },
                                    onToggleRole = { viewModel.toggleUserRole(it) },
                                    onToggleSubscription = { viewModel.toggleSubscriptionActive(it) },
                                    onSignOut = {
                                        signOutUser(credentialManager, scope)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            UiState.Idle -> {}
        }
    }
}
