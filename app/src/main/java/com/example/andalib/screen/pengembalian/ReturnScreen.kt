package com.example.andalib.screen

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.andalib.data.network.ReturnRequest
import com.example.andalib.screen.Return.ReturnViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Job


// =========================================================
// 1. TEMA DAN KONSTANTA
// =========================================================

val BlueDarkest = Color(0xFF021024)
val BlueDark = Color(0xFF052659)
val BlueMedium = Color(0xFF5483B3)
val BlueSkyLight = Color(0xFFC1E8FF)
val White = Color(0xFFFFFFFF)
val RedError = Color(0xFFE53935)

private val LightColorScheme = lightColorScheme(
    primary = BlueMedium,
    onPrimary = White,
    secondary = BlueDark,
    onSecondary = White,
    background = BlueSkyLight,
    onBackground = BlueDarkest,
    surface = White,
    onSurface = BlueDarkest,
    error = RedError,
    onError = White
)

@Composable
fun AndalibTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColorScheme, content = content)
}

// =========================================================
// 2. MODEL DATA (Hanya model yang diperlukan oleh UI)
// =========================================================

// Model Data untuk Riwayat (Display) - DTO/UI Model
data class ReturnHistoryItem(
    val id: String,
    val title: String,
    val member: String,
    val borrowDate: String,
    val dueDate: String,
    val returnDate: String,
    val fine: String,
    val isLate: Boolean
)

// Model Data untuk Anggota yang dipilih (Digunakan oleh ViewModel)
data class AnggotaUI(val nim: String, val nama: String)
// Model Data untuk Pinjaman Aktif (Digunakan oleh ViewModel)
data class PeminjamanUI(
    val id: Int,
    val judulBuku: String,
    val tglPinjam: String, // Format dd/MM/yyyy
    val jatuhTempo: String // Format dd/MM/yyyy
)

// Data Riwayat Awal (initialHistoryList) telah dihapus total

// =========================================================
// 3. LOGIKA PEMBANTU
// =========================================================

private const val FINE_PER_DAY = 3000

@SuppressLint("ConstantLocale")
@Stable
val dateFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

fun calculateFine(dueDateStr: String, returnDateStr: String): Int {
    try {
        val dueDate = dateFormat.parse(dueDateStr) ?: return 0
        val returnDate = dateFormat.parse(returnDateStr) ?: return 0

        if (returnDate.before(dueDate) || returnDate == dueDate) {
            return 0
        }

        val diffInMillies = returnDate.time - dueDate.time
        val diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS)

        return (diffInDays * FINE_PER_DAY).toInt()
    } catch (_: Exception) {
        return 0
    }
}

fun formatRupiah(amount: Int): String {
    val idLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()
    val formatter = java.text.NumberFormat.getCurrencyInstance(idLocale)
    return formatter.format(amount).replace("Rp", "Rp.").trim()
}


// =========================================================
// 4. KOMPONEN PEMBANTU UI
// ... (Komponen pembantu UI tidak diulang untuk menghemat ruang, asumsikan sudah benar)
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarHeader(
    title: String,
    onBackClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(text = title, color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueDark),
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    )
}

@Composable
fun SummaryCard(total: Int, late: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(label = "Total Pengembalian", value = total, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            StatItem(label = "Terlambat", value = late, isLate = true, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatItem(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    isLate: Boolean = false
) {
    val (bgColor, textColor) = if (isLate) { MaterialTheme.colorScheme.error.copy(alpha = 0.1f) to MaterialTheme.colorScheme.error } else { MaterialTheme.colorScheme.background to MaterialTheme.colorScheme.onSurface }
    val titleColor = if (isLate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = titleColor)
        Text(value.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)
    }
}

@Composable
fun HistoryItem(
    item: ReturnHistoryItem,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (item.isLate) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Terlambat",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "terlambat",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Peminjam: ${item.member}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            DataRow(label = "Tanggal Pinjam", value = item.borrowDate)
            DataRow(label = "Jatuh Tempo", value = item.dueDate)
            DataRow(label = "Dikembalikan", value = item.returnDate)

            if (item.isLate) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)).padding(8.dp)) {
                    Text("Denda Keterlambatan", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    Text(item.fine, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                // 1. Tombol EDIT
                Button(
                    onClick = { onEdit(item.id) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.sizeIn(minWidth = 70.dp, minHeight = 40.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 2. Tombol HAPUS
                Button(
                    onClick = { onDelete(item.id) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.sizeIn(minWidth = 70.dp, minHeight = 40.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus")
                }
            }
        }
    }
}

@Composable
fun DataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$label:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun UploadProofSection(onUploadClick: () -> Unit, proofUri: Uri?, proofFileName: String?) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bukti Kerusakan (Opsional)", fontWeight = FontWeight.SemiBold)
        Button(
            onClick = onUploadClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.Upload, contentDescription = "Upload")
            Spacer(modifier = Modifier.width(8.dp))
            val buttonText = when {
                proofUri != null -> "Ganti Bukti (${proofFileName ?: "File Terpilih"})"
                else -> "Upload Bukti"
            }
            Text(buttonText, fontWeight = FontWeight.Bold)
        }

        if (proofUri != null) {
            Text(
                "File terpilih: ${proofFileName ?: proofUri.lastPathSegment}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    initialDate: Long,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
    val selectedDate = datePickerState.selectedDateMillis?.let {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
    } ?: ""

    DatePickerDialog(
        onDismissRequest = onDismiss,  shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(horizontal = 32.dp),
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(selectedDate)
                    onDismiss()
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    itemId: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(horizontal = 32.dp),
        icon = { Icon(Icons.Filled.QuestionMark, contentDescription = "Hapus", modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onSurface) },
        text = { Text("Apakah anda yakin ingin menghapus data pengembalian ${itemId ?: "ini"}?", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface) },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) { Text("Iya", color = MaterialTheme.colorScheme.onError) }
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) { Text("Tidak", color = MaterialTheme.colorScheme.onPrimary) }
            }
        },
        dismissButton = { /* Dikosongkan */ }
    )
}

@Composable
fun EditHistoryCard(item: ReturnHistoryItem) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(item.member, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            DataRow(label = "Tanggal Pinjam", value = item.borrowDate)
            DataRow(label = "Jatuh Tempo", value = item.dueDate)
        }
    }
}


// =========================================================
// 5. NAVIGASI DAN ANIMASI
// =========================================================

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(route = "home", title = "Home", icon = Icons.Filled.Home)
    object Books : BottomNavItem(route = "books", title = "Buku", icon = Icons.Filled.Book)
    object Borrowing : BottomNavItem(route = "borrowing", title = "Peminjaman", icon = Icons.Filled.ShoppingCart)
    object Return : BottomNavItem(route = "return", title = "Pengembalian", icon = Icons.Filled.AssignmentReturned)
    object Members : BottomNavItem(route = "members", title = "Anggota", icon = Icons.Filled.People)
}

val bottomNavItems = listOf(
    BottomNavItem.Home, BottomNavItem.Books, BottomNavItem.Borrowing,
    BottomNavItem.Return, BottomNavItem.Members
)

object NavAnimations {
    private const val DURATION = 300
    fun detailEnter(): EnterTransition = slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(DURATION)) + fadeIn(animationSpec = tween(DURATION))
    fun detailExit(): ExitTransition = slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(DURATION)) + fadeOut(animationSpec = tween(DURATION))
    fun detailPopEnter(): EnterTransition = slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(DURATION)) + fadeIn(animationSpec = tween(DURATION))
    fun detailPopExit(): ExitTransition = slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(DURATION)) + fadeOut(animationSpec = tween(DURATION))
    fun tabEnter(): EnterTransition = fadeIn(animationSpec = tween(DURATION))
    fun tabExit(): ExitTransition = fadeOut(animationSpec = tween(DURATION))
}

@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Scaffold(topBar = { AppBarHeader(title = title) }) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(text = "Halaman $title", style = MaterialTheme.typography.headlineMedium)
        }
    }
}


// =========================================================
// 6. FUNGSI UTAMA UNTUK MODUL PENGEMBALIAN (ReturnScreen)
// =========================================================

@Composable
fun ReturnScreen(viewModel: ReturnViewModel = viewModel()) {
    // historyList diinisialisasi sebagai daftar kosong dan akan diisi dari API nanti
    var historyList by remember { mutableStateOf(emptyList<ReturnHistoryItem>()) }
    var showDeleteDialogForId by remember { mutableStateOf<String?>(null) }

    val deleteItem: (String) -> Unit = { idToDelete ->
        historyList = historyList.filter { it.id != idToDelete }
        showDeleteDialogForId = null
        // TODO: Panggil API delete di sini
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "return_list",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("return_list", enterTransition = { NavAnimations.tabEnter() }, exitTransition = { NavAnimations.tabExit() }) {
            ReturnListContent(
                historyList = historyList,
                onAddClick = {
                    viewModel.setSelectedMember(null)
                    navController.navigate("return_add")
                },
                onEditClick = { id -> navController.navigate("return_edit/$id") },
                onDeleteClick = { id -> showDeleteDialogForId = id }
            )
        }

        composable("return_add", enterTransition = { NavAnimations.detailEnter() }, exitTransition = { NavAnimations.detailExit() }, popEnterTransition = { NavAnimations.detailPopEnter() }, popExitTransition = { NavAnimations.detailPopExit() }) {
            ReturnAddContent(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onDataAdded = { newItem ->
                    historyList = listOf(newItem) + historyList
                    navController.popBackStack()
                }
            )
        }

        composable("return_edit/{id}", enterTransition = { NavAnimations.detailEnter() }, exitTransition = { NavAnimations.detailExit() }, popEnterTransition = { NavAnimations.detailPopEnter() }, popExitTransition = { NavAnimations.detailPopExit() }) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("id") ?: return@composable
            val selectedItem = historyList.find { it.id == itemId }

            ReturnEditContent(
                viewModel = viewModel,
                selectedItem = selectedItem,
                onBackClick = { navController.popBackStack() },
                onUpdate = { updatedItem ->
                    historyList = historyList.map { if (it.id == updatedItem.id) updatedItem else it }
                    navController.popBackStack()
                }
            )
        }
    }

    if (showDeleteDialogForId != null) {
        DeleteConfirmationDialog(
            onDismiss = { showDeleteDialogForId = null },
            onConfirm = { deleteItem(showDeleteDialogForId!!) },
            itemId = showDeleteDialogForId
        )
    }
}

@Composable
private fun ReturnListContent(
    historyList: List<ReturnHistoryItem>,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Scaffold(
        topBar = { AppBarHeader(title = "Pengembalian Buku") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(0.dp))
                Text("Ringkasan Pengembalian", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                SummaryCard(total = historyList.size, late = historyList.count { it.isLate })
                Spacer(modifier = Modifier.height(16.dp))
                Text("Riwayat Pengembalian", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(historyList) { item ->
                HistoryItem(
                    item = item,
                    onEdit = onEditClick,
                    onDelete = onDeleteClick
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// =========================================================
// 7. FUNGSI INPUT PENGEMBALIAN BARU (ReturnAddContent)
//    - Menggunakan API Search Flow
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReturnAddContent(
    viewModel: ReturnViewModel,
    onBackClick: () -> Unit,
    onDataAdded: (ReturnHistoryItem) -> Unit
) {
    val calendar = Calendar.getInstance()
    val today = dateFormat.format(Date())

    // --- STATE DARI VIEWMODEL ---
    val memberSearchResults by viewModel.memberSearchResults.collectAsState() // <-- Hasil pencarian dari API
    val selectedMember by viewModel.selectedMember.collectAsState()
    val activeLoans by viewModel.activeLoans.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val transactionStatus by viewModel.transactionStatus.collectAsState()

    // --- STATE LOKAL ---
    var searchQuery by remember { mutableStateOf(selectedMember?.let { "${it.nama} (${it.nim})" } ?: "") }
    var selectedLoan by remember { mutableStateOf<PeminjamanUI?>(null) }
    var returnDateStr by remember { mutableStateOf(today) }
    var dendaAmount by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    // --- LOGIKA PENCARIAN ANGGOTA (API) ---
    LaunchedEffect(searchQuery) {
        // Reset selectedMember dan selectedLoan jika query diubah setelah anggota dipilih
        if (selectedMember != null && searchQuery != "${selectedMember!!.nama} (${selectedMember!!.nim})") {
            viewModel.setSelectedMember(null)
            selectedLoan = null
        }

        // Debouncing untuk mencegah terlalu banyak panggilan API saat mengetik
        var searchJob: Job? = null
        searchJob?.cancel()
        if (searchQuery.length >= 2) {
            viewModel.searchMembers(searchQuery) // Panggil API search
        } else if (searchQuery.length < 2) {
            viewModel.searchMembers("") // Membersihkan hasil
        }
    }

    // --- LOGIKA DENDA & SUBMIT ---
    LaunchedEffect(selectedLoan, returnDateStr) {
        if (selectedLoan != null) {
            dendaAmount = calculateFine(selectedLoan!!.jatuhTempo, returnDateStr)
        } else {
            dendaAmount = 0
        }
    }

    // Efek samping: Tangani hasil submit dari ViewModel
    LaunchedEffect(transactionStatus) {
        if (transactionStatus == true) {
            val isLate = dendaAmount > 0
            val newItem = ReturnHistoryItem(
                id = UUID.randomUUID().toString(), // Di real API, ID harus dari response
                title = selectedLoan?.judulBuku ?: "Buku Tak Dikenal",
                member = selectedMember?.nama ?: "Anggota Tak Dikenal",
                borrowDate = selectedLoan?.tglPinjam ?: today,
                dueDate = selectedLoan?.jatuhTempo ?: today,
                returnDate = returnDateStr,
                fine = formatRupiah(dendaAmount),
                isLate = isLate
            )
            viewModel.resetTransactionStatus()
            onDataAdded(newItem)
        } else if (transactionStatus == false) {
            // TODO: Tampilkan error Snackbar/Toast
            println("Gagal submit pengembalian!")
            viewModel.resetTransactionStatus()
        }
    }

    val onSubmit = {
        if (selectedLoan != null && selectedMember != null) {
            val request = ReturnRequest(
                loanId = selectedLoan!!.id,
                returnDate = returnDateStr,
                fineAmount = dendaAmount,
                note = note
            )
            viewModel.submitReturn(request)
        }
    }

    Scaffold(
        topBar = { AppBarHeader(title = "Input Pengembalian Baru", onBackClick = onBackClick) }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- STEP 1: CARI ANGGOTA ---
                item {
                    Text("1. Cari Anggota Peminjam", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueDark)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { newValue ->
                            searchQuery = newValue
                        },
                        label = { Text("Nama atau NIM Anggota") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true
                    )

                    // Dropdown Hasil Pencarian
                    if (selectedMember == null && searchQuery.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                if (memberSearchResults.isEmpty() && searchQuery.length >= 2) {
                                    Text("Anggota tidak ditemukan", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
                                } else {
                                    memberSearchResults.forEach { member ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setSelectedMember(member)
                                                    searchQuery = "${member.nama} (${member.nim})"
                                                    selectedLoan = null
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Text("${member.nama} (${member.nim})", fontWeight = FontWeight.SemiBold)
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }

                // --- STEP 2: PILIH BUKU AKTIF ---
                if (selectedMember != null) {
                    item {
                        Text("2. Pilih Buku yang Dikembalikan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueDark)

                        if (activeLoans.isEmpty()) {
                            Text("Anggota ini tidak memiliki peminjaman aktif.", color = RedError, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            var bookSearchQuery by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = bookSearchQuery,
                                onValueChange = { bookSearchQuery = it },
                                label = { Text("Filter Judul Buku...") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                leadingIcon = { Icon(Icons.Default.Book, null) }
                            )

                            val filteredActiveLoans = activeLoans.filter {
                                it.judulBuku.contains(bookSearchQuery, ignoreCase = true)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredActiveLoans.forEach { loan ->
                                    val isSelected = selectedLoan?.id == loan.id
                                    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                                    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedLoan = loan },
                                        colors = CardDefaults.cardColors(containerColor = bgColor),
                                        border = BorderStroke(1.dp, borderColor)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.onSurface)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(loan.judulBuku, fontWeight = FontWeight.SemiBold)
                                                Text("Jatuh Tempo: ${loan.jatuhTempo}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                            }
                                            if (isSelected) {
                                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- STEP 3: INPUT TANGGAL & DENDA ---
                if (selectedLoan != null) {
                    item {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("3. Input Data Pengembalian", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueDark)

                        // Tanggal Pinjam & Jatuh Tempo (Read Only)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedTextField(value = selectedLoan!!.tglPinjam, onValueChange = {}, label = { Text("Tgl Pinjam") }, readOnly = true, modifier = Modifier.weight(1f).padding(end = 4.dp))
                            OutlinedTextField(value = selectedLoan!!.jatuhTempo, onValueChange = {}, label = { Text("Jatuh Tempo") }, readOnly = true, modifier = Modifier.weight(1f).padding(start = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Tanggal Dikembalikan (Date Picker)
                        OutlinedTextField(
                            value = returnDateStr,
                            onValueChange = {},
                            label = { Text("Tanggal Dikembalikan") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Input Keterangan/Note
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Keterangan (Contoh: Buku rusak, dll.)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Info Denda
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (dendaAmount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (dendaAmount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    null,
                                    tint = if (dendaAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = if (dendaAmount > 0) "Terlambat! (${dendaAmount / FINE_PER_DAY} Hari)" else "Tepat Waktu",
                                        fontWeight = FontWeight.Bold,
                                        color = if (dendaAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Total Denda: ${formatRupiah(dendaAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }

                        // Tombol Submit
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onSubmit,
                            enabled = selectedLoan != null && !isLoading,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueDark)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Filled.Save, contentDescription = "Simpan")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simpan Pengembalian", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val initialDateMillis = try {
            dateFormat.parse(returnDateStr)?.time ?: calendar.timeInMillis
        } catch (e: Exception) {
            calendar.timeInMillis
        }

        SimpleDatePickerDialog(
            initialDate = initialDateMillis,
            onDateSelected = { newDate -> returnDateStr = newDate },
            onDismiss = { showDatePicker = false }
        )
    }
}


// =========================================================
// 8. FUNGSI EDIT PENGEMBALIAN BARU (ReturnEditContent)
// ... (Fungsi ini tidak memerlukan perubahan besar karena hanya memproses satu item)
// =========================================================

@Composable
private fun ReturnEditContent(
    viewModel: ReturnViewModel,
    selectedItem: ReturnHistoryItem?,
    onBackClick: () -> Unit,
    onUpdate: (ReturnHistoryItem) -> Unit
) {
    if (selectedItem == null) {
        onBackClick()
        return
    }

    val calendar = Calendar.getInstance()

    // --- STATE DARI VIEWMODEL ---
    val isLoading by viewModel.isLoading.collectAsState()
    val transactionStatus by viewModel.transactionStatus.collectAsState()

    // --- STATE LOKAL ---
    var returnDateStr by remember { mutableStateOf(selectedItem.returnDate) }
    var dendaAmount by remember { mutableIntStateOf(calculateFine(selectedItem.dueDate, selectedItem.returnDate)) }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var proofUri by remember { mutableStateOf<Uri?>(null) }
    var proofFileName by remember { mutableStateOf<String?>(null) }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                proofUri = uri
                proofFileName = "bukti_kerusakan_${UUID.randomUUID().toString().take(4)}.jpg"
            }
        }
    )

    // --- LOGIKA UTAMA ---
    LaunchedEffect(returnDateStr) {
        dendaAmount = calculateFine(selectedItem.dueDate, returnDateStr)
    }

    // Efek samping: Tangani hasil update dari ViewModel
    LaunchedEffect(transactionStatus) {
        if (transactionStatus == true) {
            val isLate = dendaAmount > 0
            val updatedItem = selectedItem.copy(
                returnDate = returnDateStr,
                fine = formatRupiah(dendaAmount),
                isLate = isLate
            )
            viewModel.resetTransactionStatus()
            onUpdate(updatedItem) // Pindah ke halaman list
        } else if (transactionStatus == false) {
            // TODO: Tampilkan error Snackbar/Toast (Gagal update)
            println("Gagal update pengembalian!")
            viewModel.resetTransactionStatus()
        }
    }

    val onUpdateClick = {
        val request = ReturnRequest(
            loanId = 0, // ID pinjaman yang sebenarnya harus dikelola dari riwayat
            returnDate = returnDateStr,
            fineAmount = dendaAmount,
            note = note,
            proofUri = proofUri?.toString()
        )
        // Panggil method API update di ViewModel
        viewModel.updateReturn(selectedItem.id, request)
    }

    Scaffold(
        topBar = { AppBarHeader(title = "Edit Pengembalian", onBackClick = onBackClick) }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(0.dp))
                    Text("Detail Peminjaman", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueDark)
                    EditHistoryCard(item = selectedItem)
                }

                // --- FIELD YANG BISA DIEDIT ---
                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Data Pengembalian", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueDark)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = returnDateStr,
                        onValueChange = {},
                        label = { Text("Tanggal Dikembalikan") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Keterangan/Catatan") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                // Upload Bukti
                item {
                    UploadProofSection(
                        onUploadClick = { imagePickerLauncher.launch("image/*") },
                        proofUri = proofUri,
                        proofFileName = proofFileName
                    )
                }

                // Info Denda
                item {
                    Text("Denda (Otomatis)", fontWeight = FontWeight.SemiBold, color = BlueDark)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (dendaAmount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (dendaAmount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                null,
                                tint = if (dendaAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (dendaAmount > 0) "Terlambat! (${dendaAmount / FINE_PER_DAY} Hari)" else "Tepat Waktu",
                                    fontWeight = FontWeight.Bold,
                                    color = if (dendaAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Total Denda: ${formatRupiah(dendaAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }

                // Tombol Update
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onUpdateClick,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Filled.Edit, contentDescription = "Update")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan Perubahan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val initialDateMillis = try {
            dateFormat.parse(returnDateStr)?.time ?: calendar.timeInMillis
        } catch (e: Exception) {
            calendar.timeInMillis
        }
        SimpleDatePickerDialog(
            initialDate = initialDateMillis,
            onDateSelected = { newDate -> returnDateStr = newDate },
            onDismiss = { showDatePicker = false }
        )
    }
}


// =========================================================
// 9. IMPLEMENTASI APLIKASI UTAMA (AndalibApp)
// =========================================================

@Composable
fun AndalibApp() {
    AndalibTheme {
        val navController = rememberNavController()

        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = bottomNavItems.any { it.route == currentRoute }

                if (showBottomBar) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Return.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomNavItem.Home.route, enterTransition = { NavAnimations.tabEnter() }, exitTransition = { NavAnimations.tabExit() }) { PlaceholderScreen("Home") }
                composable(BottomNavItem.Books.route, enterTransition = { NavAnimations.tabEnter() }, exitTransition = { NavAnimations.tabExit() }) { PlaceholderScreen("Buku") }
                composable(BottomNavItem.Borrowing.route, enterTransition = { NavAnimations.tabEnter() }, exitTransition = { NavAnimations.tabExit() }) { PlaceholderScreen("Peminjaman") }
                composable(BottomNavItem.Members.route, enterTransition = { NavAnimations.tabEnter() }, exitTransition = { NavAnimations.tabExit() }) { PlaceholderScreen("Anggota") }

                composable(BottomNavItem.Return.route, enterTransition = { NavAnimations.tabEnter() }, exitTransition = { NavAnimations.tabExit() }) {
                    ReturnScreen()
                }
            }
        }
    }
}