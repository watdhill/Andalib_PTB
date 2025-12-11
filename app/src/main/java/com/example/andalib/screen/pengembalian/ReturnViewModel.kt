package com.example.andalib.screen.pengembalian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Impor yang diperlukan, termasuk ApiService yang baru
import com.example.andalib.data.network.ApiService
import com.example.andalib.data.network.ReturnRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// =========================================================
// 1. UI MODEL DEFINITIONS (Tidak ada perubahan di sini)
// =========================================================

data class AnggotaUI(val nim: String, val nama: String, val email: String?)

data class PeminjamanUI(
    val id: Int,
    val judulBuku: String,
    val author: String,
    val tglPinjam: String,
    val jatuhTempo: String
)

// =========================================================
// 2. VIEWMODEL IMPLEMENTATION (Dengan Perbaikan)
// =========================================================

class ReturnViewModel(
    // === PERBAIKAN UTAMA ADA DI SINI ===
    private val apiService: ApiService // Tipe diubah dari String ke ApiService
) : ViewModel() {

    // ---------------------- STATE FLOWS ----------------------
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _memberSearchResults = MutableStateFlow<List<AnggotaUI>>(emptyList())
    val memberSearchResults: StateFlow<List<AnggotaUI>> = _memberSearchResults.asStateFlow()

    private val _selectedMember = MutableStateFlow<AnggotaUI?>(null)
    val selectedMember: StateFlow<AnggotaUI?> = _selectedMember.asStateFlow()

    private val _activeLoans = MutableStateFlow<List<PeminjamanUI>>(emptyList())
    val activeLoans: StateFlow<List<PeminjamanUI>> = _activeLoans.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _transactionStatus = MutableStateFlow<Boolean?>(null)
    val transactionStatus: StateFlow<Boolean?> = _transactionStatus.asStateFlow()

    // ---------------------- INITIALIZATION ----------------------

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(500L)
                .collect { query ->
                    if (query.length >= 1) {
                        searchMembers(query)
                    } else if (query.isEmpty() && _memberSearchResults.value.isNotEmpty()) {
                        _memberSearchResults.value = emptyList()
                    }
                }
        }
    }

    // ---------------------- PUBLIC METHODS ----------------------

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedMember(member: AnggotaUI?) {
        _selectedMember.value = member
        _memberSearchResults.value = emptyList()
        _searchQuery.value = member?.let { "${it.nama} (${it.nim})" } ?: ""

        if (member != null) {
            fetchActiveLoans(member.nim)
        } else {
            _activeLoans.value = emptyList()
        }
    }

    fun submitReturn(
        peminjamanId: Int,
        returnDate: String,
        fineAmount: Int,
        description: String,
        proofUriString: String?
    ) {
        viewModelScope.launch {
            _isLoading.update { true }
            _transactionStatus.value = null
            try {
                val request = ReturnRequest(
                    peminjamanId = peminjamanId,
                    tanggalPengembalian = returnDate,
                    denda = fineAmount,
                    keterangan = description.takeIf { it.isNotBlank() },
                    buktiKerusakanUrl = proofUriString
                )
                // Sekarang pemanggilan ini valid
                val response = apiService.submitReturn(request)
                _transactionStatus.value = response.success
            } catch (e: Exception) {
                _transactionStatus.value = false
            } finally {
                _isLoading.update { false }
            }
        }
    }

    fun updateReturn(
        returnId: String,
        peminjamanId: Int,
        returnDate: String,
        fineAmount: Int,
        description: String,
        proofUriString: String?
    ) {
        viewModelScope.launch {
            _isLoading.update { true }
            _transactionStatus.value = null
            try {
                val request = ReturnRequest(
                    peminjamanId = peminjamanId,
                    tanggalPengembalian = returnDate,
                    denda = fineAmount,
                    keterangan = description.takeIf { it.isNotBlank() },
                    buktiKerusakanUrl = proofUriString
                )
                // Sekarang pemanggilan ini valid
                val response = apiService.updateReturn(returnId, request)
                _transactionStatus.value = response.success
            } catch (e: Exception) {
                _transactionStatus.value = false
            } finally {
                _isLoading.update { false }
            }
        }
    }

    fun resetTransactionStatus() {
        _transactionStatus.value = null
    }

    // ---------------------- PRIVATE API CALLS ----------------------

    private fun searchMembers(query: String) {
        viewModelScope.launch {
            _isLoading.update { true }
            try {
                // Sekarang pemanggilan ini valid
                val results = apiService.searchMembers(query).map {
                    AnggotaUI(it.nim, it.nama, it.email)
                }
                _memberSearchResults.value = results
            } catch (e: Exception) {
                _memberSearchResults.value = emptyList()
            } finally {
                _isLoading.update { false }
            }
        }
    }

    private fun fetchActiveLoans(nim: String) {
        viewModelScope.launch {
            _isLoading.update { true }
            _activeLoans.value = emptyList()
            try {
                // Sekarang pemanggilan ini valid
                val loans = apiService.fetchActiveLoans(nim).map {
                    PeminjamanUI(it.id, it.judulBuku, it.author, it.tglPinjam, it.jatuhTempo)
                }
                _activeLoans.value = loans
            } catch (e: Exception) {
                _activeLoans.value = emptyList()
            } finally {
                _isLoading.update { false }
            }
        }
    }
}
