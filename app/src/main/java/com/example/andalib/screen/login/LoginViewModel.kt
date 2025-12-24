package com.example.andalib.screen.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.andalib.data.login.LoginRequest
import com.example.andalib.data.login.LoginResponse
import com.example.andalib.data.network.AuthService
import com.example.andalib.data.network.createAuthService
import com.example.andalib.data.TokenManager
import com.example.andalib.service.NotificationPollingService
import com.google.gson.Gson // Import Gson untuk parsing error body
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow // Tambahkan asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException


data class ErrorResponse(val message: String)

class LoginViewModel(
    private val authService: AuthService,
    private val tokenManager: TokenManager,
    private val context: Context
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)

    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        if (_loginState.value is LoginUiState.Loading || email.isBlank() || password.isBlank()) {
            if (email.isBlank() || password.isBlank()) {
                _loginState.value = LoginUiState.Error("Email dan password tidak boleh kosong.")
            }
            return
        }

        _loginState.value = LoginUiState.Loading

        viewModelScope.launch {
            try {

                val response: Response<LoginResponse> = authService.login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    val loginBody = response.body()


                    if (loginBody?.token != null) {
                        tokenManager.saveAuthToken(loginBody.token)


                        loginBody.user?.let { user ->
                            tokenManager.saveAdminInfo(user.id, user.name)
                        }


                        NotificationPollingService.start(context)

                        _loginState.value = LoginUiState.Success(loginBody.message ?: "Login berhasil!")
                    } else {
                        _loginState.value = LoginUiState.Error(loginBody?.message ?: "Token tidak diterima atau respons tidak valid.")
                    }
                } else {
                    val errorMessage = parseHttpError(response)
                    _loginState.value = LoginUiState.Error(errorMessage)
                }
            } catch (e: IOException) {
                _loginState.value = LoginUiState.Error("Koneksi gagal. Periksa koneksi internet dan status server Anda.")
            } catch (e: HttpException) {
                _loginState.value = LoginUiState.Error("Terjadi galat HTTP: ${e.code()}")
            } catch (e: Exception) {
                _loginState.value = LoginUiState.Error("Terjadi galat yang tidak diketahui: ${e.localizedMessage}")
            }
        }
    }

    private fun parseHttpError(response: Response<*>): String {
        return try {
            val errorBodyStr = response.errorBody()?.string()
            if (!errorBodyStr.isNullOrEmpty()) {
                val errorResponse = Gson().fromJson(errorBodyStr, ErrorResponse::class.java)
                errorResponse.message // Mengambil pesan dari JSON {message: "..."}
            } else {
                "Gagal login dengan kode: ${response.code()}"
            }
        } catch (_: Exception) {
            "Gagal login dengan kode: ${response.code()}"
        }
    }

    fun resetState() {
        _loginState.value = LoginUiState.Idle
    }

    sealed class LoginUiState {
        object Idle : LoginUiState()
        object Loading : LoginUiState()
        data class Success(val message: String) : LoginUiState()
        data class Error(val message: String) : LoginUiState()
    }
}


class LoginViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {

            val tokenManager = TokenManager(context.applicationContext)
            val authService = createAuthService(tokenManager)
            return LoginViewModel(authService, tokenManager, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}