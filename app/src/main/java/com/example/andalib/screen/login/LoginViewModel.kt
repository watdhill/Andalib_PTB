package com.example.andalib.screen.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider // PENTING untuk ViewModelProvider.Factory
import androidx.lifecycle.viewModelScope
import com.example.andalib.data.login.LoginRequest
import com.example.andalib.data.network.AuthService
import com.example.andalib.data.network.createAuthService // PENTING untuk Factory
import com.example.andalib.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authService: AuthService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState

    fun login(email: String, password: String) {
        if (_loginState.value is LoginUiState.Loading || email.isBlank() || password.isBlank()) return

        _loginState.value = LoginUiState.Loading

        viewModelScope.launch {
            try {
                val response = authService.login(LoginRequest(email, password))

                if (response.success && response.token != null) {
                    tokenManager.saveAuthToken(response.token)
                    _loginState.value = LoginUiState.Success
                } else {
                    _loginState.value = LoginUiState.Error(response.message)
                }
            } catch (e: Exception) {
                _loginState.value = LoginUiState.Error("Koneksi gagal: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginUiState.Idle
    }

    sealed class LoginUiState {
        object Idle : LoginUiState()
        object Loading : LoginUiState()
        object Success : LoginUiState()
        data class Error(val message: String) : LoginUiState()
    }
}

// Factory untuk membuat ViewModel (tanpa Hilt/Koin)
class LoginViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            val tokenManager = TokenManager(context)
            val authService = createAuthService(tokenManager)
            return LoginViewModel(authService, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}