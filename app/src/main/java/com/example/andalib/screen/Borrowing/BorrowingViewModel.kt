package com.example.andalib.screen.Borrowing

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.andalib.AnggotaItem
import com.example.andalib.Book
import com.example.andalib.BookDatabase
import com.example.andalib.Borrowing
import com.example.andalib.BukuItem
import com.example.andalib.CreateBorrowingRequest
import com.example.andalib.UpdateBorrowingRequest
import com.example.andalib.data.TokenManager
import com.example.andalib.data.network.BorrowingApi
import com.example.andalib.data.network.createBorrowingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class BorrowingViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val tokenManager = TokenManager(application)
    private val api: BorrowingApi = createBorrowingService(tokenManager)
    private val bookDatabase = BookDatabase(context) // Database lokal untuk buku

    // =========================================================
    // STATE UNTUK UI
    // =========================================================

    var borrowings by mutableStateOf(emptyList<Borrowing>())
        private set

    var books by mutableStateOf(emptyList<BukuItem>())
        private set

    var members by mutableStateOf(emptyList<AnggotaItem>())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Selected items untuk form
    var selectedBook by mutableStateOf<BukuItem?>(null)
        private set

    var selectedMember by mutableStateOf<AnggotaItem?>(null)
        private set

    init {
        loadBorrowings()
        loadBooks()
        loadMembers()
    }

    // =========================================================
    // LOAD DATA
    // =========================================================

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

    /**
     * Mengambil semua buku dari backend API
     * Jika gagal, fallback ke database lokal
     */
    fun loadBooks() {
        viewModelScope.launch {
            try {
                // Coba ambil dari backend dulu
                val response = api.getAllBooks()
                android.util.Log.d("BorrowingVM", "Backend response: ${response.code()} - ${response.message()}")
                if (response.isSuccessful && response.body() != null) {
                    val backendBooks = response.body()!!
                    android.util.Log.d("BorrowingVM", "Loaded ${backendBooks.size} books from backend")
                    books = backendBooks
                } else {
                    android.util.Log.w("BorrowingVM", "Backend failed, trying local DB...")
                    // Fallback ke database lokal
                    loadBooksFromLocalDb()
                }
            } catch (e: Exception) {
                android.util.Log.e("BorrowingVM", "Backend error: ${e.message}, trying local DB...")
                // Fallback ke database lokal jika network error
                loadBooksFromLocalDb()
            }
        }
    }

    private suspend fun loadBooksFromLocalDb() {
        try {
            val localBooks = withContext(Dispatchers.IO) {
                bookDatabase.getAllBooks()
            }
            android.util.Log.d("BorrowingVM", "Loaded ${localBooks.size} books from local DB")
            books = localBooks.map { book ->
                BukuItem(
                    id = book.id,
                    title = book.title,
                    author = book.author,
                    stok = 1
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("BorrowingVM", "Local DB error: ${e.message}")
        }
    }

    fun loadMembers() {
        viewModelScope.launch {
            try {
                val response = api.getAllMembers()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success && body.data != null) {
                        members = body.data
                    }
                }
            } catch (e: Exception) {
                // Silent fail for members
            }
        }
    }

    // =========================================================
    // SEARCH
    // =========================================================

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

    fun searchMembers(query: String) {
        if (query.isEmpty()) {
            loadMembers()
            return
        }

        viewModelScope.launch {
            try {
                val response = api.searchMembers(query)
                if (response.isSuccessful && response.body() != null) {
                    members = response.body()!!
                }
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    fun searchBooks(query: String) {
        if (query.isEmpty()) {
            loadBooks()
            return
        }

        viewModelScope.launch {
            try {
                // Coba search dari backend dulu
                val response = api.searchBooks(query)
                if (response.isSuccessful && response.body() != null) {
                    val backendBooks = response.body()!!
                    android.util.Log.d("BorrowingVM", "Backend search '$query' found ${backendBooks.size} books")
                    books = backendBooks
                } else {
                    // Fallback ke database lokal
                    searchBooksFromLocalDb(query)
                }
            } catch (e: Exception) {
                android.util.Log.e("BorrowingVM", "Backend search error: ${e.message}")
                // Fallback ke database lokal
                searchBooksFromLocalDb(query)
            }
        }
    }

    private suspend fun searchBooksFromLocalDb(query: String) {
        try {
            val localBooks = withContext(Dispatchers.IO) {
                bookDatabase.searchBooks(query)
            }
            android.util.Log.d("BorrowingVM", "Local search '$query' found ${localBooks.size} books")
            books = localBooks.map { book ->
                BukuItem(
                    id = book.id,
                    title = book.title,
                    author = book.author,
                    stok = 1
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("BorrowingVM", "Local search error: ${e.message}")
        }
    }

    /**
     * Membuat peminjaman baru tanpa upload KRS
     */
    suspend fun createBorrowing(
        nim: String,
        bukuId: Int,
        jatuhTempo: String,
        tanggalPinjam: String? = null
    ): Boolean {
        return try {
            android.util.Log.d("BorrowingVM", "=== CREATE BORROWING ===")
            android.util.Log.d("BorrowingVM", "NIM: $nim")
            android.util.Log.d("BorrowingVM", "BukuId: $bukuId")
            android.util.Log.d("BorrowingVM", "JatuhTempo: $jatuhTempo")
            android.util.Log.d("BorrowingVM", "TanggalPinjam: $tanggalPinjam")

            val request = CreateBorrowingRequest(
                nim = nim,
                bukuId = bukuId,
                jatuhTempo = jatuhTempo,
                tanggalPinjam = tanggalPinjam
            )
            val response = api.createBorrowing(request)

            android.util.Log.d("BorrowingVM", "Response code: ${response.code()}")
            android.util.Log.d("BorrowingVM", "Response body: ${response.body()}")
            android.util.Log.d("BorrowingVM", "Error body: ${response.errorBody()?.string()}")

            if (response.isSuccessful && response.body()?.success == true) {
                loadBorrowings()
                loadBooks() // Refresh stok buku
                true
            } else {
                errorMessage = "Gagal menambah: ${response.body()?.message ?: response.message()}"
                android.util.Log.e("BorrowingVM", "Error: $errorMessage")
                false
            }
        } catch (e: Exception) {
            errorMessage = "Koneksi Gagal saat menambah: ${e.message}"
            android.util.Log.e("BorrowingVM", "Exception: ${e.message}", e)
            false
        }
    }

    /**
     * Membuat peminjaman baru dengan upload KRS
     */
    suspend fun createBorrowingWithKrs(
        nim: String,
        bukuId: Int,
        jatuhTempo: String,
        tanggalPinjam: String?,
        krsUri: Uri?
    ): Boolean {
        return try {
            val nimBody = nim.toRequestBody("text/plain".toMediaTypeOrNull())
            val bukuIdBody = bukuId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val jatuhTempoBody = jatuhTempo.toRequestBody("text/plain".toMediaTypeOrNull())
            val tanggalPinjamBody = tanggalPinjam?.toRequestBody("text/plain".toMediaTypeOrNull())

            var krsImagePart: MultipartBody.Part? = null
            if (krsUri != null) {
                val file = uriToFile(krsUri)
                if (file != null) {
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    krsImagePart = MultipartBody.Part.createFormData("krsImage", file.name, requestFile)
                }
            }

            val response = api.createBorrowingWithKrs(
                nim = nimBody,
                bukuId = bukuIdBody,
                jatuhTempo = jatuhTempoBody,
                tanggalPinjam = tanggalPinjamBody,
                adminId = null,
                krsImage = krsImagePart
            )

            if (response.isSuccessful && response.body()?.success == true) {
                loadBorrowings()
                loadBooks()
                true
            } else {
                errorMessage = "Gagal menambah: ${response.body()?.message ?: response.message()}"
                false
            }
        } catch (e: Exception) {
            errorMessage = "Koneksi Gagal saat menambah: ${e.message}"
            false
        }
    }

    /**
     * Membuat peminjaman baru dengan upload KRS dari file path
     */
    suspend fun createBorrowingWithKrsPath(
        nim: String,
        bukuId: Int,
        jatuhTempo: String,
        tanggalPinjam: String?,
        krsPath: String
    ): Boolean {
        return try {
            val nimBody = nim.toRequestBody("text/plain".toMediaTypeOrNull())
            val bukuIdBody = bukuId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val jatuhTempoBody = jatuhTempo.toRequestBody("text/plain".toMediaTypeOrNull())
            val tanggalPinjamBody = tanggalPinjam?.toRequestBody("text/plain".toMediaTypeOrNull())

            val file = File(krsPath)
            var krsImagePart: MultipartBody.Part? = null
            android.util.Log.d("BorrowingVM", "KRS Path: $krsPath")
            android.util.Log.d("BorrowingVM", "File exists: ${file.exists()}")
            android.util.Log.d("BorrowingVM", "File size: ${file.length()}")
            if (file.exists()) {
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                krsImagePart = MultipartBody.Part.createFormData("krsImage", file.name, requestFile)
                android.util.Log.d("BorrowingVM", "KRS Part created: ${file.name}")
            }

            val response = api.createBorrowingWithKrs(
                nim = nimBody,
                bukuId = bukuIdBody,
                jatuhTempo = jatuhTempoBody,
                tanggalPinjam = tanggalPinjamBody,
                adminId = null,
                krsImage = krsImagePart
            )

            android.util.Log.d("BorrowingVM", "Response code: ${response.code()}")
            android.util.Log.d("BorrowingVM", "Response body: ${response.body()}")

            if (response.isSuccessful && response.body()?.success == true) {
                loadBorrowings()
                loadBooks()
                true
            } else {
                errorMessage = "Gagal menambah: ${response.body()?.message ?: response.message()}"
                android.util.Log.e("BorrowingVM", "Error: $errorMessage")
                false
            }
        } catch (e: Exception) {
            errorMessage = "Koneksi Gagal saat menambah: ${e.message}"
            android.util.Log.e("BorrowingVM", "Exception: ${e.message}", e)
            false
        }
    }

    // =========================================================
    // UPDATE PEMINJAMAN
    // =========================================================

    suspend fun updateBorrowing(
        id: Int,
        jatuhTempo: String?,
        status: String? = null
    ): Boolean {
        return try {
            val request = UpdateBorrowingRequest(
                jatuhTempo = jatuhTempo,
                status = status
            )
            val response = api.updateBorrowing(id, request)
            if (response.isSuccessful && response.body()?.success == true) {
                loadBorrowings()
                true
            } else {
                errorMessage = "Gagal update: ${response.body()?.message ?: response.message()}"
                false
            }
        } catch (e: Exception) {
            errorMessage = "Koneksi Gagal saat update: ${e.message}"
            false
        }
    }

    /**
     * Upload KRS untuk peminjaman yang sudah ada
     */
    suspend fun uploadKrs(id: Int, krsUri: Uri): Boolean {
        return try {
            val file = uriToFile(krsUri)
            if (file == null) {
                errorMessage = "Gagal membaca file KRS"
                return false
            }

            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val krsImagePart = MultipartBody.Part.createFormData("krsImage", file.name, requestFile)

            val response = api.uploadKrs(id, krsImagePart)
            if (response.isSuccessful && response.body()?.success == true) {
                loadBorrowings()
                true
            } else {
                errorMessage = "Gagal upload KRS: ${response.body()?.message ?: response.message()}"
                false
            }
        } catch (e: Exception) {
            errorMessage = "Koneksi Gagal saat upload KRS: ${e.message}"
            false
        }
    }


    /**
     * Upload KRS untuk peminjaman yang sudah ada menggunakan file path
     */
    suspend fun uploadKrsWithPath(id: Int, krsPath: String): Boolean {
        return try {
            // Cek apakah ini path lokal atau path server
            if (!krsPath.startsWith("/data/")) {
                // Ini path server, tidak perlu upload ulang
                return true
            }

            val file = File(krsPath)
            if (!file.exists()) {
                errorMessage = "File KRS tidak ditemukan"
                return false
            }

            android.util.Log.d("BorrowingVM", "uploadKrsWithPath: id=$id, path=$krsPath")
            android.util.Log.d("BorrowingVM", "File exists: ${file.exists()}, size: ${file.length()}")

            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val krsImagePart = MultipartBody.Part.createFormData("krsImage", file.name, requestFile)

            val response = api.uploadKrs(id, krsImagePart)
            android.util.Log.d("BorrowingVM", "Upload response: ${response.code()}")

            if (response.isSuccessful && response.body()?.success == true) {
                loadBorrowings()
                true
            } else {
                errorMessage = "Gagal upload KRS: ${response.body()?.message ?: response.message()}"
                android.util.Log.e("BorrowingVM", "Upload failed: $errorMessage")
                false
            }
        } catch (e: Exception) {
            errorMessage = "Koneksi Gagal saat upload KRS: ${e.message}"
            android.util.Log.e("BorrowingVM", "Exception: ${e.message}", e)
            false
        }
    }

    // =========================================================
    // DELETE PEMINJAMAN
    // =========================================================

    suspend fun deleteBorrowing(id: Int): Boolean {
        return try {
            val response = api.deleteBorrowing(id)
            if (response.isSuccessful && response.body()?.success == true) {
                loadBorrowings()
                loadBooks() // Refresh stok buku
                true
            } else {
                errorMessage = "Gagal hapus: ${response.body()?.message ?: response.message()}"
                false
            }
        } catch (e: Exception) {
            errorMessage = "Koneksi Gagal saat hapus: ${e.message}"
            false
        }
    }

    // =========================================================
    // HELPER FUNCTIONS
    // =========================================================

    fun selectBook(book: BukuItem?) {
        selectedBook = book
    }

    fun selectMember(member: AnggotaItem?) {
        selectedMember = member
    }

    fun clearErrorMessage() {
        errorMessage = null
    }

    /**
     * Konversi Uri ke File
     */
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "krs_temp_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    // =========================================================
    // LEGACY SUPPORT (untuk kompatibilitas dengan BorrowingScreen lama)
    // =========================================================

    /**
     * Legacy: Create borrowing dari object Borrowing
     * Digunakan oleh BorrowingScreen lama
     */
    suspend fun createBorrowing(newBorrowing: Borrowing): Boolean {
        // Cari buku berdasarkan title
        val book = books.find { it.title == newBorrowing.bookTitle }
        if (book == null) {
            errorMessage = "Buku '${newBorrowing.bookTitle}' tidak ditemukan. Silakan pilih dari daftar buku."
            return false
        }

        // Cari member berdasarkan NIM
        val member = members.find { it.nim == newBorrowing.nim }
        if (member == null) {
            errorMessage = "Anggota dengan NIM '${newBorrowing.nim}' tidak ditemukan. Silakan pilih dari daftar anggota."
            return false
        }

        return createBorrowing(
            nim = newBorrowing.nim,
            bukuId = book.id,
            jatuhTempo = newBorrowing.returnDate,
            tanggalPinjam = newBorrowing.borrowDate
        )
    }

    /**
     * Legacy: Update borrowing dari object Borrowing
     * Digunakan oleh BorrowingScreen lama
     */
    suspend fun updateBorrowing(borrowing: Borrowing): Boolean {
        return updateBorrowing(
            id = borrowing.id,
            jatuhTempo = borrowing.returnDate,
            status = borrowing.status
        )
    }
}