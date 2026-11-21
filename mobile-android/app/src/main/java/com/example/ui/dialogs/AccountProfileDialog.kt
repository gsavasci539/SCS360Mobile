package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

internal fun accountError(failure: Exception, fallback: String): String = when (failure) {
    is HttpException -> when (failure.code()) {
        401 -> "Oturumunuzu yeniden açın."
        402 -> "Üyelik ve ödeme ekranını kontrol edin."
        429 -> "Çok fazla deneme yapıldı. Bir süre sonra yeniden deneyin."
        409, 422 -> runCatching {
            JSONObject(failure.response()?.errorBody()?.string().orEmpty()).getString("message")
        }.getOrDefault(fallback)
        else -> fallback
    }
    is IllegalStateException -> failure.message ?: fallback
    else -> fallback
}

@Composable
fun AccountProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onChangePassword: () -> Unit,
    onSave: suspend (firstName: String, lastName: String, email: String, phone: String, currentPassword: String?) -> Unit,
) {
    var firstName by remember(user.id) { mutableStateOf(user.first_name.orEmpty()) }
    var lastName by remember(user.id) { mutableStateOf(user.last_name.orEmpty()) }
    var email by remember(user.id) { mutableStateOf(user.email) }
    var phone by remember(user.id) { mutableStateOf(user.phone.orEmpty()) }
    var password by remember(user.id) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val emailChanged = !user.email.equals(email.trim(), ignoreCase = true)

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Profil bilgileri") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(firstName, { firstName = it }, label = { Text("Ad") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy)
                OutlinedTextField(lastName, { lastName = it }, label = { Text("Soyad") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy)
                OutlinedTextField(email, { email = it }, label = { Text("E-posta") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                OutlinedTextField(phone, { phone = it }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                if (emailChanged) OutlinedTextField(password, { password = it }, label = { Text("Mevcut şifre") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = PasswordVisualTransformation())
                TextButton(onClick = onChangePassword, enabled = !busy) { Text("Şifre değiştir") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() && (!emailChanged || password.isNotEmpty()),
                onClick = {
                    if (!busy) {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                onSave(firstName, lastName, email, phone, password.takeIf { emailChanged })
                                onDismiss()
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                error = accountError(failure, "Profil kaydedilemedi. Bilgilerinizi kontrol edip tekrar deneyin.")
                            } finally {
                                busy = false
                            }
                        }
                    }
                }
            ) { Text(if (busy) "Kaydediliyor…" else "Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Vazgeç") } },
    )
}
