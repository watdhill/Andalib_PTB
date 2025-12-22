package com.example.andalib.screen.Borrowing

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.andalib.BookDatabase
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

    var selectedBook by mutableStateOf<BukuItem?>(null)
        private set

    var selectedMember by mutableStateOf<AnggotaItem?>(null)
        private set

    init {
        loadBorrowings()
        loadBooks()
        loadMembers()
    }


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

    fun loadBooks() {
        viewModelScope.launch {
            try {
                val response = api.getAllBooks()
                android.util.Log.d("BorrowingVM", "Backend response: ${response.code()} - ${response.message()}")
                if (response.isSuccessful && response.body() != null) {
                    val backendBooks = response.body()!!
                    android.util.Log.d("BorrowingVM", "Loaded ${backendBooks.size} books from backend")
                    books = backendBooks
                } else {
                    android.util.Log.w("BorrowingVM", "Backend failed, trying local DB...")
                    loadBooksFromLocalDb()
                }
            } catch (e: Exception) {
                android.util.Log.e("BorrowingVM", "Backend error: ${e.message}, trying local DB...")
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
                    stok = book.stok,
                    isbn = book.isbn
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
                val response = api.searchBooks(query)
                if (response.isSuccessful && response.body() != null) {
                    val backendBooks = response.body()!!
                    android.util.Log.d("BorrowingVM", "Backend search '$query' found ${backendBooks.size} books")
                    books = backendBooks
                } else {
                    searchBooksFromLocalDb(query)
                }
            } catch (e: Exception) {
                android.util.Log.e("BorrowingVM", "Backend search error: ${e.message}")
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
                    stok = book.stok,
                    isbn = book.isbn
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("BorrowingVM", "Local search error: ${e.message}")
        }
    }

    suspend fun createBorrowing(
        nim: String,
        bukuId: Int,
        jatuhTempo: String,
        tanggalPinjam: String? = null
    ): Boolean {
        return try {
            android.util.Log.d("BorrowingVM", "=== CREATE BORROWING (Basic) ===")
            android.util.Log.d("BorrowingVM", "NIM: $nim")
            android.util.Log.d("BorrowingVM", "BukuId: $bukuId")
            android.util.Log.d("BorrowingVM", "JatuhTempo: $jatuhTempo")
            android.util.Log.d("BorrowingVM", "TanggalPinjam: $tanggalPinjam")

            val adminId = tokenManager.getAdminId()
            android.util.Log.d("BorrowingVM", "AdminId: $adminId")


            val request = CreateBorrowingRequest(
                nim = nim,
                bukuId = bukuId,
                jatuhTempo = jatuhTempo,
                tanggalPinjam = tanggalPinjam,
                adminId = adminId
            )

            val response = api.createBorrowing(request)

            android.util.Log.d("BorrowingVM", "Response code: ${response.code()}")
            android.util.Log.d("BorrowingVM", "Response body: ${response.body()}")
            android.util.Log.d("BorrowingVM", "Error body: ${response.errorBody()?.string()}")

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

    suspend fun createBorrowingWithKrs(
        nim: String,
        bukuId: Int,
        jatuhTempo: String,
        tanggalPinjam: String?,
        krsUri: Uri?
    ): Boolean {
        return try {
            android.util.Log.d("BorrowingVM", "=== CREATE BORROWING WITH KRS (Uri) ===")

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

            val adminId = tokenManager.getAdminId()
            val adminIdBody = adminId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            android.util.Log.d("BorrowingVM", "AdminId: $adminId")

            val response = api.createBorrowingWithKrs(
                nim = nimBody,
                bukuId = bukuIdBody,
                jatuhTempo = jatuhTempoBody,
                tanggalPinjam = tanggalPinjamBody,
                adminId = adminIdBody,
                krsImage = krsImagePart
            )

            android.util.Log.d("BorrowingVM", "Response code: ${response.code()}")

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

    suspend fun createBorrowingWithKrsPath(
        nim: String,
        bukuId: Int,
        jatuhTempo: String,
        tanggalPinjam: String?,
        krsPath: String
    ): Boolean {
        return try {
            android.util.Log.d("BorrowingVM", "=== CREATE BORROWING WITH KRS (Path) ===")

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

            val adminId = tokenManager.getAdminId()
            val adminIdBody = adminId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            android.util.Log.d("BorrowingVM", "AdminId: $adminId")

            val response = api.createBorrowingWithKrs(
                nim = nimBody,
                bukuId = bukuIdBody,
                jatuhTempo = jatuhTempoBody,
                tanggalPinjam = tanggalPinjamBody,
                adminId = adminIdBody,
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

    suspend fun uploadKrsWithPath(id: Int, krsPath: String): Boolean {
        return try {

            if (!krsPath.startsWith("/data/")) {
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


    fun selectBook(book: BukuItem?) {
        selectedBook = book
    }

    fun selectMember(member: AnggotaItem?) {
        selectedMember = member
    }

    fun clearErrorMessage() {
        errorMessage = null
    }

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

    suspend fun createBorrowing(newBorrowing: Borrowing): Boolean {

        val book = books.find { it.title == newBorrowing.bookTitle }
        if (book == null) {
            errorMessage = "Buku '${newBorrowing.bookTitle}' tidak ditemukan. Silakan pilih dari daftar buku."
            return false
        }

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


    suspend fun updateBorrowing(borrowing: Borrowing): Boolean {
        return updateBorrowing(
            id = borrowing.id,
            jatuhTempo = borrowing.returnDate,
            status = borrowing.status
        )
    }
}