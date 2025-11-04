package com.example.andalib.screen

import android.annotation.SuppressLint
import android.net.Uri // Diperlukan untuk URI file yang diupload
import androidx.activity.compose.rememberLauncherForActivityResult // Diperlukan untuk Activity Result API
import androidx.activity.result.contract.ActivityResultContracts.GetContent // Diperlukan untuk memilih konten
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// =========================================================
// 1. TEMA, WARNA, DAN DATA MODELS
// =========================================================

val BlueDarkest = Color(0xFF021024) // Hampir hitam
val BlueDark = Color(0xFF052659)    // Biru Tua, untuk Top Bar/Bottom Nav Background
val BlueMedium = Color(0xFF5483B3)  // Biru Sedang, untuk Aksen Utama
val BlueLight = Color(0xFF7DA0CA)   // Biru Muda, untuk Outline/Garis
val BlueSkyLight = Color(0xFFC1E8FF) // Biru Langit Sangat Muda, untuk Background Layar
val White = Color(0xFFFFFFFF)       // Putih, untuk Card Background & Teks OnPrimary
val RedError = Color(0xFFE53935)

private val LightColorScheme = lightColorScheme(
    primary = BlueMedium,         // Aksen utama, tombol
    onPrimary = White,            // Teks di atas primary
    secondary = BlueDark,         // Background TopAppBar, BottomAppBar
    onSecondary = White,          // Teks di atas secondary
    background = BlueSkyLight,    // Latar belakang layar keseluruhan
    onBackground = BlueDarkest,   // Teks di atas background
    surface = White,              // Background Card, Dialog
    onSurface = BlueDarkest,      // Teks di atas surface
    error = RedError,             // Warna error, denda
    onError = White               // Teks di atas error
)

@Composable
fun AndalibTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColorScheme, content = content)
}

data class Member(val id: Int, val name: String, val email: String)
data class Book(val id: Int, val title: String, val author: String)
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
    BottomNavItem.Home,
    BottomNavItem.Books,
    BottomNavItem.Borrowing,
    BottomNavItem.Return,
    BottomNavItem.Members
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

val dummyMembers = listOf(
    Member(1, "Rahil Syahputra", "rahil@example.com"),
    Member(2, "Budi Hartono", "budi@example.com"),
    Member(3, "Citra Dewi", "citra@example.com"),
    Member(4, "Dion Satria", "dion@example.com"),
    Member(5, "Fahri Rizal", "fahri@example.com")
)

// =========================================================
// 2. LOGIKA PEMBANTU
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
    } catch (_: Exception) { // FIX: Parameter 'e' diganti dengan '_'
        return 0
    }
}

fun formatRupiah(amount: Int): String {
    val idLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()
    val formatter = java.text.NumberFormat.getCurrencyInstance(idLocale)
    return formatter.format(amount).replace("Rp", "Rp.").trim()
}

// =========================================================
// 3. KOMPONEN PEMBANTU
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarHeader(
    title: String,
    onBackClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(text = title, color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondary),
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
                TextButton(
                    onClick = { onEdit(item.id) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", color = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = { onDelete(item.id) },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
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
            // Dalam aplikasi nyata, Anda dapat menggunakan AsyncImage/Coil untuk menampilkan gambar dari URI di sini.
            Text(
                "File terpilih: ${proofFileName ?: proofUri.lastPathSegment}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun StatusSelection(
    isTepatWaktuSelected: Boolean, isTerlambatSelected: Boolean,
    onTepatWaktuClick: () -> Unit, onTerlambatClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatusButton(modifier = Modifier.weight(1f), label = "Tepat Waktu", iconVector = Icons.Filled.CheckCircle, isSelected = isTepatWaktuSelected, onClick = onTepatWaktuClick, color = MaterialTheme.colorScheme.primary)
        StatusButton(modifier = Modifier.weight(1f), label = "Terlambat", iconVector = Icons.Filled.Warning, isSelected = isTerlambatSelected, onClick = onTerlambatClick, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun StatusButton(
    modifier: Modifier = Modifier, label: String, iconVector: ImageVector,
    isSelected: Boolean, onClick: () -> Unit, color: Color
) {
    val borderColor = if (isSelected) color else Color.Transparent
    val backgroundColor = if (isSelected) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.background

    ElevatedCard(
        modifier = modifier.height(90.dp).clickable(onClick = onClick).border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp), colors = CardDefaults.elevatedCardColors(containerColor = backgroundColor), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(iconVector, contentDescription = label, tint = color, modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, textAlign = TextAlign.Center)
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
        onDismissRequest = onDismiss,
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
fun SuccessNotification() {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, contentDescription = "Sukses", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Data berhasil ditambahkan", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MemberCard(member: Member, onClick: (Member) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(member) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text(member.name.first().toString(), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(member.name, fontWeight = FontWeight.SemiBold)
                Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun BookSelectionCard(book: Book, onClick: (Book) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(book) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.SemiBold)
                Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Icon(Icons.Filled.Book, contentDescription = "Buku", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun DatePickerFields(
    borrowDate: String,
    dueDate: String,
    onBorrowDateClick: () -> Unit,
    onDueDateClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text("Tanggal Pinjam", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
            DateField(borrowDate, onClick = onBorrowDateClick)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text("Jatuh Tempo", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
            DateField(dueDate, onClick = onDueDateClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(date: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(Icons.Filled.CalendarToday, contentDescription = "Tanggal", tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(date, color = MaterialTheme.colorScheme.onSurface)
    }
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
// 4. FUNGSI UTAMA UNTUK MODUL PENGEMBALIAN (ReturnScreen)
// =========================================================

val initialHistoryList = listOf(
    ReturnHistoryItem("1", "Pemrograman Web Lanjut", "Rahil Syahputra", "01/10/2025", "07/10/2025", "08/10/2025", "Rp. 6.000", true),
    ReturnHistoryItem("2", "Dasar Pemrograman Kotlin", "Budi Hartono", "05/10/2025", "15/10/2025", "15/10/2025", "Rp. 0", false),
    ReturnHistoryItem("3", "Algoritma dan Struktur Data", "Citra Dewi", "10/09/2025", "20/09/2025", "25/09/2025", "Rp. 15.000", true),
    ReturnHistoryItem("4", "Jaringan Komputer", "Dion Satria", "20/10/2025", "27/10/2025", "27/10/2025", "Rp. 0", false),
)

@Composable
fun ReturnScreen() {
    var historyList by remember { mutableStateOf(initialHistoryList) }
    var showDeleteDialogForId by remember { mutableStateOf<String?>(null) }

    val deleteItem: (String) -> Unit = { idToDelete ->
        historyList = historyList.filter { it.id != idToDelete }
        showDeleteDialogForId = null
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
                onAddClick = { navController.navigate("return_add") },
                onEditClick = { id -> navController.navigate("return_edit/$id") },
                onDeleteClick = { id -> showDeleteDialogForId = id }
            )
        }

        composable("return_add", enterTransition = { NavAnimations.detailEnter() }, exitTransition = { NavAnimations.detailExit() }, popEnterTransition = { NavAnimations.detailPopEnter() }, popExitTransition = { NavAnimations.detailPopExit() }) {
            ReturnAddContent(onBackClick = { navController.popBackStack() }, onDataAdded = { newItem ->
                historyList = historyList + newItem
                navController.popBackStack()
            })
        }

        composable("return_edit/{id}", enterTransition = { NavAnimations.detailEnter() }, exitTransition = { NavAnimations.detailExit() }, popEnterTransition = { NavAnimations.detailPopEnter() }, popExitTransition = { NavAnimations.detailPopExit() }) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("id") ?: return@composable
            val selectedItem = historyList.find { it.id == itemId }

            ReturnEditContent(
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
                Text("Pengembalian Buku", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReturnAddContent(onBackClick: () -> Unit, onDataAdded: (ReturnHistoryItem) -> Unit) {

    var searchQuery by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    val filteredMembers = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            dummyMembers.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var selectedMember by remember { mutableStateOf<Member?>(null) }
    var selectedBook by remember { mutableStateOf<Book?>(null) }

    val dummyBooks = listOf(
        Book(1, "Pemrograman Web Lanjut", "Smith"),
        Book(2, "Dasar Pemrograman Kotlin", "Jones"),
        Book(3, "Algoritma dan Struktur Data", "Williams")
    )

    val todayMillis = Calendar.getInstance().timeInMillis

    var borrowDate by remember { mutableStateOf(dateFormat.format(Date(todayMillis))) }
    var dueDate by remember { mutableStateOf(dateFormat.format(Date(todayMillis))) }
    var returnDate by remember { mutableStateOf(dateFormat.format(Date(todayMillis))) }

    var datePickerTarget by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { AppBarHeader(title = "Tambah Pengembalian", onBackClick = onBackClick) },
        bottomBar = {
            val isButtonEnabled = selectedMember != null && selectedBook != null
            Button(
                onClick = {
                    val fineAmount = calculateFine(dueDate, returnDate)
                    val newItem = ReturnHistoryItem(
                        id = UUID.randomUUID().toString(),
                        title = selectedBook?.title ?: "Unknown Book",
                        member = selectedMember?.name ?: "Unknown Member",
                        borrowDate = borrowDate,
                        dueDate = dueDate,
                        returnDate = returnDate,
                        fine = formatRupiah(fineAmount),
                        isLate = fineAmount > 0
                    )
                    onDataAdded(newItem)
                    showSuccess = true
                },
                enabled = isButtonEnabled,
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
            ) {
                Text("Tambah Pengembalian", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showSuccess) { item { SuccessNotification() } }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cari Anggota...") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cari") }
                )
            }
            if (selectedMember != null) {
                item {
                    Text("Anggota Terpilih:", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    MemberCard(selectedMember!!) { selectedMember = null }
                }
            }
            if (searchQuery.isNotBlank() && selectedMember == null) {
                items(filteredMembers) { member ->
                    MemberCard(member = member, onClick = { selectedMember = it; searchQuery = "" })
                }
            } else if (searchQuery.isNotBlank() && filteredMembers.isEmpty()) {
                item { Text("Anggota tidak ditemukan.", color = MaterialTheme.colorScheme.error) }
            }

            item {
                Text("Pilih Buku", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                if (selectedBook != null) {
                    BookSelectionCard(selectedBook!!) { selectedBook = null }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dummyBooks.forEach { book ->
                            BookSelectionCard(book = book, onClick = { selectedBook = it })
                        }
                    }
                }
            }

            item {
                DatePickerFields(
                    borrowDate = borrowDate,
                    dueDate = dueDate,
                    onBorrowDateClick = { datePickerTarget = "borrow" },
                    onDueDateClick = { datePickerTarget = "due" }
                )
                Text("Tanggal Pengembalian", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
                DateField(returnDate, onClick = { datePickerTarget = "return" })
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (datePickerTarget != null) {
        SimpleDatePickerDialog(
            initialDate = todayMillis,
            onDateSelected = { newDate ->
                when (datePickerTarget) {
                    "borrow" -> borrowDate = newDate
                    "due" -> dueDate = newDate
                    "return" -> returnDate = newDate
                }
            },
            onDismiss = { datePickerTarget = null }
        )
    }
}

@Composable
private fun ReturnEditContent(selectedItem: ReturnHistoryItem?, onBackClick: () -> Unit, onUpdate: (ReturnHistoryItem) -> Unit) {

    val itemToEdit = selectedItem ?: ReturnHistoryItem(UUID.randomUUID().toString(), "Unknown", "Data Error", "N/A", "N/A", "N/A", "Rp. 0", false)
    val todayMillis = Calendar.getInstance().timeInMillis

    var returnDate by remember { mutableStateOf(itemToEdit.returnDate) }
    var datePickerTarget by remember { mutableStateOf<String?>(null) }

    // STATE UNTUK UPLOAD BUKTI
    var proofUri by remember { mutableStateOf<Uri?>(null) }
    var proofFileName by remember { mutableStateOf<String?>(null) }

    // LAUNCHER UNTUK MEMBUKA GALERI/FILE PICKER
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                proofUri = uri
                // Dalam aplikasi nyata, Anda harus mendapatkan nama file asli.
                // Di sini kita menggunakan placeholder karena tidak ada Context.
                proofFileName = "bukti_kerusakan_${UUID.randomUUID().toString().take(4)}.jpg"
            }
        }
    )

    // Perhitungan Denda Otomatis
    val fineAmountInt by remember(returnDate, itemToEdit.dueDate) {
        mutableIntStateOf(calculateFine(itemToEdit.dueDate, returnDate))
    }

    val statusTerlambat by remember(fineAmountInt) {
        mutableStateOf(fineAmountInt > 0)
    }

    val statusTepatWaktu by remember(statusTerlambat) {
        mutableStateOf(!statusTerlambat)
    }

    val fineAmountText by remember(fineAmountInt) {
        mutableStateOf(formatRupiah(fineAmountInt))
    }

    val handleUpdate = {
        val updatedItem = itemToEdit.copy(
            fine = fineAmountText,
            isLate = statusTerlambat,
            returnDate = returnDate
            // URI/Filename bukti tidak disimpan di sini karena model data ReturnHistoryItem tidak memilikinya.
            // Anda perlu menambahkan properti 'proofUri' ke data class ReturnHistoryItem jika ingin menyimpannya.
        )
        onUpdate(updatedItem)
    }

    Scaffold(
        topBar = { AppBarHeader(title = "Edit Pengembalian", onBackClick = onBackClick) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(0.dp))
                Text("Riwayat Pengembalian", fontWeight = FontWeight.Bold)
                EditHistoryCard(item = itemToEdit)
            }
            item {
                Text("Tanggal Pengembalian", fontWeight = FontWeight.SemiBold)
                DateField(returnDate, onClick = { datePickerTarget = "return" })
            }
            item {
                UploadProofSection(
                    onUploadClick = {
                        imagePickerLauncher.launch("image/*") // Memicu pemilih file/galeri
                    },
                    proofUri = proofUri,
                    proofFileName = proofFileName
                )
            }
            item {
                Text("Status Pengembalian", fontWeight = FontWeight.SemiBold)

                StatusSelection(
                    isTepatWaktuSelected = statusTepatWaktu,
                    isTerlambatSelected = statusTerlambat,
                    onTepatWaktuClick = { /* Dibiarkan kosong */ },
                    onTerlambatClick = { /* Dibiarkan kosong */ }
                )

                if (statusTerlambat) {
                    val daysLate = TimeUnit.DAYS.convert(dateFormat.parse(returnDate)!!.time - dateFormat.parse(itemToEdit.dueDate)!!.time, TimeUnit.MILLISECONDS)
                    Text(
                        "Perhatian: Buku ini terlambat $daysLate hari.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            item {
                Text("Denda (Otomatis)", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = fineAmountText,
                    onValueChange = { /* Diabaikan karena otomatis */ },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    leadingIcon = { Icon(Icons.Filled.Money, contentDescription = "Denda") }
                )
            }
            item {
                Button(
                    onClick = handleUpdate,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                ) {
                    Text("Simpan Perubahan", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (datePickerTarget != null) {
        SimpleDatePickerDialog(
            initialDate = todayMillis,
            onDateSelected = { newDate ->
                returnDate = newDate
            },
            onDismiss = { datePickerTarget = null }
        )
    }
}


// =========================================================
// 5. NAVIGASI APLIKASI UTAMA (AndalibApp)
// =========================================================

@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Scaffold(topBar = { AppBarHeader(title = title) }) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(text = "Halaman $title", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

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