package com.example.andalib.screen

import android.content.Context
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val database = remember { BookDatabase(context) }

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
                    containerColor = MaterialTheme.colorScheme.primary,
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
                            database.insertBook(newBook)
                            showNotif("✓ Buku berhasil ditambahkan!")
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
    val categories = listOf("Fiksi", "Non-Fiksi", "Sejarah", "Sains", "Biografi")}