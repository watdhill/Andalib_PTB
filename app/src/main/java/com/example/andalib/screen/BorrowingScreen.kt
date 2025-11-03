package com.example.andalib.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.andalib.Borrowing
import com.example.andalib.BorrowingDatabase
import com.example.andalib.ui.theme.AndalibDarkBlue
import com.example.andalib.ui.theme.AndalibGray
import com.example.andalib.ui.theme.AndalibWhite
import kotlinx.coroutines.delay
import java.io.File
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import com.example.andalib.createImageFile
import com.example.andalib.saveImageToInternalStorage
import com.example.andalib.getCurrentDate
import com.example.andalib.getFutureDate
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowingScreen() {
    val context = LocalContext.current
    val database = remember { BorrowingDatabase(context) }

    var borrowings by remember { mutableStateOf(database.getAllActiveBorrowings()) }
    var searchQuery by remember { mutableStateOf("") }
    var currentView by remember { mutableStateOf("list") } // list, add, edit
    var selectedBorrowing by remember { mutableStateOf<Borrowing?>(null) }
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Form States
    var formName by remember { mutableStateOf("") }
    var formNim by remember { mutableStateOf("") }
    var formMajor by remember { mutableStateOf("") }
    var formContact by remember { mutableStateOf("") }
    var formBookTitle by remember { mutableStateOf("") }
    var formAuthor by remember { mutableStateOf("") }
    var formIsbn by remember { mutableStateOf("") }
    var formIdentityPath by remember { mutableStateOf("") }
    var formBorrowDate by remember { mutableStateOf(getCurrentDate()) }
    var formReturnDate by remember { mutableStateOf(getFutureDate(7)) }

    val filteredBorrowings = if (searchQuery.isEmpty()) {
        borrowings
    } else {
        database.searchBorrowings(searchQuery)
    }

    fun refreshBorrowings() {
        borrowings = database.getAllActiveBorrowings()
    }

    fun showNotif(message: String) {
        notificationMessage = message
        showNotification = true
    }

    fun resetForm() {
        formName = ""
        formNim = ""
        formMajor = ""
        formContact = ""
        formBookTitle = ""
        formAuthor = ""
        formIsbn = ""
        formIdentityPath = ""
        formBorrowDate = getCurrentDate()
        formReturnDate = getFutureDate(7)
        selectedBorrowing = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentView) {
                            "add" -> "Tambah Data Peminjam"
                            "edit" -> "Edit Data Peminjam"
                            else -> "Peminjaman Buku"
                        },
                        fontWeight = FontWeight.Bold,
                        color = AndalibWhite
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AndalibDarkBlue
                ),
                actions = {
                    if (currentView == "list") {
                        IconButton(onClick = {
                            resetForm()
                            currentView = "add"
                        }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Tambah Peminjaman",
                                tint = AndalibWhite
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (currentView != "list") {
                        IconButton(onClick = {
                            currentView = "list"
                            selectedBorrowing = null
                            resetForm()
                        }) {
                            Icon(
                                // FIX 1: Mengganti ke AutoMirrored
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = AndalibWhite
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = {
            if (showNotification) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { showNotification = false }) {
                            Text("OK", color = AndalibWhite)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AndalibWhite
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(notificationMessage, color = AndalibWhite)
                    }
                }
                LaunchedEffect(Unit) {
                    delay(3000)
                    showNotification = false
                }
            }
        }
    ) { padding ->
        when (currentView) {
            "list" -> BorrowingListView(
                borrowings = filteredBorrowings,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onEditClick = { borrowing ->
                    selectedBorrowing = borrowing
                    formName = borrowing.borrowerName
                    formNim = borrowing.nim
                    formMajor = borrowing.major
                    formContact = borrowing.contact
                    formBookTitle = borrowing.bookTitle
                    formAuthor = borrowing.author
                    formIsbn = borrowing.isbn
                    formIdentityPath = borrowing.identityPath
                    formBorrowDate = borrowing.borrowDate
                    formReturnDate = borrowing.returnDate
                    currentView = "edit"
                },
                onDeleteClick = { borrowing ->
                    selectedBorrowing = borrowing
                    showDeleteDialog = true
                },
                modifier = Modifier.padding(padding)
            )

            "add", "edit" -> AddEditBorrowingView(
                isEdit = currentView == "edit",
                name = formName,
                nim = formNim,
                major = formMajor,
                contact = formContact,
                bookTitle = formBookTitle,
                author = formAuthor,
                isbn = formIsbn,
                identityPath = formIdentityPath,
                borrowDate = formBorrowDate,
                returnDate = formReturnDate,
                onNameChange = { formName = it },
                onNimChange = { formNim = it },
                onMajorChange = { formMajor = it },
                onContactChange = { formContact = it },
                onBookTitleChange = { formBookTitle = it },
                onAuthorChange = { formAuthor = it },
                onIsbnChange = { formIsbn = it },
                onIdentityPathChange = { formIdentityPath = it },
                onBorrowDateChange = { formBorrowDate = it },
                onReturnDateChange = { formReturnDate = it },
                onSave = {
                    if (formName.isNotEmpty() && formNim.isNotEmpty() && formBookTitle.isNotEmpty()) {
                        if (currentView == "add") {
                            val newBorrowing = Borrowing(
                                borrowerName = formName,
                                nim = formNim,
                                major = formMajor,
                                contact = formContact,
                                bookTitle = formBookTitle,
                                author = formAuthor,
                                isbn = formIsbn,
                                identityPath = formIdentityPath,
                                borrowDate = formBorrowDate,
                                returnDate = formReturnDate
                            )
                            val result = database.insertBorrowing(newBorrowing)
                            if (result > 0) {
                                showNotif("✓ Buku berhasil dipinjamkan!")
                            } else {
                                showNotif("✗ Gagal meminjamkan buku")
                            }
                        } else {
                            selectedBorrowing?.let { borrowing ->
                                val updatedBorrowing = borrowing.copy(
                                    borrowerName = formName,
                                    nim = formNim,
                                    major = formMajor,
                                    contact = formContact,
                                    bookTitle = formBookTitle,
                                    author = formAuthor,
                                    isbn = formIsbn,
                                    identityPath = formIdentityPath,
                                    borrowDate = formBorrowDate,
                                    returnDate = formReturnDate
                                )
                                database.updateBorrowing(updatedBorrowing)
                                showNotif("✓ Data peminjaman berhasil diperbarui!")
                            }
                        }
                        refreshBorrowings()
                        currentView = "list"
                        resetForm()
                    }
                },
                context = context,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Verifikasi hapus") },
            text = { Text("Apakah Anda yakin ingin menghapus data peminjaman ini?") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedBorrowing?.let { borrowing ->
                            database.deleteBorrowing(borrowing.id)
                            refreshBorrowings()
                            showNotif("✓ Data peminjaman berhasil dihapus!")
                            currentView = "list"
                            selectedBorrowing = null
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// --- Composable untuk List Peminjaman Aktif ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowingListView(
    borrowings: List<Borrowing>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onEditClick: (Borrowing) -> Unit,
    onDeleteClick: (Borrowing) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Daftar Peminjam Aktif",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = AndalibDarkBlue,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Cari data peminjam aktif") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AndalibDarkBlue,
                focusedLabelColor = AndalibDarkBlue,
                unfocusedBorderColor = AndalibGray.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (borrowings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = Color.Gray.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Tidak ada peminjaman aktif",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(borrowings) { borrowing ->
                    BorrowingItem(
                        borrowing = borrowing,
                        onEditClick = { onEditClick(borrowing) },
                        onDeleteClick = { onDeleteClick(borrowing) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun BorrowingItem(
    borrowing: Borrowing,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AndalibWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- Bagian Atas: Ikon dan Detail Buku/Peminjam ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // IKON BUKU LINGKARAN PENUH BIRU
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        // FIX 2: Mengganti ke AutoMirrored
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Book Icon",
                        modifier = Modifier.size(32.dp),
                        tint = AndalibDarkBlue
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // DETAIL Teks
                Column(modifier = Modifier.weight(1f)) {
                    // Judul Buku
                    Text(
                        text = borrowing.bookTitle,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = AndalibDarkBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Nama & NIM Peminjam
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = AndalibDarkBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${borrowing.borrowerName} | ${borrowing.nim}",
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tanggal Pinjam/Kembali
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = AndalibGray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pinjam : ${borrowing.borrowDate}",
                            fontSize = 12.sp,
                            color = AndalibGray
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Kembali : ${borrowing.returnDate}",
                            fontSize = 12.sp,
                            color = AndalibGray
                        )
                    }
                }
            }

            // FIX 3: Mengganti ke HorizontalDivider
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Bagian Bawah: Tombol Aksi ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // BUTTON EDIT (Hijau + Ikon Edit)
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f).height(45.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50) // Hijau solid
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit", fontWeight = FontWeight.SemiBold)
                }

                // BUTTON HAPUS (Merah + Ikon Sampah)
                Button(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f).height(45.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error // Merah
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}


// --- Composable untuk Tambah/Edit Peminjaman ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBorrowingView(
    isEdit: Boolean,
    name: String,
    nim: String,
    major: String,
    contact: String,
    bookTitle: String,
    author: String,
    isbn: String,
    identityPath: String,
    borrowDate: String,
    returnDate: String,
    onNameChange: (String) -> Unit,
    onNimChange: (String) -> Unit,
    onMajorChange: (String) -> Unit,
    onContactChange: (String) -> Unit,
    onBookTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onIsbnChange: (String) -> Unit,
    onIdentityPathChange: (String) -> Unit,
    onBorrowDateChange: (String) -> Unit,
    onReturnDateChange: (String) -> Unit,
    onSave: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val majors = listOf(
        "Teknik Informatika", "Sistem Informasi", "Teknik Elektro",
        "Teknik Mesin", "Manajemen", "Akuntansi", "Lainnya"
    )

    // Definisikan warna fokus
    val focusedColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AndalibDarkBlue,
        focusedLabelColor = AndalibDarkBlue,
        cursorColor = AndalibDarkBlue,
        unfocusedBorderColor = AndalibGray.copy(alpha = 0.5f)
    )

    // Image Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = saveImageToInternalStorage(context, it, "identity")
            onIdentityPathChange(savedPath)
        }
    }

    // Camera Launcher
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri.value?.let { uri ->
                val savedPath = saveImageToInternalStorage(context, uri, "identity")
                onIdentityPathChange(savedPath)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        item {
            // Judul Form
            Text(
                text = if (isEdit) "Edit Data Peminjam" else "Tambah Data Peminjam",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AndalibDarkBlue
            )
            // FIX 3: Mengganti ke HorizontalDivider
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Form Peminjam ---

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nama Peminjam *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = focusedColors
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = nim,
                onValueChange = onNimChange,
                label = { Text("NIM *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                colors = focusedColors
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Major Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = major,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Jurusan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = focusedColors
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    majors.forEach { majorItem ->
                        DropdownMenuItem(
                            text = { Text(majorItem) },
                            onClick = {
                                onMajorChange(majorItem)
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = contact,
                onValueChange = onContactChange,
                label = { Text("Kontak") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                colors = focusedColors
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- Form Buku ---

            OutlinedTextField(
                value = bookTitle,
                onValueChange = onBookTitleChange,
                label = { Text("Judul Buku *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = focusedColors
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = author,
                onValueChange = onAuthorChange,
                label = { Text("Pengarang") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = focusedColors
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = isbn,
                onValueChange = onIsbnChange,
                label = { Text("ISBN") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = focusedColors
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- Upload Identitas ---

            Text(
                text = "Upload Kartu Identitas/KTM",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AndalibGray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (identityPath.isNotEmpty() && File(identityPath).exists()) {
                        val bitmap = remember(identityPath) {
                            BitmapFactory.decodeFile(identityPath)
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "ID Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = AndalibGray
                        )
                    }
                }

                Button(
                    onClick = { showImageSourceDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (identityPath.isNotEmpty()) "Ganti File" else "Upload Identitas")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Tanggal Pinjam dan Kembali ---

            OutlinedTextField(
                value = borrowDate,
                onValueChange = onBorrowDateChange,
                label = { Text("Tanggal Pinjam *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = isEdit,
                colors = focusedColors
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = returnDate,
                onValueChange = onReturnDateChange,
                label = { Text("Tanggal Kembali *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = focusedColors
            )
            Spacer(modifier = Modifier.height(32.dp))

            // --- Tombol Simpan ---
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotEmpty() && nim.isNotEmpty() && bookTitle.isNotEmpty()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEdit) "Simpan" else "Tambah Peminjaman")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tombol Batal (hanya di mode edit)
            if (isEdit) {
                OutlinedButton(
                    onClick = {
                        // Kembali ke list view
                        onSave() // Memastikan navigasi kembali ke list
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Batal")
                }
            }
        }
    }

    // Image Source Dialog
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Pilih Sumber Foto") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Galeri")
                    }
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            val photoFile = createImageFile(context, "identity")
                            cameraImageUri.value = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                photoFile
                            )
                            cameraLauncher.launch(cameraImageUri.value!!)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kamera")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}