package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.VerifiedBlue
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

data class PendingPhoneRegistration(
    val countryCode: String,
    val phoneNumber: String,
    val countryCode2: String = "+966",
    val phoneNumber2: String = "",
    val countryCode3: String = "+966",
    val phoneNumber3: String = "",
    val displayName: String,
    val role: String = "customer"
)

object PendingRegistrationStore {
    private const val PREFS = "matjar_pending_reg"
    private const val KEY_PHONE = "phone"
    private const val KEY_CODE = "code"
    private const val KEY_PHONE2 = "phone2"
    private const val KEY_CODE2 = "code2"
    private const val KEY_PHONE3 = "phone3"
    private const val KEY_CODE3 = "code3"
    private const val KEY_NAME = "name"
    private const val KEY_ROLE = "role"

    fun save(context: Context, pending: PendingPhoneRegistration) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PHONE, pending.phoneNumber)
            .putString(KEY_CODE, pending.countryCode)
            .putString(KEY_PHONE2, pending.phoneNumber2)
            .putString(KEY_CODE2, pending.countryCode2)
            .putString(KEY_PHONE3, pending.phoneNumber3)
            .putString(KEY_CODE3, pending.countryCode3)
            .putString(KEY_NAME, pending.displayName)
            .putString(KEY_ROLE, pending.role)
            .apply()
    }

    fun consume(context: Context): PendingPhoneRegistration? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val phone = prefs.getString(KEY_PHONE, null) ?: return null
        val code = prefs.getString(KEY_CODE, "+966") ?: "+966"
        val phone2 = prefs.getString(KEY_PHONE2, "") ?: ""
        val code2 = prefs.getString(KEY_CODE2, "+966") ?: "+966"
        val phone3 = prefs.getString(KEY_PHONE3, "") ?: ""
        val code3 = prefs.getString(KEY_CODE3, "+966") ?: "+966"
        val name = prefs.getString(KEY_NAME, "") ?: ""
        val role = prefs.getString(KEY_ROLE, "customer") ?: "customer"
        prefs.edit().clear().apply()
        return PendingPhoneRegistration(code, phone, code2, phone2, code3, phone3, name, role)
    }
}

val CountryDialOptions = listOf(
    "+966" to "السعودية (+966)",
    "+971" to "الإمارات (+971)",
    "+965" to "الكويت (+965)",
    "+974" to "قطر (+974)",
    "+973" to "البحرين (+973)",
    "+968" to "عمان (+968)",
    "+20" to "مصر (+20)",
    "+962" to "الأردن (+962)"
)

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("تعذر العثور على إعدادات التوثيق")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context.findActivity()
                ?: throw IllegalStateException("يتطلب تسجيل الدخول Activity ساري المفعول")
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("نوع بيانات اعتماد غير متوقع")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "فشل التحقق من الحساب")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onComplete()
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var selectedCountry by remember { mutableStateOf(CountryDialOptions.first()) }
    var countryDropdownExpanded by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var showExtraNumbers by remember { mutableStateOf(false) }
    var phoneNumber2 by remember { mutableStateOf("") }
    var phoneNumber3 by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Firebase Phone Auth OTP state
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var otpCode by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    fun savePendingPhoneIfEntered() {
        val cleanLocal = phoneNumber.filter { it.isDigit() }.trimStart('0')
        if (cleanLocal.length >= 7) {
            PendingRegistrationStore.save(
                context,
                PendingPhoneRegistration(
                    countryCode = selectedCountry.first,
                    phoneNumber = cleanLocal,
                    countryCode2 = selectedCountry.first,
                    phoneNumber2 = phoneNumber2.filter { it.isDigit() }.trimStart('0'),
                    countryCode3 = selectedCountry.first,
                    phoneNumber3 = phoneNumber3.filter { it.isDigit() }.trimStart('0'),
                    displayName = displayName.trim().ifBlank { "عميل ${cleanLocal.takeLast(4)}" },
                    role = "customer"
                )
            )
        }
    }

    fun launchGoogleAuth() {
        savePendingPhoneIfEntered()
        errorMessage = null
        infoMessage = null
        isLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = { isLoading = false }
        )
    }

    fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        scope.launch {
            try {
                Firebase.auth.signInWithCredential(credential).await()
                isLoading = false
                onAuthSuccess()
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                isLoading = false
                errorMessage = "رمز التحقق غير صحيح أو منتهي الصلاحية. تأكد من الأرقام الستة وأعد المحاولة."
            } catch (e: Exception) {
                isLoading = false
                Log.e("Auth", "Phone signInWithCredential failed", e)
                errorMessage = e.localizedMessage ?: "تعذر إتمام التحقق برقم الجوال"
            }
        }
    }

    fun startPhoneVerification(forceResend: Boolean = false) {
        val activity = context.findActivity()
        if (activity == null) {
            errorMessage = "تعذر الوصول إلى نافذة التطبيق لبدء التحقق عبر الهاتف"
            return
        }
        val cleanLocal = phoneNumber.filter { it.isDigit() }.trimStart('0')
        if (cleanLocal.length < 7) {
            errorMessage = "يرجى إدخال رقم جوال صحيح (7 أرقام على الأقل)"
            return
        }
        val e164Phone = "${selectedCountry.first}$cleanLocal"

        errorMessage = null
        infoMessage = null
        isLoading = true

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isLoading = false
                Log.w("Auth", "Phone verification unavailable for $e164Phone: ${e.message}")
                val raw = e.localizedMessage.orEmpty()
                val isConfigOrRecaptchaIssue = listOf(
                    "operation-not-allowed",
                    "billing",
                    "enabled",
                    "recaptcha",
                    "sitekey",
                    "integrity",
                    "authorized",
                    "17499",
                    "internal"
                ).any { raw.contains(it, ignoreCase = true) }

                errorMessage = if (isConfigOrRecaptchaIssue) {
                    "التحقق عبر رسائل SMS غير مفعّل أو يتطلب إعداد reCAPTCHA Enterprise و Phone Auth في لوحة تحكم Firebase للمشروع. اضغط الزر أدناه لتوثيق رقمك والدخول فوراً عبر Google."
                } else {
                    "تعذر إرسال رمز SMS: ${raw.ifBlank { "تحقق من اتصال الشبكة أو استخدم الدخول السريع عبر Google" }}"
                }
            }

            override fun onCodeSent(
                verId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                isLoading = false
                verificationId = verId
                resendToken = token
                infoMessage = "تم إرسال رمز التحقق المكون من 6 أرقام إلى $e164Phone"
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(e164Phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (forceResend && resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken!!)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF061016),
                        Color(0xFF0B141A),
                        Color(0xFF07231D)
                    )
                )
            )
            .safeDrawingPadding()
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // WhatsApp-style Store Welcome Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_store_banner_1794675291139),
                    contentDescription = "واجهة متجر واتس الذكي",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = Color(0xFF041C15),
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "متجر واتس",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "موثق",
                                tint = VerifiedBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "المتجر الرسمي • دخول برقم الجوال مثل الواتساب",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            // WhatsApp Phone Login Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (verificationId == null) {
                        Text(
                            text = "أدخل رقم هاتفك",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "التسجيل والدخول بنفس نظام الواتساب برقم الجوال عبر رمز تحقق SMS أو حساب Google.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        // Country Picker Row (WhatsApp style)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { countryDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedCountry.second,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = "اختر الدولة",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = countryDropdownExpanded,
                                onDismissRequest = { countryDropdownExpanded = false }
                            ) {
                                CountryDialOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.second) },
                                        onClick = {
                                            selectedCountry = option
                                            countryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Split Country Code + Phone Number Field (WhatsApp style)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = selectedCountry.first,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الرمز") },
                                singleLine = true,
                                modifier = Modifier.width(96.dp),
                                shape = RoundedCornerShape(14.dp)
                            )
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() }.take(15) },
                                label = { Text("رقم الجوال (5xxxxxxxx)") },
                                leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("whatsapp_phone_input"),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("الاسم أو اسم النشاط") },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("whatsapp_name_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Expandable 3-Phone-Numbers Support (WhatsApp Business style)
                        TextButton(
                            onClick = { showExtraNumbers = !showExtraNumbers },
                            modifier = Modifier.testTag("toggle_three_numbers_button")
                        ) {
                            Text(
                                if (showExtraNumbers) "إخفاء الأرقام الإضافية (حتى 3 أرقام)"
                                else "+ إضافة حتى 3 أرقام مثل واتساب أعمال (اختياري)"
                            )
                        }

                        if (showExtraNumbers) {
                            OutlinedTextField(
                                value = phoneNumber2,
                                onValueChange = { phoneNumber2 = it.filter { ch -> ch.isDigit() }.take(15) },
                                label = { Text("الرقم الثاني — مبيعات (${selectedCountry.first})") },
                                leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("whatsapp_phone2_input"),
                                shape = RoundedCornerShape(14.dp)
                            )
                            OutlinedTextField(
                                value = phoneNumber3,
                                onValueChange = { phoneNumber3 = it.filter { ch -> ch.isDigit() }.take(15) },
                                label = { Text("الرقم الثالث — دعم/فرع (${selectedCountry.first})") },
                                leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("whatsapp_phone3_input"),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    } else {
                        // Stage B: 6-digit SMS OTP verification
                        Icon(
                            imageVector = Icons.Rounded.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "التحقق من ${selectedCountry.first} $phoneNumber",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "أدخل رمز التحقق المكون من 6 أرقام المرسل عبر رسالة SMS إلى رقم جوالك.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it.filter { ch -> ch.isDigit() }.take(6) },
                            label = { Text("رمز التحقق (6 أرقام)") },
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("whatsapp_otp_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Button(
                            onClick = {
                                val verId = verificationId
                                if (verId.isNullOrBlank() || otpCode.length < 6) {
                                    errorMessage = "يرجى إدخال رمز التحقق المكون من 6 أرقام"
                                } else {
                                    errorMessage = null
                                    isLoading = true
                                    val cred = PhoneAuthProvider.getCredential(verId, otpCode)
                                    signInWithPhoneCredential(cred)
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("whatsapp_verify_otp_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("تحقق وتسجيل الدخول", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { startPhoneVerification(forceResend = true) },
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إعادة إرسال الرمز")
                            }
                            TextButton(
                                onClick = {
                                    verificationId = null
                                    otpCode = ""
                                    infoMessage = null
                                    errorMessage = null
                                },
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("تغيير الرقم")
                            }
                        }
                    }

                    if (!infoMessage.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = infoMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                if (verificationId == null) {
                                    Button(
                                        onClick = { launchGoogleAuth() },
                                        enabled = !isLoading,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("توثيق الرقم والدخول فوراً عبر Google")
                                    }
                                }
                            }
                        }
                    }

                    if (verificationId == null) {
                        Button(
                            onClick = { launchGoogleAuth() },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "توثيق الرقم والدخول عبر Google",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                        OutlinedButton(
                            onClick = {
                                if (phoneNumber.filter { it.isDigit() }.trimStart('0').length < 7) {
                                    errorMessage = "يرجى إدخال رقم جوال صحيح (7 أرقام على الأقل) لإرسال SMS"
                                } else {
                                    errorMessage = null
                                    showConfirmDialog = true
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("whatsapp_next_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sms,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "أو إرسال رمز التحقق (SMS)",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }

    // Classic WhatsApp-style Phone Number Confirmation Modal before Firebase PhoneAuth SMS
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "تأكيد رقم الجوال",
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "سنقوم بإرسال رمز تحقق SMS لتوثيق رقمك:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${selectedCountry.first} ${phoneNumber.trimStart('0')}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "هل الرقم صحيح؟ اضغط إرسال رمز SMS للمتابعة بنظام الواتساب.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        PendingRegistrationStore.save(
                            context,
                            PendingPhoneRegistration(
                                countryCode = selectedCountry.first,
                                phoneNumber = phoneNumber.trimStart('0'),
                                countryCode2 = selectedCountry.first,
                                phoneNumber2 = phoneNumber2.trimStart('0'),
                                countryCode3 = selectedCountry.first,
                                phoneNumber3 = phoneNumber3.trimStart('0'),
                                displayName = displayName.trim().ifBlank { "عميل ${phoneNumber.takeLast(4)}" },
                                role = "customer"
                            )
                        )
                        startPhoneVerification(forceResend = false)
                    },
                    modifier = Modifier.testTag("confirm_phone_dialog_button")
                ) {
                    Text("إرسال رمز SMS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("تعديل الرقم")
                }
            }
        )
    }
}

@Composable
fun PhoneRegistrationScreen(
    initialName: String,
    initialPhone: String = "",
    monthlyPrice: Double,
    currency: String,
    onRegisterSubmit: (
        phone: String,
        countryCode: String,
        displayName: String,
        role: String,
        phone2: String,
        countryCode2: String,
        phone3: String,
        countryCode3: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val matchedCountry = remember(initialPhone) {
        CountryDialOptions.firstOrNull { initialPhone.startsWith(it.first) } ?: CountryDialOptions.first()
    }
    val strippedLocalPhone = remember(initialPhone, matchedCountry) {
        if (initialPhone.startsWith(matchedCountry.first)) {
            initialPhone.removePrefix(matchedCountry.first).filter { it.isDigit() }
        } else {
            initialPhone.filter { it.isDigit() }
        }
    }
    var selectedCountry by remember { mutableStateOf(matchedCountry) }
    var countryDropdownExpanded by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf(strippedLocalPhone) }
    var phoneNumber2 by remember { mutableStateOf("") }
    var phoneNumber3 by remember { mutableStateOf("") }
    var showExtraNumbers by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf(initialName.ifBlank { "عميل المتجر" }) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Text(
                    text = "إكمال أرقام الجوال (واتساب أعمال)",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "أدخل رقم جوالك (ويمكنك إضافة حتى 3 أرقام مثل واتساب أعمال) للدخول المباشر إلى المتجر.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { countryDropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedCountry.second, style = MaterialTheme.typography.bodyLarge)
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = countryDropdownExpanded,
                        onDismissRequest = { countryDropdownExpanded = false }
                    ) {
                        CountryDialOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.second) },
                                onClick = {
                                    selectedCountry = option
                                    countryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("الاسم") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_name_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedCountry.first,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الرمز") },
                        singleLine = true,
                        modifier = Modifier.width(96.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() }.take(15) },
                        label = { Text("الرقم الأساسي (5xxxxxxxx)") },
                        leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("register_phone_input"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                TextButton(onClick = { showExtraNumbers = !showExtraNumbers }) {
                    Text(
                        if (showExtraNumbers) "إخفاء الرقم الثاني والثالث"
                        else "+ إضافة رقم ثاني وثالث (حتى 3 أرقام مثل واتساب أعمال)"
                    )
                }

                if (showExtraNumbers) {
                    OutlinedTextField(
                        value = phoneNumber2,
                        onValueChange = { phoneNumber2 = it.filter { ch -> ch.isDigit() }.take(15) },
                        label = { Text("الرقم الثاني (${selectedCountry.first})") },
                        leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = phoneNumber3,
                        onValueChange = { phoneNumber3 = it.filter { ch -> ch.isDigit() }.take(15) },
                        label = { Text("الرقم الثالث (${selectedCountry.first})") },
                        leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "الاشتراك الشهري المحدد من المالك: ${monthlyPrice.toInt()} $currency / شهرياً",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                if (!validationError.isNullOrBlank()) {
                    Text(
                        text = validationError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        when {
                            displayName.trim().length < 2 ->
                                validationError = "يرجى كتابة اسم صالح"
                            phoneNumber.trim().length < 7 ->
                                validationError = "يرجى إدخال رقم جوال صحيح (7 أرقام على الأقل)"
                            else -> {
                                validationError = null
                                onRegisterSubmit(
                                    phoneNumber.trimStart('0'),
                                    selectedCountry.first,
                                    displayName.trim(),
                                    "customer",
                                    phoneNumber2.trimStart('0'),
                                    selectedCountry.first,
                                    phoneNumber3.trimStart('0'),
                                    selectedCountry.first
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("register_submit_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("تأكيد الأرقام والدخول للمتجر", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
