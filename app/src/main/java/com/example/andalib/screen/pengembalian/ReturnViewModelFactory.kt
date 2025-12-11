package com.example.andalib.screen.pengembalian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.andalib.data.network.ApiService

/**
 * Factory kustom untuk menginstansiasi ReturnViewModel dengan ApiService sebagai dependency.
 * Sudah TIDAK lagi menggunakan data dummy / MOCK.
 */
class ReturnViewModelFactory(
    private val apiService: ApiService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReturnViewModel::class.java)) {
            return ReturnViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
