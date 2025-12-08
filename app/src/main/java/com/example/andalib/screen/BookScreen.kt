package com.example.andalib.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import com.example.andalib.Book
import com.example.andalib.BookDatabase
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import com.example.andalib.data.network.createBookService
import com.example.andalib.saveImageToInternalStorage
import com.example.andalib.ui.theme.AndalibDarkBlue


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen() {
    val context = LocalContext.current
    val database = remember { BookDatabase(context) }
    val coroutineScope = rememberCoroutineScope()

    var books by remember { mutableStateOf(database.getAllBooks()) }
    var searchQuery by remember { mutableStateOf("") }
    var currentView by remember { mutableStateOf("list") }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }

    var formTitle by remember { mutableStateOf("") }
    var formAuthor by remember { mutableStateOf("") }
    var formPublisher by remember { mutableStateOf("") }
    var formYear by remember { mutableStateOf("") }
    var formCategory by remember { mutableStateOf("") }
    var formCoverPath by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val filteredBooks = if (searchQuery.isEmpty()) {
        books
    } else {
        database.searchBooks(searchQuery)
    }

    fun refreshBooks() {
        books = database.getAllBooks()
    }

    fun showNotif(message: String) {
        notificationMessage = message
        showNotification = true
    }

    fun resetForm() {
        formTitle = ""
        formAuthor = ""
        formPublisher = ""
        formYear = ""
        formCategory = ""
        formCoverPath = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Perpustakaan",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AndalibDarkBlue,
                    titleContentColor = Color.White
                ),
                actions = {
                    if (currentView == "list") {
                        IconButton(onClick = {
                            resetForm()
                            currentView = "add"
                        }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Tambah Buku",
                                tint = Color.White
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (currentView != "list") {
                        IconButton(onClick = {
                            currentView = "list"
                            selectedBook = null
                        }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White
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
                            Text("OK")
                        }
                    }
                ) {
                    Text(notificationMessage)
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000)
                    showNotification = false
                }
            }
        }
    ) { padding ->
        when (currentView) {
            "list" -> BookListView(
                books = filteredBooks,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onBookClick = { book ->
                    selectedBook = book
                    currentView = "detail"
                },
                modifier = Modifier.padding(padding)
            )

            "detail" -> selectedBook?.let { book ->
                BookDetailView(
                    book = book,
                    onEdit = {
                        formTitle = book.title
                        formAuthor = book.author
                        formPublisher = book.publisher
                        formYear = book.year
                        formCategory = book.category
                        formCoverPath = book.coverPath
                        currentView = "edit"
                    },
                    onDelete = {
                        showDeleteDialog = true
                    },
                    modifier = Modifier.padding(padding)
                )
            }

            "add", "edit" -> AddEditBookView(
                isEdit = currentView == "edit",
                title = formTitle,
                author = formAuthor,
                publisher = formPublisher,
                year = formYear,
                category = formCategory,
                coverPath = formCoverPath,
                onTitleChange = { formTitle = it },
                onAuthorChange = { formAuthor = it },
                onPublisherChange = { formPublisher = it },
                onYearChange = { formYear = it },
                onCategoryChange = { formCategory = it },
                onCoverPathChange = { formCoverPath = it },
                onSave = {
                    if (formTitle.isNotEmpty() && formAuthor.isNotEmpty()) {
                        if (currentView == "add") {
                            val newBook = Book(
                                title = formTitle,
                                author = formAuthor,
                                publisher = formPublisher,
                                year = formYear,
                                category = formCategory,
                                coverPath = formCoverPath
                            )
                            val localId = database.insertBook(newBook)
                            showNotif("✓ Buku berhasil ditambahkan!")

                            // Try to sync to server in background
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val service = createBookService()
                                    val payload = mapOf<String, Any?>(
                                        "title" to newBook.title,
                                        "author" to newBook.author,
                                        "publicationYear" to newBook.year.toIntOrNull(),
                                        // quantity not present in local DB; default to 1
                                        "quantity" to 1,
                                        // send kategoriName so backend can find or create category
                                        "kategoriName" to newBook.category
                                    )
                                    val resp = service.createBook(payload)
                                    if (resp.isSuccessful) {
                                        val body = resp.body()
                                        val bukuObj = body?.get("buku") as? Map<*, *>
                                        val serverIdAny = bukuObj?.get("id")
                                        val serverId = when (serverIdAny) {
                                            is Double -> serverIdAny.toInt()
                                            is Int -> serverIdAny
                                            is Long -> serverIdAny.toInt()
                                            else -> null
                                        }
                                        if (serverId != null) {
                                            database.setServerId(localId.toInt(), serverId)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Log or ignore sync error; local DB has the data
                                }
                            }
                        } else {
                            selectedBook?.let { book ->
                                val updatedBook = book.copy(
                                    title = formTitle,
                                    author = formAuthor,
                                    publisher = formPublisher,
                                    year = formYear,
                                    category = formCategory,
                                    coverPath = formCoverPath
                                )
                                database.updateBook(updatedBook)
                                showNotif("✓ Buku berhasil diperbarui!")

                                // Sync update to server if this book has serverId
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val serverId = updatedBook.serverId
                                        if (serverId != null) {
                                            val service = createBookService()
                                            val payload = mapOf<String, Any?>(
                                                "title" to updatedBook.title,
                                                "author" to updatedBook.author,
                                                "publicationYear" to updatedBook.year.toIntOrNull(),
                                                "quantity" to 1,
                                                "kategoriName" to updatedBook.category
                                            )
                                            service.updateBook(serverId, payload)
                                        }
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                }
                            }
                        }
                        refreshBooks()
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
            title = { Text("Hapus Buku") },
            text = { Text("Yakin ingin menghapus buku ini?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedBook?.let { book ->
                        // attempt to delete on server if serverId exists
                        val serverId = book.serverId
                        if (serverId != null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val service = createBookService()
                                    service.deleteBook(serverId)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }
                        }

                        database.deleteBook(book.id)
                        refreshBooks()
                        showNotif("✓ Buku berhasil dihapus!")
                        currentView = "list"
                        selectedBook = null
                    }
                    showDeleteDialog = false
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListView(
    books: List<Book>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Cari judul atau penulis...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(books) { book ->
                BookItem(book = book, onClick = { onBookClick(book) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun BookItem(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (book.coverPath.isNotEmpty() && File(book.coverPath).exists()) {
                    val bitmap = remember(book.coverPath) {
                        BitmapFactory.decodeFile(book.coverPath)
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Book Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.Top)
            ) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.author,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${book.publisher} • ${book.year}",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (book.category.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = book.category,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookDetailView(
    book: Book,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (book.coverPath.isNotEmpty() && File(book.coverPath).exists()) {
                    val bitmap = remember(book.coverPath) {
                        BitmapFactory.decodeFile(book.coverPath)
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Book Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = book.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(label = "Penulis", value = book.author)
            DetailRow(label = "Penerbit", value = book.publisher)
            DetailRow(label = "Tahun", value = book.year)
            DetailRow(label = "Kategori", value = book.category)

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus")
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    if (value.isNotEmpty()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray,
                fontSize = 14.sp
            )
            Text(
                text = value,
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBookView(
    isEdit: Boolean,
    title: String,
    author: String,
    publisher: String,
    year: String,
    category: String,
    coverPath: String,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onPublisherChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onCoverPathChange: (String) -> Unit,
    onSave: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Fiksi", "Non-Fiksi", "Sejarah", "Sains", "Biografi", "Pendidikan", "Religi")

    // Image picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = saveImageToInternalStorage(context, it, "book_cover")
            onCoverPathChange(savedPath)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = if (isEdit) "Edit Buku" else "Tambah Buku Baru",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Cover Preview & Upload
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(150.dp, 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverPath.isNotEmpty() && File(coverPath).exists()) {
                        val bitmap = remember(coverPath) {
                            BitmapFactory.decodeFile(coverPath)
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Cover Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pilih Gambar")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form Fields
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Judul Buku *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = author,
                onValueChange = onAuthorChange,
                label = { Text("Penulis *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = publisher,
                onValueChange = onPublisherChange,
                label = { Text("Penerbit") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = year,
                onValueChange = onYearChange,
                label = { Text("Tahun Terbit") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategori") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                onCategoryChange(cat)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotEmpty() && author.isNotEmpty()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEdit) "Simpan Perubahan" else "Tambah Buku")
            }
        }
    }
}

