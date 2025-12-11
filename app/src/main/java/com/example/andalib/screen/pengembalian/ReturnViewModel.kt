package com.example.andalib.screen.Return


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.andalib.data.network.ApiService
import com.example.andalib.data.network.ReturnRequest
import com.example.andalib.screen.AnggotaUI
import com.example.andalib.screen.PeminjamanUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class ReturnViewModel(
    // ApiService harus di-inject (disediakan) saat ViewModel dibuat.
    private val apiService: ApiService
) : ViewModel() {

    // Daftar anggota dummy (allMembers) telah dihapus

    // StateFlow untuk daftar hasil pencarian anggota
    private val _memberSearchResults = MutableStateFlow<List<AnggotaUI>>(emptyList())
    val memberSearchResults: StateFlow<List<AnggotaUI>> = _memberSearchResults.asStateFlow()

    // StateFlow untuk Anggota yang dipilih
    private val _selectedMember = MutableStateFlow<AnggotaUI?>(null)
    val selectedMember: StateFlow<AnggotaUI?> = _selectedMember.asStateFlow()

    // StateFlow untuk Daftar Pinjaman Aktif anggota yang dipilih
    private val _activeLoans = MutableStateFlow<List<PeminjamanUI>>(emptyList())
    val activeLoans: StateFlow<List<PeminjamanUI>> = _activeLoans.asStateFlow()

    // StateFlow untuk status loading API
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // StateFlow untuk Hasil Submit/Update API (true: sukses, false: gagal, null: idle)
    private val _transactionStatus = MutableStateFlow<Boolean?>(null)
    val transactionStatus: StateFlow<Boolean?> = _transactionStatus.asStateFlow()


    /**
     * Memanggil API untuk mencari daftar anggota berdasarkan query.
     */
    fun searchMembers(query: String) {
        if (query.length < 2) {
            _memberSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.update { true }
            try {
                // Panggil API searchMembers, lalu mapping ke AnggotaUI
                val results = apiService.searchMembers(query).map {
                    AnggotaUI(it.nim, it.nama)
                }
                _memberSearchResults.value = results
            } catch (e: Exception) {
                // Handle error (misalnya: tampilkan pesan error)
                _memberSearchResults.value = emptyList()
            } finally {
                _isLoading.update { false }
            }
        }
    }


    /**
     * Mengatur anggota yang dipilih dan memicu pengambilan daftar pinjaman aktif.
     */
    fun setSelectedMember(member: AnggotaUI?) {
        _selectedMember.value = member
        _memberSearchResults.value = emptyList() // Hapus hasil pencarian setelah dipilih

        if (member != null) {
            fetchActiveLoans(member.nim)
        } else {
            _activeLoans.value = emptyList()
        }
    }

    /**
     * Memanggil API untuk mengambil daftar pinjaman aktif berdasarkan NIM.
     */
    private fun fetchActiveLoans(nim: String) {
        viewModelScope.launch {
            _isLoading.update { true }
            _activeLoans.value = emptyList()
            try {
                // Panggil API fetchActiveLoans, lalu mapping ke PeminjamanUI
                val loans = apiService.fetchActiveLoans(nim).map {
                    PeminjamanUI(it.id, it.judulBuku, it.tglPinjam, it.jatuhTempo)
                }
                _activeLoans.value = loans
            } catch (e: Exception) {
                // Handle error
                _activeLoans.value = emptyList()
            } finally {
                _isLoading.update { false }
            }
        }
    }

    /**
     * Mengirim data pengembalian ke server.
     */
    fun submitReturn(request: ReturnRequest) {
        viewModelScope.launch {
            _isLoading.update { true }
            _transactionStatus.value = null
            try {
                val response = apiService.submitReturn(request)
                _transactionStatus.value = response.success
            } catch (e: Exception) {
                _transactionStatus.value = false
            } finally {
                _isLoading.update { false }
            }
        }
    }

    /**
     * Mengirim data update pengembalian ke server.
     */
    fun updateReturn(returnId: String, request: ReturnRequest) {
        viewModelScope.launch {
            _isLoading.update { true }
            _transactionStatus.value = null
            try {
                val response = apiService.updateReturn(returnId, request)
                _transactionStatus.value = response.success
            } catch (e: Exception) {
                _transactionStatus.value = false
            } finally {
                _isLoading.update { false }
            }
        }
    }

    /**
     * Mereset status transaksi setelah notifikasi ditampilkan.
     */
    fun resetTransactionStatus() {
        _transactionStatus.value = null
    }
}