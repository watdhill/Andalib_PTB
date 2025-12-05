package com.example.andalib.screen.signup

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.andalib.data.TokenManager
import com.example.andalib.data.network.AuthService
import com.example.andalib.data.network.createAuthService
import com.example.andalib.data.signup.SignUpRequest
import com.example.andalib.data.signup.SignUpResponse // Pastikan import ini
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response // Import ini PENTING
import java.io.IOException

// Data class kecil untuk parsing error
data class ErrorResponse(val message: String)

class SignUpViewModel(
    private val authService: AuthService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _signUpState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val signUpState: StateFlow<SignUpUiState> = _signUpState.asStateFlow()

    fun signup(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _signUpState.value = SignUpUiState.Error("Data tidak boleh kosong.")
            return
        }

        if (_signUpState.value is SignUpUiState.Loading) return
        _signUpState.value = SignUpUiState.Loading

        viewModelScope.launch {
            try {
                val request = SignUpRequest(name, email, password)

                // 1. Panggil API. Karena return-nya Response<SignUpResponse>, tidak akan throw HttpException untuk error 4xx/5xx
                val response: Response<SignUpResponse> = authService.signup(request)

                // 2. Cek apakah HTTP Status Code 200-299
                if (response.isSuccessful) {
                    // 3. Ambil body (isinya SignUpResponse yang asli)
                    val body = response.body()

                    if (body != null && body.token != null) {
                        // SUKSES: Simpan token
                        tokenManager.saveAuthToken(body.token)
                        _signUpState.value = SignUpUiState.Success(body.message)
                    } else {
                        // Body null atau token tidak ada
                        _signUpState.value = SignUpUiState.Error(body?.message ?: "Gagal: Respons server kosong.")
                    }
                } else {
                    // 4. Handle Error (400, 401, 409, 500)
                    val errorMsg = parseErrorBody(response)
                    _signUpState.value = SignUpUiState.Error(errorMsg)
                }

            } catch (e: IOException) {
                _signUpState.value = SignUpUiState.Error("Koneksi internet bermasalah.")
            } catch (e: Exception) {
                Log.e("SignUpVM", "Error", e)
                _signUpState.value = SignUpUiState.Error("Terjadi kesalahan: ${e.localizedMessage}")
            }
        }
    }

    // Fungsi helper untuk membaca error body dari Retrofit Response
    private fun parseErrorBody(response: Response<*>): String {
        return try {
            val errorBodyStr = response.errorBody()?.string()
            if (!errorBodyStr.isNullOrEmpty()) {
                val errorResponse = Gson().fromJson(errorBodyStr, ErrorResponse::class.java)
                errorResponse.message
            } else {
                "Gagal dengan kode: ${response.code()}"
            }
        } catch (e: Exception) {
            "Gagal dengan kode: ${response.code()}"
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

// Factory (Tidak berubah, copy saja jika perlu)
class SignUpViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignUpViewModel::class.java)) {
            val tokenManager = TokenManager(context.applicationContext)
            val authService = createAuthService(tokenManager)
            return SignUpViewModel(authService, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}