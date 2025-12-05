package com.example.andalib.screen.Borrowing

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.andalib.Borrowing
import com.example.andalib.data.TokenManager
import com.example.andalib.data.network.BorrowingApi
import com.example.andalib.data.network.createBorrowingService
import kotlinx.coroutines.launch
import retrofit2.Response

class BorrowingViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val api: BorrowingApi = createBorrowingService(tokenManager)

    // State untuk UI (VM ERROR MESSAGE)
    var borrowings by mutableStateOf(emptyList<Borrowing>())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadBorrowings()
    }

    // --- READ (GET All) ---
    fun loadBorrowings() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = api.getAllBorrowings()
                if (response.isSuccessful && response.body() != null) {
                    borrowings = response.body()!!
                } else {
                    errorMessage = "Gagal memuat data: ${response.code()} ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "Koneksi Gagal: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // --- SEARCH ---
    fun searchBorrowings(query: String) {
        if (query.isEmpty()) {
            loadBorrowings()
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = api.searchBorrowings(query)
                if (response.isSuccessful && response.body() != null) {
                    borrowings = response.body()!!
                } else {
                    errorMessage = "Pencarian gagal: ${response.code()} ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "Koneksi Gagal saat mencari: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // --- CREATE (POST) ---
    suspend fun createBorrowing(newBorrowing: Borrowing): Boolean {
        return try {
            val response = api.createBorrowing(newBorrowing)
            if (response.isSuccessful && response.body() != null) {
                loadBorrowings()
                true
            } else {
                errorMessage = "Gagal menambah: ${response.errorBody()?.string() ?: response.message()}"
                false
            }
        } catch (e: Exception) {
            errorMessage = "Koneksi Gagal saat menambah: ${e.message}"
            false
        }
    }

    // --- UPDATE (PUT) ---
    suspend fun updateBorrowing(borrowing: Borrowing): Boolean {
        if (borrowing.id == 0) return false

        return try {
            val response = api.updateBorrowing(borrowing.id, borrowing)
            if (response.isSuccessful && response.body() != null) {
                loadBorrowings()
                true
            } else {
                errorMessage = "Gagal update: ${response.errorBody()?.string() ?: response.message()}"
                false
            }
        } catch (e: Exception) {
            errorMessage = "Koneksi Gagal saat update: ${e.message}"
            false
        }
    }

    // --- DELETE ---
    suspend fun deleteBorrowing(id: Int): Boolean {
        return try {
            val response = api.deleteBorrowing(id)
            if (response.isSuccessful) {
                loadBorrowings()
                true
            } else {
                errorMessage = "Gagal hapus: ${response.errorBody()?.string() ?: response.message()}"
                false
            }
        } catch (e: Exception) {
            errorMessage = "Koneksi Gagal saat hapus: ${e.message}"
            false
        }
    }

    // FIX 1: Tambahkan fungsi publik clearErrorMessage()
    fun clearErrorMessage() {
        errorMessage = null
    }
}