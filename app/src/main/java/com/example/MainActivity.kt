package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firebase.configureAppCheck
import com.example.data.model.SoundEffectType
import com.example.data.model.StoreConfig
import com.example.data.model.UserProfile
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
import com.example.ui.theme.VerifiedBlue
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import java.util.function.Consumer

private tailrec fun Context.findComponentActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

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
                defaultPhoneNumber = user.phoneNumber.orEmpty(),
                onToggleScreenshotProtection = onToggleScreenshotProtection
            )
        }
    }
}

@Composable
private fun AuthenticatedStoreShell(
    currentUserId: String,
    defaultDisplayName: String,
    defaultPhoneNumber: String,
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
    val aiBgGenState by viewModel.aiBgGenState.collectAsStateWithLifecycle()
    val bellPulseCount by viewModel.bellPulseCount.collectAsStateWithLifecycle()
    val toastBanner by viewModel.toastBanner.collectAsStateWithLifecycle()

    val shellContext = LocalContext.current
    var isScreenRecordingActive by remember { mutableStateOf(false) }

    // Android 15+ (API 35) live screen recording detection callback
    DisposableEffect(shellContext) {
        val activity = shellContext.findComponentActivity()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && activity != null) {
            val callback = Consumer<Int> { state ->
                isScreenRecordingActive = (state == WindowManager.SCREEN_RECORDING_STATE_VISIBLE)
            }
            val initialState = activity.windowManager.addScreenRecordingCallback(
                activity.mainExecutor,
                callback
            )
            isScreenRecordingActive = (initialState == WindowManager.SCREEN_RECORDING_STATE_VISIBLE)
            onDispose {
                activity.windowManager.removeScreenRecordingCallback(callback)
            }
        } else {
            onDispose {}
        }
    }

    LaunchedEffect(storeConfig.blockScreenshots) {
        onToggleScreenshotProtection(storeConfig.blockScreenshots)
    }

    MatjarTheme(
        themeMode = storeConfig.themeMode,
        accentColorHex = storeConfig.accentColorHex
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val pState = profileState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    PhoneRegistrationScreen(
                        initialName = defaultDisplayName,
                        initialPhone = defaultPhoneNumber,
                        monthlyPrice = storeConfig.monthlySubscriptionPrice,
                        currency = storeConfig.subscriptionCurrency,
                        onRegisterSubmit = { phone, code, name, role, phone2, code2, phone3, code3 ->
                            viewModel.registerPhoneAndProfile(
                                phoneNumber = phone,
                                countryCode = code,
                                displayName = name,
                                role = role,
                                phoneNumber2 = phone2,
                                countryCode2 = code2,
                                phoneNumber3 = phone3,
                                countryCode3 = code3
                            )
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
                                    role = "customer",
                                    phoneNumber2 = pending.phoneNumber2,
                                    countryCode2 = pending.countryCode2,
                                    phoneNumber3 = pending.phoneNumber3,
                                    countryCode3 = pending.countryCode3
                                )
                            }
                        }
                    }
                    if (profile == null || profile.phoneNumber.isBlank()) {
                        PhoneRegistrationScreen(
                            initialName = defaultDisplayName,
                            initialPhone = defaultPhoneNumber,
                            monthlyPrice = storeConfig.monthlySubscriptionPrice,
                            currency = storeConfig.subscriptionCurrency,
                            onRegisterSubmit = { phone, code, name, role, phone2, code2, phone3, code3 ->
                                viewModel.registerPhoneAndProfile(
                                    phoneNumber = phone,
                                    countryCode = code,
                                    displayName = name,
                                    role = role,
                                    phoneNumber2 = phone2,
                                    countryCode2 = code2,
                                    phoneNumber3 = phone3,
                                    countryCode3 = code3
                                )
                            }
                        )
                    } else {
                        val isOwnerMode = profile.role == "owner"
                        val visibleTabs = remember(isOwnerMode) {
                            if (isOwnerMode) StoreTab.entries
                            else listOf(StoreTab.CHAT, StoreTab.CATALOG, StoreTab.STATUSES)
                        }
                        var selectedTab by remember { mutableStateOf(StoreTab.CHAT) }
                        var showCustomerStoreDialog by remember { mutableStateOf(false) }
                        var showOwnerPinDialog by remember { mutableStateOf(false) }
                        var lastSeenAlertSeq by remember { mutableLongStateOf(-1L) }

                        val snackbarHostState = remember { SnackbarHostState() }
                        val context = LocalContext.current
                        val scope = rememberCoroutineScope()
                        val credentialManager = remember(context) { CredentialManager.create(context) }

                        LaunchedEffect(isOwnerMode) {
                            if (!isOwnerMode && selectedTab == StoreTab.OWNER) {
                                selectedTab = StoreTab.CHAT
                            }
                        }

                        // Automatically play owner's live alert sound when synced on customer screen
                        LaunchedEffect(storeConfig.lastOwnerAlertSeq, isOwnerMode) {
                            val seq = storeConfig.lastOwnerAlertSeq
                            if (lastSeenAlertSeq < 0L) {
                                lastSeenAlertSeq = seq
                            } else if (seq > lastSeenAlertSeq) {
                                lastSeenAlertSeq = seq
                                val fx = SoundEffectType.fromId(storeConfig.lastOwnerAlertSoundId)
                                if (!isOwnerMode) {
                                    viewModel.playSoundEffect(fx)
                                }
                            }
                        }

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
                                    visibleTabs.forEach { tab ->
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
                                        aiBgGenState = aiBgGenState,
                                        bellPulseCount = bellPulseCount,
                                        isOwnerMode = isOwnerMode,
                                        onBellClick = { viewModel.triggerBellHorn() },
                                        onSendMessage = { viewModel.sendCustomerMessage(it) },
                                        onOrderProduct = { viewModel.sendProductOrderToChat(it) },
                                        onPlaySound = { viewModel.playSoundEffect(it) },
                                        onSendSoundToChat = { viewModel.sendSoundMessageToChat(it) },
                                        onTriggerOwnerAlert = { viewModel.triggerOwnerCustomerAlert(it) },
                                        onSendShipmentUpdate = { viewModel.sendShipmentCardToChat(it) },
                                        onChangeShipmentStatus = { ship, step, label ->
                                            viewModel.changeShipmentStatus(ship, step, label)
                                        },
                                        onUpdateBgRotation = { autoRotate, intervalSec ->
                                            viewModel.updateBgRotationSettings(autoRotate, intervalSec)
                                        },
                                        onGenerateAiRotatingBackgrounds = { prompt, count, intervalSec ->
                                            viewModel.generateAiRotatingBackgrounds(context, prompt, count, intervalSec)
                                        },
                                        onToggleChatInternetSignal = { viewModel.toggleChatInternetSignal() },
                                        onBlockedMediaAttempt = { viewModel.notifyMediaProtectionTriggered() },
                                        onSaveTickerText = { newTicker ->
                                            viewModel.updateStoreConfig(storeConfig.copy(tickerText = newTicker))
                                        },
                                        onOpenOwnerStudio = {
                                            if (isOwnerMode) {
                                                selectedTab = StoreTab.OWNER
                                            } else {
                                                showCustomerStoreDialog = true
                                            }
                                        }
                                    )
                                    StoreTab.CATALOG -> CatalogScreen(
                                        storeConfig = storeConfig,
                                        products = products,
                                        isOwnerMode = isOwnerMode,
                                        onOrderProductInChat = { prod ->
                                            viewModel.sendProductOrderToChat(prod)
                                            selectedTab = StoreTab.CHAT
                                        },
                                        onSaveProduct = { viewModel.saveProduct(it) },
                                        onDeleteProduct = { viewModel.deleteProduct(it) },
                                        onBlockedMediaAttempt = { viewModel.notifyMediaProtectionTriggered() }
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
                                        },
                                        onEditImage = { bmp, prompt ->
                                            viewModel.editPromoImageWithPrompt(bmp, prompt)
                                        }
                                    )
                                    StoreTab.OWNER -> OwnerDashboardScreen(
                                        userProfile = profile,
                                        storeConfig = storeConfig,
                                        shipments = shipments,
                                        aiBgGenState = aiBgGenState,
                                        onSaveConfig = { viewModel.updateStoreConfig(it) },
                                        onSaveThreeNumbers = { p1, c1, p2, c2, p3, c3 ->
                                            viewModel.updateBusinessPhones(p1, c1, p2, c2, p3, c3)
                                        },
                                        onPlaySound = { viewModel.playSoundEffect(it) },
                                        onTriggerOwnerAlert = { viewModel.triggerOwnerCustomerAlert(it) },
                                        onSaveChatMedia = { type, url ->
                                            viewModel.saveChatBackgroundMedia(type, url)
                                        },
                                        onAddMultipleBgMedia = { entries ->
                                            viewModel.addMultipleMediaToBgPlaylist(entries)
                                        },
                                        onRemoveBgPlaylistItem = { idx ->
                                            viewModel.removeBgPlaylistItem(idx)
                                        },
                                        onClearBgPlaylist = { viewModel.clearBgPlaylist() },
                                        onUpdateBgRotation = { autoRotate, intervalSec ->
                                            viewModel.updateBgRotationSettings(autoRotate, intervalSec)
                                        },
                                        onGenerateAiRotatingBackgrounds = { prompt, count, intervalSec ->
                                            viewModel.generateAiRotatingBackgrounds(context, prompt, count, intervalSec)
                                        },
                                        onAdvanceShipment = { viewModel.advanceShipmentStep(it) },
                                        onChangeShipmentStatus = { ship, step, label ->
                                            viewModel.changeShipmentStatus(ship, step, label)
                                        },
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

                        if (showCustomerStoreDialog) {
                            CustomerAccountAndStoreDialog(
                                storeConfig = storeConfig,
                                userProfile = profile,
                                onDismiss = { showCustomerStoreDialog = false },
                                onSaveThreeNumbers = { p1, c1, p2, c2, p3, c3 ->
                                    viewModel.updateBusinessPhones(p1, c1, p2, c2, p3, c3)
                                    showCustomerStoreDialog = false
                                },
                                onRedeemSubscriptionCode = { code, onFeedback ->
                                    viewModel.redeemSubscriptionCode(code, onFeedback)
                                },
                                onRequestOwnerUnlock = {
                                    showCustomerStoreDialog = false
                                    showOwnerPinDialog = true
                                },
                                onSignOut = {
                                    showCustomerStoreDialog = false
                                    signOutUser(credentialManager, scope)
                                }
                            )
                        }

                        if (showOwnerPinDialog) {
                            OwnerPinUnlockDialog(
                                hasExistingPin = storeConfig.ownerAccessPin.isNotBlank(),
                                onDismiss = { showOwnerPinDialog = false },
                                onSubmitPin = { pin, onFeedback ->
                                    viewModel.unlockOwnerModeWithPin(pin) { ok, err ->
                                        if (ok) {
                                            showOwnerPinDialog = false
                                            selectedTab = StoreTab.OWNER
                                        } else {
                                            onFeedback(err ?: "رمز المالك غير صحيح")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                UiState.Idle -> {}
            }

            // Full-screen Privacy Veil when Android 15+ reports active screen recording
            if (isScreenRecordingActive && (storeConfig.blockScreenshots || storeConfig.blockMediaCopy)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xF5070E14))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = "حماية ضد تسجيل الشاشة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "تم حجب الشاشة والوسائط أثناء تسجيل الشاشة",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "يمنع تسجيل الشاشة أو حفظ الصور ومقاطع الفيديو داخل المتجر لحماية الخصوصية.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerAccountAndStoreDialog(
    storeConfig: StoreConfig,
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onSaveThreeNumbers: (p1: String, c1: String, p2: String, c2: String, p3: String, c3: String) -> Unit,
    onRedeemSubscriptionCode: (code: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onRequestOwnerUnlock: () -> Unit,
    onSignOut: () -> Unit
) {
    var phone1 by remember(userProfile.phoneNumber) { mutableStateOf(userProfile.phoneNumber) }
    var code1 by remember(userProfile.countryCode) { mutableStateOf(userProfile.countryCode.ifBlank { "+966" }) }
    var phone2 by remember(userProfile.phoneNumber2) { mutableStateOf(userProfile.phoneNumber2) }
    var code2 by remember(userProfile.countryCode2) { mutableStateOf(userProfile.countryCode2.ifBlank { "+966" }) }
    var phone3 by remember(userProfile.phoneNumber3) { mutableStateOf(userProfile.phoneNumber3) }
    var code3 by remember(userProfile.countryCode3) { mutableStateOf(userProfile.countryCode3.ifBlank { "+966" }) }
    var subCodeInput by remember { mutableStateOf("") }
    var subCodeFeedback by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(storeConfig.storeName, style = MaterialTheme.typography.titleLarge)
                if (storeConfig.isVerified) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = "متجر موثق",
                        tint = VerifiedBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = storeConfig.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = "الاشتراك المحدد من المالك",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "${storeConfig.monthlySubscriptionPrice.toInt()} ${storeConfig.subscriptionCurrency} • المدة: ${storeConfig.subscriptionMonths} شهر و ${storeConfig.subscriptionDays} يوم",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (userProfile.subscriptionExpiresAtEpochMs > 0L) {
                                    val remDays = ((userProfile.subscriptionExpiresAtEpochMs - System.currentTimeMillis()) / (1000L * 60L * 60L * 24L)).coerceAtLeast(0L)
                                    Text(
                                        text = "المتبقي في اشتراكك: ~$remDays يوم",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = subCodeInput,
                                onValueChange = {
                                    subCodeInput = it.filter { ch -> ch.isDigit() }.take(4)
                                    subCodeFeedback = null
                                },
                                label = { Text("رمز الاشتراك (4 أرقام)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("customer_sub_code_input")
                            )
                            Button(
                                onClick = {
                                    onRedeemSubscriptionCode(subCodeInput) { ok, err ->
                                        subCodeFeedback = if (ok) "تم التفعيل ✓" else err
                                    }
                                },
                                modifier = Modifier.testTag("customer_redeem_sub_code_button")
                            ) {
                                Text("تفعيل")
                            }
                        }
                        if (!subCodeFeedback.isNullOrBlank()) {
                            Text(
                                text = subCodeFeedback.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (subCodeFeedback?.contains("✓") == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (storeConfig.instagramHandle.isNotBlank() || storeConfig.snapchatHandle.isNotBlank()) {
                    Text(
                        text = "إنستقرام: ${storeConfig.instagramHandle} • سناب: ${storeConfig.snapchatHandle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("أرقامك المسجلة (حتى 3 أرقام مثل واتساب أعمال):", style = MaterialTheme.typography.labelLarge)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = code1,
                        onValueChange = { code1 = it },
                        label = { Text("الرمز") },
                        singleLine = true,
                        modifier = Modifier.width(84.dp)
                    )
                    OutlinedTextField(
                        value = phone1,
                        onValueChange = { phone1 = it.filter { ch -> ch.isDigit() }.take(15) },
                        label = { Text("الرقم 1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = code2,
                        onValueChange = { code2 = it },
                        label = { Text("الرمز") },
                        singleLine = true,
                        modifier = Modifier.width(84.dp)
                    )
                    OutlinedTextField(
                        value = phone2,
                        onValueChange = { phone2 = it.filter { ch -> ch.isDigit() }.take(15) },
                        label = { Text("الرقم 2") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = code3,
                        onValueChange = { code3 = it },
                        label = { Text("الرمز") },
                        singleLine = true,
                        modifier = Modifier.width(84.dp)
                    )
                    OutlinedTextField(
                        value = phone3,
                        onValueChange = { phone3 = it.filter { ch -> ch.isDigit() }.take(15) },
                        label = { Text("الرقم 3") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onRequestOwnerUnlock,
                        modifier = Modifier.testTag("open_owner_pin_dialog_button")
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("دخول المالك بالرمز")
                    }
                    TextButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تسجيل خروج")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveThreeNumbers(phone1, code1, phone2, code2, phone3, code3) }
            ) {
                Text("حفظ الأرقام")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun OwnerPinUnlockDialog(
    hasExistingPin: Boolean,
    onDismiss: () -> Unit,
    onSubmitPin: (pin: String, onFeedback: (String) -> Unit) -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(if (hasExistingPin) "دخول المالك المحمي" else "تعيين رمز المالك السري")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (hasExistingPin) {
                        "أدخل الرمز السري للمالك المكون من 4 أرقام لفتح لوحة التحكم. تبقى بيانات دخول المالك مخفية عن العملاء."
                    } else {
                        "أنشئ رمزاً سرياً للمالك مكوناً من 4 أرقام لحماية لوحة التحكم وإخفاء بيانات المالك عند تحميل التطبيق على جهاز آخر."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it.filter { ch -> ch.isDigit() }.take(4)
                        pinError = null
                    },
                    label = { Text("الرمز السري (4 أرقام)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_pin_unlock_input")
                )
                if (!pinError.isNullOrBlank()) {
                    Text(
                        text = pinError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitPin(pinInput) { msg -> pinError = msg }
                },
                modifier = Modifier.testTag("owner_pin_unlock_confirm")
            ) {
                Text(if (hasExistingPin) "فتح لوحة المالك" else "حفظ الرمز والفتح")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
