package com.example.andalib.screen.Borrowing

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.BitmapFactory
import androidx.compose.material.icons.automirrored.filled.Notes
import com.example.andalib.*
import com.example.andalib.ui.theme.AndalibDarkBlue
import com.example.andalib.ui.theme.AndalibGray
import com.example.andalib.ui.theme.AndalibWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowingScreen(viewModel: BorrowingViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State dari ViewModel
    val borrowings = viewModel.borrowings
    val books = viewModel.books
    val members = viewModel.members
    val isLoading = viewModel.isLoading
    val vmErrorMessage = viewModel.errorMessage

    var searchQuery by remember { mutableStateOf("") }
    var currentView by remember { mutableStateOf("list") }
    var selectedBorrowing by remember { mutableStateOf<Borrowing?>(null) }

    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Form States - Simplified
    var selectedMember by remember { mutableStateOf<AnggotaItem?>(null) }
    var selectedBook by remember { mutableStateOf<BukuItem?>(null) }
    var formIdentityPath by remember { mutableStateOf("") }
    var formBorrowDate by remember { mutableStateOf(getCurrentDate()) }
    var formReturnDate by remember { mutableStateOf(getFutureDate(7)) }

    // Search states for dropdowns
    var memberSearchQuery by remember { mutableStateOf("") }
    var bookSearchQuery by remember { mutableStateOf("") }
    var showMemberDropdown by remember { mutableStateOf(false) }
    var showBookDropdown by remember { mutableStateOf(false) }

    // Error handling
    LaunchedEffect(vmErrorMessage) {
        if (vmErrorMessage != null) {
            notificationMessage = "ERROR: $vmErrorMessage"
            showNotification = true
        }
    }

    // Search members when query changes - with debouncing
    LaunchedEffect(memberSearchQuery) {
        // Tunggu 300ms setelah user berhenti mengetik
        kotlinx.coroutines.delay(300)
        if (memberSearchQuery.isNotEmpty()) {
            viewModel.searchMembers(memberSearchQuery)
        } else {
            viewModel.loadMembers()
        }
    }

    // Search books when query changes - with debouncing
    LaunchedEffect(bookSearchQuery) {
        // Tunggu 300ms setelah user berhenti mengetik
        kotlinx.coroutines.delay(300)
        if (bookSearchQuery.isNotEmpty()) {
            viewModel.searchBooks(bookSearchQuery)
        } else {
            viewModel.loadBooks()
        }
    }

    fun showNotif(message: String) {
        notificationMessage = message
        showNotification = true
    }

    fun resetForm() {
        selectedMember = null
        selectedBook = null
        memberSearchQuery = ""
        bookSearchQuery = ""
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
                            "add" -> "Tambah Peminjaman"
                            "edit" -> "Edit Peminjaman"
                            "detail" -> "Detail Peminjaman"
                            else -> "Peminjaman Buku"
                        },
                        fontWeight = FontWeight.Bold,
                        color = AndalibWhite
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AndalibDarkBlue),
                navigationIcon = {
                    if (currentView != "list") {
                        IconButton(onClick = {
                            currentView = "list"
                            resetForm()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = AndalibWhite)
                        }
                    }
                },
                actions = {
                    if (currentView == "list") {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    resetForm()
                                    currentView = "add"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Tambah",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
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
                        TextButton(onClick = {
                            showNotification = false
                            viewModel.clearErrorMessage()
                        }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) {
                    Text(notificationMessage)
                }
                LaunchedEffect(showNotification) {
                    delay(3000)
                    showNotification = false
                    viewModel.clearErrorMessage()
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AndalibDarkBlue)
            }
        } else {
            when (currentView) {
                "list" -> BorrowingListView(
                    borrowings = borrowings,
                    searchQuery = searchQuery,
                    onSearchChange = {
                        searchQuery = it
                        viewModel.searchBorrowings(it)
                    },
                    onDetailClick = { borrowing ->
                        selectedBorrowing = borrowing
                        currentView = "detail"
                    },
                    onAddClick = { currentView = "add" },
                    modifier = Modifier.padding(top = padding.calculateTopPadding())
                )

                "detail" -> selectedBorrowing?.let { borrowing ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding())
                    ) {
                        BorrowingDetailView(
                            borrowing = borrowing,
                            onEdit = {
                                // Find member and book from lists
                                selectedMember = members.find { it.nim == borrowing.nim }
                                selectedBook = books.find { it.title == borrowing.bookTitle }
                                memberSearchQuery = borrowing.nim
                                bookSearchQuery = borrowing.bookTitle
                                android.util.Log.d("BorrowingScreen", "onEdit: identityPath='${borrowing.identityPath}'")
                                formIdentityPath = borrowing.identityPath
                                formBorrowDate = borrowing.borrowDate
                                formReturnDate = borrowing.returnDate
                                currentView = "edit"
                            },
                            onDelete = {
                                showDeleteDialog = true
                            }
                        )
                    }
                }

                "add", "edit" -> AddEditBorrowingViewNew(
                    isEdit = currentView == "edit",
                    // Member selection
                    selectedMember = selectedMember,
                    memberSearchQuery = memberSearchQuery,
                    members = members,
                    showMemberDropdown = showMemberDropdown,
                    onMemberSearchChange = {
                        memberSearchQuery = it
                        showMemberDropdown = it.isNotEmpty()
                    },
                    onMemberSelect = { member ->
                        selectedMember = member
                        memberSearchQuery = "${member.nim} - ${member.name}"
                        showMemberDropdown = false
                    },
                    onMemberDropdownDismiss = { showMemberDropdown = false },
                    // Book selection
                    selectedBook = selectedBook,
                    bookSearchQuery = bookSearchQuery,
                    books = books,
                    showBookDropdown = showBookDropdown,
                    onBookSearchChange = {
                        bookSearchQuery = it
                        showBookDropdown = it.isNotEmpty()
                    },
                    onBookSelect = { book ->
                        selectedBook = book
                        bookSearchQuery = book.title
                        showBookDropdown = false
                    },
                    onBookDropdownDismiss = { showBookDropdown = false },
                    // Other fields
                    identityPath = formIdentityPath,
                    borrowDate = formBorrowDate,
                    returnDate = formReturnDate,
                    onIdentityPathChange = { formIdentityPath = it },
                    onBorrowDateChange = { formBorrowDate = it },
                    onReturnDateChange = { formReturnDate = it },
                    onSave = {
                        android.util.Log.d("BorrowingScreen", "onSave called: formIdentityPath='$formIdentityPath', isEmpty=${formIdentityPath.isEmpty()}")
                        if (selectedMember != null && selectedBook != null) {
                            scope.launch {
                                val success = if (currentView == "add") {
                                    // Gunakan createBorrowingWithKrsPath jika ada gambar KRS
                                    android.util.Log.d("BorrowingScreen", "Creating new borrowing, formIdentityPath='$formIdentityPath'")
                                    if (formIdentityPath.isNotEmpty()) {
                                        viewModel.createBorrowingWithKrsPath(
                                            nim = selectedMember!!.nim,
                                            bukuId = selectedBook!!.id,
                                            jatuhTempo = formReturnDate,
                                            tanggalPinjam = formBorrowDate.ifEmpty { null },
                                            krsPath = formIdentityPath
                                        )
                                    } else {
                                        viewModel.createBorrowing(
                                            nim = selectedMember!!.nim,
                                            bukuId = selectedBook!!.id,
                                            jatuhTempo = formReturnDate,
                                            tanggalPinjam = formBorrowDate
                                        )
                                    }
                                } else {
                                    selectedBorrowing?.let { borrowing ->
                                        // Update borrowing data terlebih dahulu
                                        val updateSuccess = viewModel.updateBorrowing(
                                            id = borrowing.id,
                                            jatuhTempo = formReturnDate
                                        )

                                        // Jika ada KRS baru (path lokal), upload
                                        if (updateSuccess && formIdentityPath.startsWith("/data/")) {
                                            viewModel.uploadKrsWithPath(borrowing.id, formIdentityPath)
                                        } else {
                                            updateSuccess
                                        }
                                    } ?: false
                                }

                                if (success) {
                                    showNotif(if (currentView == "add") "✓ Peminjaman berhasil ditambahkan!" else "✓ Peminjaman berhasil diperbarui!")
                                    currentView = "list"
                                    resetForm()
                                }
                            }
                        } else {
                            showNotif("Harap pilih Anggota dan Buku dari daftar")
                        }
                    },
                    context = context,
                    modifier = Modifier.padding(padding)
                )
            }
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
                        scope.launch {
                            selectedBorrowing?.let { borrowing ->
                                val success = viewModel.deleteBorrowing(borrowing.id)
                                if (success) {
                                    showNotif("✓ Peminjaman berhasil dihapus!")
                                    currentView = "list"
                                    selectedBorrowing = null
                                }
                            }
                            showDeleteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

// ============================================================
// LIST VIEW
// ============================================================

@Composable
fun BorrowingListView(
    borrowings: List<Borrowing>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onDetailClick: (Borrowing) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AndalibDarkBlue)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Title
                Text(
                    text = "Peminjaman Buku",
                    modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 8.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Cari nama, NIM, atau judul buku...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AndalibGray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = AndalibGray)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = AndalibDarkBlue
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Borrowing List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (borrowings.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color(0xFFE0E0E0)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Belum ada data peminjaman",
                                    color = AndalibGray,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(borrowings) { borrowing ->
                            BorrowingCard(
                                borrowing = borrowing,
                                onDetailClick = { onDetailClick(borrowing) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BorrowingCard(
    borrowing: Borrowing,
    onDetailClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetailClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Avatar + Borrower Info + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(AndalibDarkBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = AndalibDarkBlue
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Borrower Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = borrowing.borrowerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "NIM: ${borrowing.nim}",
                        fontSize = 12.sp,
                        color = AndalibGray
                    )
                    if (borrowing.major.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val badgeColor = Color(0xFF00ACC1)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = badgeColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = borrowing.major.replace("_", " "),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = badgeColor
                            )
                        }
                    }
                }

                // Status Badge
                val isReturned = borrowing.status == "DIKEMBALIKAN"
                val statusColor = if (isReturned) Color(0xFF2196F3) else Color(0xFF4CAF50)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = borrowing.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // View Detail Button - full width blue with press effect
            Button(
                onClick = onDetailClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AndalibDarkBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lihat Detail",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

// ============================================================
// DETAIL VIEW
// ============================================================

@Composable
fun BorrowingDetailView(
    borrowing: Borrowing,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(AndalibDarkBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = AndalibDarkBlue
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = borrowing.borrowerName,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "NIM: ${borrowing.nim}",
            fontSize = 16.sp,
            color = AndalibGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Jurusan
                DetailRowBorrowing(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = "Jurusan",
                    value = borrowing.major.replace("_", " ").ifEmpty { "-" }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE0E0E0))
                Spacer(modifier = Modifier.height(16.dp))

                // Buku
                DetailRowBorrowing(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = "Judul Buku",
                    value = borrowing.bookTitle
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pengarang
                DetailRowBorrowing(
                    icon = Icons.Default.Person,
                    label = "Pengarang",
                    value = borrowing.author.ifEmpty { "-" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stok Buku
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        tint = AndalibDarkBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Stok Buku", fontSize = 12.sp, color = AndalibGray)
                        val stokColor = if (borrowing.stok > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        Box(
                            modifier = Modifier
                                .background(stokColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${borrowing.stok} tersedia",
                                fontWeight = FontWeight.Bold,
                                color = stokColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE0E0E0))
                Spacer(modifier = Modifier.height(16.dp))

                // Tanggal Pinjam
                DetailRowBorrowing(
                    icon = Icons.Default.CalendarToday,
                    label = "Tanggal Pinjam",
                    value = borrowing.borrowDate
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Jatuh Tempo
                DetailRowBorrowing(
                    icon = Icons.Default.Event,
                    label = "Jatuh Tempo",
                    value = borrowing.returnDate
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE0E0E0))
                Spacer(modifier = Modifier.height(16.dp))

                // Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = AndalibDarkBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Status", fontSize = 12.sp, color = AndalibGray)
                        val isReturned = borrowing.status == "DIKEMBALIKAN"
                        val statusColor = if (isReturned) Color(0xFF2196F3) else Color(0xFF4CAF50)
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = borrowing.status,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onEdit,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AndalibDarkBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit", fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onDelete,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hapus", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun DetailRowBorrowing(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = AndalibDarkBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = AndalibGray)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

// ============================================================
// ADD/EDIT VIEW WITH AUTOCOMPLETE DROPDOWNS
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBorrowingViewNew(
    isEdit: Boolean,
    // Member selection
    selectedMember: AnggotaItem?,
    memberSearchQuery: String,
    members: List<AnggotaItem>,
    showMemberDropdown: Boolean,
    onMemberSearchChange: (String) -> Unit,
    onMemberSelect: (AnggotaItem) -> Unit,
    onMemberDropdownDismiss: () -> Unit,
    // Book selection
    selectedBook: BukuItem?,
    bookSearchQuery: String,
    books: List<BukuItem>,
    showBookDropdown: Boolean,
    onBookSearchChange: (String) -> Unit,
    onBookSelect: (BukuItem) -> Unit,
    onBookDropdownDismiss: () -> Unit,
    // Other fields
    identityPath: String,
    borrowDate: String,
    returnDate: String,
    onIdentityPathChange: (String) -> Unit,
    onBorrowDateChange: (String) -> Unit,
    onReturnDateChange: (String) -> Unit,
    onSave: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var cameraFilePath by remember { mutableStateOf<String?>(null) }
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = saveImageToInternalStorage(context, it, "krs")
            onIdentityPathChange(savedPath)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        android.util.Log.d("BorrowingScreen", "Camera result: success=$success, cameraFilePath=$cameraFilePath")
        if (success && cameraFilePath != null) {
            // Gambar sudah disimpan di file oleh kamera, langsung gunakan path-nya
            android.util.Log.d("BorrowingScreen", "Setting identityPath: $cameraFilePath")
            onIdentityPathChange(cameraFilePath!!)
        }
    }

    val focusedColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AndalibDarkBlue,
        focusedLabelColor = AndalibDarkBlue,
        cursorColor = AndalibDarkBlue
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SECTION: PILIH ANGGOTA
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AndalibDarkBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih Anggota", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Search field with dropdown
                    ExposedDropdownMenuBox(
                        expanded = showMemberDropdown && members.isNotEmpty(),
                        onExpandedChange = { }
                    ) {
                        OutlinedTextField(
                            value = memberSearchQuery,
                            onValueChange = onMemberSearchChange,
                            label = { Text("Cari NIM atau Nama Anggota") },
                            placeholder = { Text("Ketik untuk mencari...") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true,
                            readOnly = isEdit,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (memberSearchQuery.isNotEmpty() && !isEdit) {
                                    IconButton(onClick = {
                                        onMemberSearchChange("")
                                        onMemberSelect(AnggotaItem("", "", null, null, null))
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Hapus")
                                    }
                                }
                            },
                            colors = focusedColors
                        )

                        ExposedDropdownMenu(
                            expanded = showMemberDropdown && members.isNotEmpty() && !isEdit,
                            onDismissRequest = onMemberDropdownDismiss
                        ) {
                            members.take(5).forEach { member ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(member.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                "NIM: ${member.nim} • ${member.major ?: ""}",
                                                fontSize = 12.sp,
                                                color = AndalibGray
                                            )
                                        }
                                    },
                                    onClick = { onMemberSelect(member) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(AndalibDarkBlue.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                member.name.firstOrNull()?.uppercase() ?: "?",
                                                color = AndalibDarkBlue,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Show selected member info
                    if (selectedMember != null && selectedMember.nim.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, AndalibDarkBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = AndalibDarkBlue.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar dengan inisial
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(AndalibDarkBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = selectedMember.name.firstOrNull()?.uppercase() ?: "?",
                                        color = AndalibDarkBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Info anggota
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedMember.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "NIM: ${selectedMember.nim}",
                                        fontSize = 12.sp,
                                        color = AndalibGray
                                    )
                                    selectedMember.major?.let { major ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val badgeColor = Color(0xFF00ACC1)
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = badgeColor.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = major.replace("_", " "),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = badgeColor
                                            )
                                        }
                                    }
                                    selectedMember.contact?.let { contact ->
                                        Text(
                                            text = "📞 $contact",
                                            fontSize = 11.sp,
                                            color = AndalibGray
                                        )
                                    }
                                }

                                // Checkmark
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Dipilih",
                                    tint = AndalibDarkBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION: PILIH BUKU
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = AndalibDarkBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih Buku", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Search field with dropdown
                    ExposedDropdownMenuBox(
                        expanded = showBookDropdown && books.isNotEmpty(),
                        onExpandedChange = { }
                    ) {
                        OutlinedTextField(
                            value = bookSearchQuery,
                            onValueChange = onBookSearchChange,
                            label = { Text("Cari Judul Buku") },
                            placeholder = { Text("Ketik untuk mencari...") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true,
                            readOnly = isEdit,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (bookSearchQuery.isNotEmpty() && !isEdit) {
                                    IconButton(onClick = {
                                        onBookSearchChange("")
                                        onBookSelect(BukuItem(0, "", "", 0))
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Hapus")
                                    }
                                }
                            },
                            colors = focusedColors
                        )

                        ExposedDropdownMenu(
                            expanded = showBookDropdown && books.isNotEmpty() && !isEdit,
                            onDismissRequest = onBookDropdownDismiss
                        ) {
                            books.take(5).forEach { book ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(book.title, fontWeight = FontWeight.Bold)
                                            Text(
                                                "oleh ${book.author} • Stok: ${book.stok}",
                                                fontSize = 12.sp,
                                                color = if (book.stok > 0) AndalibGray else Color.Red
                                            )
                                        }
                                    },
                                    onClick = {
                                        if (book.stok > 0) {
                                            onBookSelect(book)
                                        }
                                    },
                                    enabled = book.stok > 0,
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = if (book.stok > 0) AndalibDarkBlue else AndalibGray
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Show selected book info
                    if (selectedBook != null && selectedBook.id != 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, AndalibDarkBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = AndalibDarkBlue.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Book icon placeholder
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AndalibDarkBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = AndalibDarkBlue,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Info buku
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedBook.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "oleh ${selectedBook.author}",
                                        fontSize = 12.sp,
                                        color = AndalibGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Badge stok
                                    val stokColor = if (selectedBook.stok > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = stokColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Stok: ${selectedBook.stok}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = stokColor
                                        )
                                    }
                                }

                                // Checkmark
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Dipilih",
                                    tint = AndalibDarkBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION: TANGGAL
        item {
            var showBorrowDatePicker by remember { mutableStateOf(false) }
            var showReturnDatePicker by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = AndalibDarkBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tanggal Peminjaman", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tanggal Pinjam
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Tanggal Pinjam",
                                fontSize = 12.sp,
                                color = AndalibGray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isEdit) Color.Gray else AndalibDarkBlue, RoundedCornerShape(8.dp))
                                    .background(if (isEdit) Color(0xFFF5F5F5) else Color.White)
                                    .clickable(enabled = !isEdit) { showBorrowDatePicker = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = borrowDate.ifEmpty { "Pilih tanggal" },
                                        color = if (borrowDate.isEmpty()) AndalibGray else Color.Black
                                    )
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = "Pilih Tanggal",
                                        tint = if (isEdit) AndalibGray else AndalibDarkBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Jatuh Tempo
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Jatuh Tempo",
                                fontSize = 12.sp,
                                color = AndalibGray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, AndalibDarkBlue, RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .clickable { showReturnDatePicker = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = returnDate.ifEmpty { "Pilih tanggal" },
                                        color = if (returnDate.isEmpty()) AndalibGray else Color.Black
                                    )
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = "Pilih Tanggal",
                                        tint = AndalibDarkBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // DatePicker Dialog untuk Tanggal Pinjam
            if (showBorrowDatePicker) {
                val datePickerState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showBorrowDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                val date = java.util.Date(millis)
                                onBorrowDateChange(sdf.format(date))
                            }
                            showBorrowDatePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBorrowDatePicker = false }) {
                            Text("Batal")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // DatePicker Dialog untuk Jatuh Tempo
            if (showReturnDatePicker) {
                val datePickerState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showReturnDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                val date = java.util.Date(millis)
                                onReturnDateChange(sdf.format(date))
                            }
                            showReturnDatePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReturnDatePicker = false }) {
                            Text("Batal")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }

        // SECTION: UPLOAD KRS (Optional)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AndalibDarkBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload KRS (Opsional)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (identityPath.isNotEmpty()) {
                        // Check if it's a local file path (/data/) or a server path (/uploads/)
                        if (identityPath.startsWith("/data/")) {
                            // Local file - use BitmapFactory
                            val file = File(identityPath)
                            if (file.exists()) {
                                val bitmap = BitmapFactory.decodeFile(identityPath)
                                bitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "KRS",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        } else {
                            // Remote file from backend - use AsyncImage
                            // If path starts with /uploads/, prepend base URL
                            val krsUrl = if (identityPath.startsWith("/uploads/")) {
                                "http://10.0.2.2:3000$identityPath"
                            } else {
                                "http://10.0.2.2:3000/uploads/krs/$identityPath"
                            }
                            AsyncImage(
                                model = krsUrl,
                                contentDescription = "KRS",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = { showImageSourceDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (identityPath.isEmpty()) "Upload Foto KRS" else "Ganti Foto")
                    }
                }
            }
        }

        // SAVE BUTTON
        item {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = selectedMember != null && selectedMember.nim.isNotEmpty() &&
                        selectedBook != null && selectedBook.id != 0,
                colors = ButtonDefaults.buttonColors(containerColor = AndalibDarkBlue)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isEdit) "Simpan Perubahan" else "Tambah Peminjaman",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
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
                            val photoFile = createImageFile(context, "krs")
                            cameraFilePath = photoFile.absolutePath
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