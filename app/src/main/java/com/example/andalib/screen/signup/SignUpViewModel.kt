package com.example.andalib.screen.signup


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.andalib.data.signup.SignUpRequest
import com.example.andalib.data.network.AuthService
import com.example.andalib.data.network.createAuthService
import com.example.andalib.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val authService: AuthService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _signUpState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val signUpState: StateFlow<SignUpUiState> = _signUpState

    fun signup(name: String, email: String, password: String) {
        if (_signUpState.value is SignUpUiState.Loading) return

        _signUpState.value = SignUpUiState.Loading

        viewModelScope.launch {
            try {
                val request = SignUpRequest(name, email, password)
                val response = authService.signup(request)

                if (response.success && response.token != null) {
                    tokenManager.saveAuthToken(response.token)
                    // PERUBAHAN: Tambahkan pesan ke state Success
                    _signUpState.value = SignUpUiState.Success(response.message)
                } else {
                    _signUpState.value = SignUpUiState.Error(response.message)
                }
            } catch (e: Exception) {
                // ... (kode error)
            }
        }
    }

    fun resetState() {
        _signUpState.value = SignUpUiState.Idle
    }

    sealed class SignUpUiState {
        object Idle : SignUpUiState()
        object Loading : SignUpUiState()
        data class Success(val message: String) : SignUpUiState()
        data class Error(val message: String) : SignUpUiState()
    }
}

// Factory untuk SignUpViewModel (letakan di bawah kelas utama)
class SignUpViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignUpViewModel::class.java)) {
            val tokenManager = TokenManager(context)
            val authService = createAuthService(tokenManager)
            return SignUpViewModel(authService, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}