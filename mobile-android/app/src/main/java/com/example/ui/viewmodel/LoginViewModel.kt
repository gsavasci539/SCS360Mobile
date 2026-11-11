package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionManager
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.Okul360Repository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * UI State for Login and Authentication flow
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val forgotPasswordSent: Boolean = false,
    val currentUser: User? = null,
    val isAuthenticated: Boolean = false,
    val activeRole: UserRole = UserRole.ADMIN
)

/**
 * LoginViewModel managing Authentication, session storage,
 * and user authentication states.
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val repository = Okul360Repository(application)
    private var firebaseAuth: FirebaseAuth? = null

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("LoginViewModel", "Firebase Auth initialization warning: ${e.message}")
        }
        checkExistingSession()
    }

    /**
     * Check if a valid session is already active in local storage
     */
    fun checkExistingSession() {
        val token = sessionManager.getToken() ?: return
        if (!sessionManager.isLoggedIn()) return
        viewModelScope.launch {
            try {
                val user = repository.restoreSession(token)
                val role = com.example.data.model.SessionRolePolicy.primary(user) ?: return@launch
                sessionManager.saveSession(user, com.example.data.remote.ApiClientProvider.interceptor.authToken)
                _uiState.update { it.copy(isAuthenticated = true, currentUser = user, activeRole = role) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(isAuthenticated = false, currentUser = null, errorMessage = "Oturum sunucuda doğrulanamadı. Yeniden giriş yapın.") } }
        }
    }

    fun onEmailChanged(newEmail: String) {
        _uiState.update { it.copy(email = newEmail, errorMessage = null) }
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.update { it.copy(password = newPassword, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onRememberMeChanged(remember: Boolean) {
        _uiState.update { it.copy(rememberMe = remember) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Sign in using Email & Password via Firebase Auth or institutional credentials
     */
    fun signInWithEmail(
        onSuccess: (User) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password.trim()

        if (email.isBlank() || password.isBlank()) {
            val err = "Lütfen e-posta ve şifrenizi eksiksiz giriniz."
            _uiState.update { it.copy(errorMessage = err) }
            onError(err)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val authenticatedUser = repository.login(email, password)

                if (authenticatedUser == null) {
                    val errorText = "Giriş başarısız. E-posta veya şifre hatalı."
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorText
                        )
                    }
                    onError(errorText)
                    return@launch
                }

                val roleEnum = com.example.data.model.SessionRolePolicy.primary(authenticatedUser)
                    ?: error("Hesabınızın desteklenen etkin rolü bulunmuyor.")

                // Save session in local persistence
                if (_uiState.value.rememberMe) {
                    sessionManager.saveSession(authenticatedUser, com.example.data.remote.ApiClientProvider.interceptor.authToken)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentUser = authenticatedUser,
                        activeRole = roleEnum,
                        errorMessage = null,
                        successMessage = "Giriş başarılı"
                    )
                }

                onSuccess(authenticatedUser)

            } catch (e: Exception) {
                val errorText = "Giriş yapılamadı: ${e.localizedMessage ?: "Sunucu bağlantı hatası oluştu."}"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorText
                    )
                }
                onError(errorText)
            }
        }
    }

    /**
     * Firebase password reset email flow
     */
    fun sendPasswordReset(
        email: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val targetEmail = email.trim()
        if (targetEmail.isBlank()) {
            onComplete(false, "Lütfen geçerli bir e-posta adresi giriniz.")
            return
        }

        viewModelScope.launch {
            val auth = firebaseAuth
            if (auth != null) {
                try {
                    auth.sendPasswordResetEmail(targetEmail).await()
                    onComplete(true, "$targetEmail adresine şifre sıfırlama bağlantısı gönderildi.")
                    return@launch
                } catch (e: Exception) {
                    Log.d("LoginViewModel", "Firebase reset password note: ${e.message}")
                }
            }

            onComplete(true, "$targetEmail adresine şifre sıfırlama bağlantısı iletildi.")
        }
    }

    /**
     * Sign out and clear stored session
     */
    fun signOut(onSignedOut: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                firebaseAuth?.signOut()
            } catch (e: Exception) {
                Log.w("LoginViewModel", "Sign out error: ${e.message}")
            }
            sessionManager.clearSession()
            _uiState.update {
                LoginUiState(
                    email = "",
                    password = "",
                    isAuthenticated = false,
                    currentUser = null
                )
            }
            onSignedOut()
        }
    }

}
