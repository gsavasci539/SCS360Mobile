package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SessionManager
import com.example.data.model.UserRole
import com.example.ui.components.Okul360CircularLoadingIndicator
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    loginError: String? = null,
    isLoading: Boolean = false,
    activePlans: List<com.example.data.model.SubscriptionPlan> = emptyList(),
    onLoginSuccess: (String, String) -> Unit,
    onForgotPassword: (String) -> Unit,
    onRegisterNewTenant: (
        orgName: String,
        taxNo: String,
        ownerName: String,
        ownerEmail: String,
        ownerPhone: String,
        password: String,
        planId: String,
        billingPeriod: String
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onGoogleSignIn: () -> Unit = {}
) {
    val context = LocalContext.current
    var rememberedEmail by remember { mutableStateOf(SessionManager(context).getUser()?.email ?: "") }
    var email by remember { mutableStateOf(rememberedEmail) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showPricingCatalogDialog by remember { mutableStateOf(false) }
    var showTenantSignupDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Official Logo
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(102.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF6F9F6),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.scs360_logo),
                        contentDescription = "SCS360 Resmi Logo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "DAHA AKILLI OKULLAR, DAHA GÜÇLÜ GELECEK",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                        color = Color(0xFF163B3B).copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Kurum, Şube & Öğrenci Yönetim Platformu",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Card Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "🔐 Güvenli Giriş",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )

                    // Error Banner if present
                    val activeError = loginError ?: validationError
                    if (activeError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Text(
                                    text = activeError,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it 
                            validationError = null
                        },
                        label = { Text("Kurumsal E-posta") },
                        placeholder = { Text("ornek@scs360.com") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, contentDescription = null, tint = PrimaryBlue)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it 
                            validationError = null
                        },
                        label = { Text("Şifre") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = PrimaryBlue)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Şifreyi Gizle" else "Şifreyi Göster",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (email.isBlank() || password.isBlank()) {
                                    validationError = "Lütfen e-posta ve şifrenizi giriniz."
                                } else {
                                    onLoginSuccess(email, password)
                                }
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Remember Me & Forgot Password Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                            )
                            Text(
                                text = "Beni Hatırla",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(
                            onClick = {
                                forgotEmail = email
                                showForgotPasswordDialog = true
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Şifremi Unuttum?",
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Standard Login Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onLoginSuccess(email, password)
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (isLoading) {
                            Okul360CircularLoadingIndicator(
                                size = 22.dp,
                                strokeWidth = 2.5.dp,
                                primaryColor = Color.White,
                                secondaryColor = Color(0xFF93C5FD)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Giriş Yapılıyor...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Giriş Yap",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "🔒 SCS360 kurumsal sunucu ve veritabanı altyapısı ile güvenli oturum açma.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Public Marketing / Pricing Catalog & Self-Service Tenant Signup Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showPricingCatalogDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paket & Fiyatlar", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                FilledTonalButton(
                    onClick = { showTenantSignupDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DomainAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yeni Kurum Kaydı", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tenant & Security Info Badge
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Multi-Tenant Güvenli Kurumsal Altyapı",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Text(
                    text = "Şifremi Unuttum",
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Kayıtlı e-posta adresinizi giriniz. Şifre sıfırlama talimatı anında iletilecektir.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = { Text("E-posta Adresi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotEmail.isNotBlank()) {
                            onForgotPassword(forgotEmail)
                            showForgotPasswordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Sıfırlama Bağlantısı Gönder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    if (showPricingCatalogDialog) {
        val visiblePlans = activePlans.filter { it.is_active }
        AlertDialog(
            onDismissRequest = { showPricingCatalogDialog = false },
            title = {
                Column {
                    Text("SCS360 Paket ve Fiyatlandırma (/fiyatlandirma)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Çoklu şube, öğrenci takibi, finans, revir ve kapı güvenliği", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (visiblePlans.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Paketler hazırlanıyor. Lütfen daha sonra tekrar kontrol edin veya kurumsal satış ekibiyle iletişime geçin.",
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        visiblePlans.forEach { plan ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy, modifier = Modifier.weight(1f))
                                        Text(
                                            if (plan.price <= 0.0) "Ücretsiz" else "₺${plan.price.toInt()} / ${if (plan.billing_period == "YEARLY") "Yıl" else "Ay"}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.5.sp,
                                            color = PrimaryBlue
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(plan.description, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Kota: ${plan.branch_limit} Şube • ${plan.student_limit} Öğrenci • ${plan.user_limit} Kullanıcı",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (visiblePlans.isNotEmpty()) {
                    Button(
                        onClick = {
                            showPricingCatalogDialog = false
                            showTenantSignupDialog = true
                        }
                    ) {
                        Text("Kurum Kaydına Geç")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPricingCatalogDialog = false }) { Text("Kapat") }
            }
        )
    }

    if (showTenantSignupDialog) {
        val visiblePlans = activePlans.filter { it.is_active }
        var orgName by remember { mutableStateOf("") }
        var taxNo by remember { mutableStateOf("") }
        var ownerName by remember { mutableStateOf("") }
        var ownerEmail by remember { mutableStateOf("") }
        var ownerPhone by remember { mutableStateOf("+90 532 ") }
        var signupPassword by remember { mutableStateOf("Scs360!2026") }
        var selectedPlanId by remember { mutableStateOf(visiblePlans.firstOrNull()?.id ?: "plan-1") }
        var billingPeriod by remember { mutableStateOf("MONTHLY") }

        AlertDialog(
            onDismissRequest = { showTenantSignupDialog = false },
            title = {
                Column {
                    Text("Yeni Kurum Kaydı (/kayit)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Kurum + Kurucu (Owner) + 11 Standart Rol + İlk Üyelik", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (visiblePlans.isEmpty()) {
                        Text("Paketler hazırlanıyor. Aktif paket olmadan yeni kurum kaydı açılamaz.", color = MaterialTheme.colorScheme.error, fontSize = 12.5.sp)
                    } else {
                        OutlinedTextField(value = orgName, onValueChange = { orgName = it }, label = { Text("Kurum / Okul Resmi Unvanı") }, singleLine = true)
                        OutlinedTextField(value = taxNo, onValueChange = { taxNo = it }, label = { Text("Vergi Numarası") }, singleLine = true)
                        OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Kurum Sahibi Ad Soyad") }, singleLine = true)
                        OutlinedTextField(value = ownerEmail, onValueChange = { ownerEmail = it }, label = { Text("Kurucu E-Posta Adresi") }, singleLine = true)
                        OutlinedTextField(value = ownerPhone, onValueChange = { ownerPhone = it }, label = { Text("Kurucu Telefon") }, singleLine = true)
                        OutlinedTextField(value = signupPassword, onValueChange = { signupPassword = it }, label = { Text("Giriş Şifresi") }, singleLine = true)
                        Text("Paket Seçimi:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        visiblePlans.forEach { p ->
                            FilterChip(
                                selected = selectedPlanId == p.id,
                                onClick = { selectedPlanId = p.id },
                                label = { Text("${p.name} (₺${p.price.toInt()})") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = billingPeriod == "MONTHLY", onClick = { billingPeriod = "MONTHLY" }, label = { Text("Aylık") })
                            FilterChip(selected = billingPeriod == "YEARLY", onClick = { billingPeriod = "YEARLY" }, label = { Text("Yıllık") })
                        }
                    }
                }
            },
            confirmButton = {
                if (visiblePlans.isNotEmpty()) {
                    Button(
                        onClick = {
                            if (orgName.isNotBlank() && ownerEmail.isNotBlank() && ownerName.isNotBlank()) {
                                onRegisterNewTenant(
                                    orgName.trim(),
                                    taxNo.trim(),
                                    ownerName.trim(),
                                    ownerEmail.trim(),
                                    ownerPhone.trim(),
                                    signupPassword,
                                    selectedPlanId,
                                    billingPeriod
                                )
                                showTenantSignupDialog = false
                            }
                        }
                    ) {
                        Text("Kurumu Oluştur & Başla")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTenantSignupDialog = false }) { Text("İptal") }
            }
        )
    }
}

/**
 * Stateful overload for LoginScreen integrating LoginViewModel with Firebase Auth
 * and session state management.
 */
@Composable
fun LoginScreen(
    viewModel: com.example.ui.viewmodel.LoginViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LoginScreen(
        loginError = state.errorMessage,
        isLoading = state.isLoading,
        onLoginSuccess = { email, password ->
            viewModel.onEmailChanged(email)
            viewModel.onPasswordChanged(password)
            viewModel.signInWithEmail(
                onSuccess = { onLoginSuccess() }
            )
        },
        onForgotPassword = { email ->
            viewModel.sendPasswordReset(email) { _, _ -> }
        }
    )
}


