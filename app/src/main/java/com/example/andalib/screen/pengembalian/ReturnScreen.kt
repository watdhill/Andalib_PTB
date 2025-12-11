package com.example.andalib.screen.pengembalian

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.andalib.data.TokenManager
import com.example.andalib.data.network.ApiService
import com.example.andalib.data.network.ReturnHistoryResponse
import com.example.andalib.data.network.ReturnRequest
import com.example.andalib.data.network.createApiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

// =========================================================
// 1. TEMA DAN KONSTANTA
// =========================================================

val BlueDarkest = Color(0xFF021024)
val BlueDark = Color(0xFF052659)
val BlueMedium = Color(0xFF5483B3)
val BlueSkyLight = Color(0xFFC1E8FF)
val White = Color(0xFFFFFFFF)
val RedError = Color(0xFFE53935)
val FineGreen = Color(0xFF4CAF50)

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
    onError = White,
    surfaceVariant = Color(0xFFF0F0F0)
)

@Composable
fun AndalibTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColorScheme, content = content)
}

// NIM diubah menjadi String agar sesuai dengan format umum
data class Member(val nim: String, val name: String, val email: String)
data class Book(val id: Int, val title: String, val author: String)

// borrowingId menggunakan String (sesuai id peminjaman backend)
data class ActiveBorrowing(
    val borrowingId: String, // ID Peminjaman
    val book: Book,
    val borrowDate: String,
    val dueDate: String
)

// loanId menggunakan String (ID Peminjaman)
data class ReturnHistoryItem(
    val id: String,
    val title: String,
    val member: String,      // Nama Anggota
    val memberNim: String,   // NIM Anggota
    val loanId: String,      // ID Pinjaman
    val borrowDate: String,
    val dueDate: String,
    val returnDate: String,
    val fine: String?,
    val isLate: Boolean,
    val keterangan: String? = null,
    val proofUriString: String? = null
)

// =========================================================
//  BOTTOM NAV
// =========================================================

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(route = "home", title = "Home", icon = Icons.Filled.Home)
    object Books :
        BottomNavItem(route = "books", title = "Buku", icon = Icons.AutoMirrored.Filled.MenuBook)

    object Borrowing :
        BottomNavItem(route = "borrowing", title = "Peminjaman", icon = Icons.Filled.Book)

    object Return :
        BottomNavItem(
            route = "return",
            title = "Pengembalian",
            icon = Icons.AutoMirrored.Filled.AssignmentReturn
        )

    object Members :
        BottomNavItem(route = "members", title = "Anggota", icon = Icons.Filled.People)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Books,
    BottomNavItem.Borrowing,
    BottomNavItem.Return,
    BottomNavItem.Members
)

@Suppress("unused")
object NavAnimations {
    private const val DURATION = 300

    fun detailEnter(): EnterTransition =
        slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(DURATION)) +
                fadeIn(animationSpec = tween(DURATION))

    fun detailExit(): ExitTransition =
        slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(DURATION)) +
                fadeOut(animationSpec = tween(DURATION))

    fun detailPopEnter(): EnterTransition =
        slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(DURATION)) +
                fadeIn(animationSpec = tween(DURATION))

    fun detailPopExit(): ExitTransition =
        slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(DURATION)) +
                fadeOut(animationSpec = tween(DURATION))

    fun tabEnter(): EnterTransition = fadeIn(animationSpec = tween(DURATION))
    fun tabExit(): ExitTransition = fadeOut(animationSpec = tween(DURATION))
}

// =========================================================
// 2. LOGIKA PEMBANTU
// =========================================================

private const val FINE_PER_DAY = 3000

@SuppressLint("ConstantLocale")
@Stable
val dateFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

fun calculateFine(dueDateStr: String, returnDateStr: String): Int {
    return try {
        val dueDate = dateFormat.parse(dueDateStr) ?: return 0
        val returnDate = dateFormat.parse(returnDateStr) ?: return 0

        if (returnDate.before(dueDate) || returnDate == dueDate) {
            0
        } else {
            val diffInMillies = returnDate.time - dueDate.time
            val diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS)
            (diffInDays * FINE_PER_DAY).toInt()
        }
    } catch (e: Exception) {
        Log.e("FineCalc", "Error calculating fine: ${e.message}")
        0
    }
}

fun formatRupiah(amount: Int): String {
    val idLocale = Locale.Builder().setLanguage("in").setRegion("ID").build()
    val formatter = java.text.NumberFormat.getCurrencyInstance(idLocale)
    return formatter.format(amount).replace("Rp", "Rp.").trim()
}

// Helper untuk menghilangkan efek ripple pada Composable
private class NoRippleInteractionSource : MutableInteractionSource {
    override val interactions: kotlinx.coroutines.flow.Flow<androidx.compose.foundation.interaction.Interaction> =
        kotlinx.coroutines.flow.emptyFlow()

    override fun tryEmit(interaction: androidx.compose.foundation.interaction.Interaction) = true
    override suspend fun emit(interaction: androidx.compose.foundation.interaction.Interaction) {}
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
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueDark),
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
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
            StatItem(
                label = "Total Pengembalian",
                value = total,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(16.dp))
            StatItem(
                label = "Terlambat",
                value = late,
                isLate = true,
                modifier = Modifier.weight(1f)
            )
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
    val (bgColor, textColor) =
        if (isLate) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) to MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.background to MaterialTheme.colorScheme.onSurface

    val titleColor =
        if (isLate) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
            Text(
                "Peminjam: ${item.member}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            DataRow(label = "Tanggal Pinjam", value = item.borrowDate)
            DataRow(label = "Jatuh Tempo", value = item.dueDate)
            DataRow(label = "Dikembalikan", value = item.returnDate)

            if (!item.keterangan.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                DataRow(label = "Keterangan", value = item.keterangan!!)
            }

            if (item.isLate) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        .padding(8.dp)
                ) {
                    Text(
                        "Denda Keterlambatan",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    item.fine?.let {
                        Text(
                            it,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {

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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun UploadProofSection(onUploadClick: () -> Unit, proofUri: Uri?, proofFileName: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Bukti Kerusakan (Opsional)", fontWeight = FontWeight.SemiBold)
        Button(
            onClick = onUploadClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ),
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
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(8.dp)
                        )
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = "Bukti",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "File terpilih: ${proofFileName ?: proofUri.lastPathSegment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun StatusSelection(
    isTepatWaktuSelected: Boolean,
    isTerlambatSelected: Boolean,
    onTepatWaktuClick: () -> Unit,
    onTerlambatClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatusButton(
            modifier = Modifier.weight(1f),
            label = "Tepat Waktu",
            iconVector = Icons.Filled.CheckCircle,
            isSelected = isTepatWaktuSelected,
            onClick = onTepatWaktuClick,
            color = FineGreen
        )
        StatusButton(
            modifier = Modifier.weight(1f),
            label = "Terlambat",
            iconVector = Icons.Filled.Warning,
            isSelected = isTerlambatSelected,
            onClick = onTerlambatClick,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun StatusButton(
    modifier: Modifier = Modifier,
    label: String,
    iconVector: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    val borderColor = if (isSelected) color else Color.Transparent
    val backgroundColor =
        if (isSelected) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant

    ElevatedCard(
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = onClick)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(iconVector, contentDescription = label, tint = color, modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                color = color,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
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

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val formatted = SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                        ).format(Date(millis))

                        Log.d("DatePicker", "OK ditekan, tanggal: $formatted")
                        onDateSelected(formatted)
                    }
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
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(horizontal = 32.dp),
        icon = {
            Icon(
                Icons.Filled.QuestionMark,
                contentDescription = "Hapus",
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                "Konfirmasi Penghapusan",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(
                "Apakah anda yakin ingin menghapus data pengembalian ${itemId ?: "ini"}?",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.onError)
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Batal", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
fun SuccessNotification() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = "Sukses",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "Data berhasil ditambahkan",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MemberCard(member: Member, onClick: (Member) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(member) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(member.name.first().toString(), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(member.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "NIM: ${member.nim} | ${member.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Pilih",
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun BorrowingCard(borrowing: ActiveBorrowing, onClick: (ActiveBorrowing) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { NoRippleInteractionSource() },
                indication = null
            ) { onClick(borrowing) }
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    borrowing.book.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Pilih",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                borrowing.book.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            DataRow(label = "Tgl Pinjam", value = borrowing.borrowDate)
            DataRow(label = "Jatuh Tempo", value = borrowing.dueDate)
        }
    }
}

@Composable
fun DatePickerFields(
    borrowDate: String,
    dueDate: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text("Detail Peminjaman", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Tanggal Pinjam",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                OutlinedTextField(
                    value = borrowDate,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Jatuh Tempo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.error
                    )
                )
            }
        }
    }
}

/**
 * Field tanggal versi “bar” (bukan TextField) untuk dipakai dengan DatePickerDialog Android.
 */
@Composable
fun DateField(
    date: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            Icons.Filled.CalendarToday,
            contentDescription = "Tanggal",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(date, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun EditHistoryCard(item: ReturnHistoryItem) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                item.member,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            DataRow(label = "Tanggal Pinjam", value = item.borrowDate)
            DataRow(label = "Jatuh Tempo", value = item.dueDate)
        }
    }
}

// =========================================================
// 4. FUNGSI UTAMA UNTUK MODUL PENGEMBALIAN (ReturnScreen)
// =========================================================

@Composable
fun ReturnScreen() {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService: ApiService = remember { createApiService(tokenManager) }
    val coroutineScope = rememberCoroutineScope()

    var historyList by remember { mutableStateOf(emptyList<ReturnHistoryItem>()) }
    var showDeleteDialogForId by remember { mutableStateOf<String?>(null) }

    // load history dari backend
    LaunchedEffect(Unit) {
        try {
            val response = apiService.getReturnHistory()
            historyList = response.map { it.toUiModel() }
        } catch (e: Exception) {
            Log.e("ReturnScreen", "Error load history: ${e.message}")
        }
    }

    val deleteItem: (String) -> Unit = { idToDelete ->
        coroutineScope.launch {
            try {
                val intId = idToDelete.toIntOrNull()
                if (intId == null) {
                    Log.e("ReturnScreen", "ID pengembalian bukan angka: $idToDelete")
                    showDeleteDialogForId = null
                    return@launch
                }

                val response = apiService.deleteReturn(intId)
                if (response.success) {
                    historyList = historyList.filter { it.id != idToDelete }
                } else {
                    Log.e("ReturnScreen", "Delete gagal di backend: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("ReturnScreen", "Error delete return: ${e.message}")
            } finally {
                showDeleteDialogForId = null
            }
        }
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "return_list",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("return_list") {
            ReturnListContent(
                historyList = historyList,
                onAddClick = { navController.navigate("return_add") },
                onEditClick = { id -> navController.navigate("return_edit/$id") },
                onDeleteClick = { id -> showDeleteDialogForId = id }
            )
        }

        composable("return_add") {
            ReturnAddContent(
                onBackClick = { navController.popBackStack() },
                onDataAdded = { newItem ->
                    historyList = listOf(newItem) + historyList
                    navController.popBackStack()
                }
            )
        }

        composable(route = "return_edit/{id}") { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("id") ?: return@composable
            val selectedItem = historyList.find { it.id == itemId }

            ReturnEditContent(
                selectedItem = selectedItem,
                onBackClick = { navController.popBackStack() },
                onUpdate = { updatedItem ->
                    historyList = historyList.map {
                        if (it.id == updatedItem.id) updatedItem else it
                    }
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

private fun ReturnHistoryResponse.toUiModel(): ReturnHistoryItem {
    return ReturnHistoryItem(
        id = id.toString(),
        title = judulBuku,
        member = namaAnggota,
        memberNim = nimAnggota,
        loanId = peminjamanId.toString(),
        borrowDate = tanggalPinjam,
        dueDate = jatuhTempo,
        returnDate = tanggalPengembalian,
        fine = formatRupiah(denda),
        isLate = denda > 0,
        keterangan = keterangan,
        proofUriString = buktiKerusakanUrl
    )
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
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(0.dp))
                Text(
                    "Pengembalian Buku",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SummaryCard(
                    total = historyList.size,
                    late = historyList.count { it.isLate }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Riwayat Pengembalian",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (historyList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "Kosong",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tidak ada riwayat pengembalian.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                items(historyList) { item ->
                    HistoryItem(
                        item = item,
                        onEdit = onEditClick,
                        onDelete = onDeleteClick
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// =========================================================
// 5. RETURN ADD
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReturnAddContent(
    onBackClick: () -> Unit,
    onDataAdded: (ReturnHistoryItem) -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService: ApiService = remember { createApiService(tokenManager) }
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    var filteredMembers by remember { mutableStateOf<List<Member>>(emptyList()) }
    var isLoadingMembers by remember { mutableStateOf(false) }

    var selectedMember by remember { mutableStateOf<Member?>(null) }
    var selectedBorrowing by remember { mutableStateOf<ActiveBorrowing?>(null) }
    var availableBorrowings by remember { mutableStateOf<List<ActiveBorrowing>>(emptyList()) }
    var isLoadingBorrowings by remember { mutableStateOf(false) }

    var proofUri by remember { mutableStateOf<Uri?>(null) }
    var proofFileName by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }

    val todayMillis = remember { Calendar.getInstance().timeInMillis }
    var returnDate by remember { mutableStateOf(dateFormat.format(Date(todayMillis))) }
    var datePickerTarget by remember { mutableStateOf<String?>(null) }

    val isButtonEnabled = selectedMember != null && selectedBorrowing != null

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                proofUri = uri
                proofFileName = "bukti_kerusakan_${UUID.randomUUID().toString().take(4)}.jpg"
            }
        }
    )

    // 1. PENCARIAN ANGGOTA
    LaunchedEffect(searchQuery) {
        searchJob?.cancel()

        if (searchQuery.length < 2) {
            filteredMembers = emptyList()
            isLoadingMembers = false
            return@LaunchedEffect
        }

        searchJob = coroutineScope.launch {
            delay(500)
            isLoadingMembers = true
            try {
                val result = apiService.searchMembers(searchQuery)
                filteredMembers = result.map {
                    Member(
                        nim = it.nim,
                        name = it.nama,
                        email = it.email ?: "-"
                    )
                }
            } catch (e: Exception) {
                Log.e("API_SEARCH_ERROR", "Gagal mengambil data anggota: ${e.message}")
                filteredMembers = emptyList()
            } finally {
                isLoadingMembers = false
            }
        }
    }

    // 2. AMBIL PEMINJAMAN AKTIF
    LaunchedEffect(selectedMember) {
        availableBorrowings = emptyList()
        selectedBorrowing = null

        val member = selectedMember ?: return@LaunchedEffect
        isLoadingBorrowings = true
        try {
            val loans = apiService.fetchActiveLoans(member.nim)
            availableBorrowings = loans.map { loan ->
                ActiveBorrowing(
                    borrowingId = loan.id.toString(),
                    book = Book(
                        id = loan.id,
                        title = loan.judulBuku,
                        author = loan.author
                    ),
                    borrowDate = loan.tglPinjam,
                    dueDate = loan.jatuhTempo
                )
            }
        } catch (e: Exception) {
            Log.e("ReturnAddContent", "Error fetching borrowings: ${e.message}")
            availableBorrowings = emptyList()
        } finally {
            isLoadingBorrowings = false
        }
    }

    // 3. SUBMIT PENGEMBALIAN
    val handleSubmit: () -> Unit = handleSubmit@{
        // Pastikan member & borrowing sudah dipilih
        if (selectedBorrowing == null || selectedMember == null) {
            return@handleSubmit
        }

        val finalDueDate = selectedBorrowing!!.dueDate
        val finalBorrowDate = selectedBorrowing!!.borrowDate
        val fineAmount = calculateFine(finalDueDate, returnDate)

        coroutineScope.launch {
            try {
                val request = ReturnRequest(
                    peminjamanId = selectedBorrowing!!.borrowingId.toInt(),
                    tanggalPengembalian = returnDate,
                    denda = fineAmount,
                    buktiKerusakanUrl = proofUri?.toString(),
                    keterangan = description.takeIf { it.isNotBlank() }
                )

                val response = apiService.submitReturn(request)

                if (response.success) {
                    val newItem = ReturnHistoryItem(
                        id = UUID.randomUUID().toString(),
                        title = selectedBorrowing!!.book.title,
                        member = selectedMember!!.name,
                        memberNim = selectedMember!!.nim,
                        loanId = selectedBorrowing!!.borrowingId,
                        borrowDate = finalBorrowDate,
                        dueDate = finalDueDate,
                        returnDate = returnDate,
                        fine = formatRupiah(fineAmount),
                        isLate = fineAmount > 0,
                        keterangan = description.takeIf { it.isNotBlank() },
                        proofUriString = proofUri?.toString()
                    )
                    onDataAdded(newItem)
                    showSuccess = true
                } else {
                    Log.e("ReturnAddContent", "Submit gagal: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("ReturnAddContent", "Error submit pengembalian: ${e.message}")
            }
        }
    }


    Scaffold(
        topBar = { AppBarHeader(title = "Tambah Pengembalian", onBackClick = onBackClick) },
        bottomBar = {
            Button(
                onClick = handleSubmit,
                enabled = isButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    "Tambah Pengembalian",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showSuccess) {
                item { SuccessNotification() }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))

                Text("Pilih Anggota", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cari NIM/Nama Anggota") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cari") }
                )
            }

            if (selectedMember != null) {
                item {
                    Text(
                        "Anggota Terpilih:",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    MemberCard(selectedMember!!) {
                        selectedMember = null
                        selectedBorrowing = null
                    }
                }
            } else if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
                if (isLoadingMembers) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mencari anggota...")
                        }
                    }
                } else if (filteredMembers.isEmpty()) {
                    item {
                        Text(
                            "Anggota tidak ditemukan.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    items(filteredMembers) { member ->
                        MemberCard(member = member) {
                            selectedMember = it
                            searchQuery = ""
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pilih Buku yang Dikembalikan", fontWeight = FontWeight.Bold)

                when {
                    selectedMember == null -> {
                        Text(
                            "Pilih anggota terlebih dahulu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    isLoadingBorrowings -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Memuat pinjaman aktif...")
                        }
                    }

                    availableBorrowings.isEmpty() -> {
                        Text(
                            "Tidak ada buku aktif yang sedang dipinjam.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    selectedBorrowing != null -> {
                        BorrowingCard(selectedBorrowing!!) { selectedBorrowing = null }
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableBorrowings.forEach { borrowing ->
                                BorrowingCard(borrowing = borrowing) {
                                    selectedBorrowing = it
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedBorrowing != null) {
                    DatePickerFields(
                        borrowDate = selectedBorrowing!!.borrowDate,
                        dueDate = selectedBorrowing!!.dueDate
                    )
                } else {
                    Text(
                        "Tanggal Pinjam dan Jatuh Tempo akan ditampilkan di sini setelah Anda memilih buku.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Text(
                    "Tanggal Pengembalian",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                DateField(returnDate) { datePickerTarget = "return" }

                if (selectedBorrowing != null) {
                    val fineAmount =
                        calculateFine(selectedBorrowing!!.dueDate, returnDate)
                    val fineColor =
                        if (fineAmount > 0) MaterialTheme.colorScheme.error else FineGreen
                    Text(
                        "Denda Dihitung: ${formatRupiah(fineAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = fineColor,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                UploadProofSection(
                    onUploadClick = { imagePickerLauncher.launch("image/*") },
                    proofUri = proofUri,
                    proofFileName = proofFileName
                )
            }

            item {
                Text("Keterangan (Opsional)", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Masukkan keterangan, contoh: buku robek") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (datePickerTarget != null) {
        val initialTime = try {
            dateFormat.parse(returnDate)?.time ?: todayMillis
        } catch (_: Exception) {
            todayMillis
        }
        SimpleDatePickerDialog(
            initialDate = initialTime,
            onDateSelected = { newDate ->
                returnDate = newDate
            },
            onDismiss = { datePickerTarget = null }
        )
    }
}

// =========================================================
// 6. RETURN EDIT
// =========================================================

@Composable
private fun ReturnEditContent(
    selectedItem: ReturnHistoryItem?,
    onBackClick: () -> Unit,
    onUpdate: (ReturnHistoryItem) -> Unit
) {
    if (selectedItem == null) {
        Scaffold(
            topBar = {
                AppBarHeader(
                    title = "Edit Pengembalian",
                    onBackClick = onBackClick
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Data pengembalian tidak ditemukan.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService: ApiService = remember { createApiService(tokenManager) }
    val coroutineScope = rememberCoroutineScope()

    val itemToEdit = selectedItem

    var returnDate by remember { mutableStateOf(itemToEdit.returnDate) }
    var proofUri by remember { mutableStateOf(itemToEdit.proofUriString?.toUri()) }
    var proofFileName by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf(itemToEdit.keterangan ?: "") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                proofUri = uri
                proofFileName = "bukti_kerusakan_${UUID.randomUUID().toString().take(4)}.jpg"
            }
        }
    )

    val fineAmountInt = remember(returnDate, itemToEdit.dueDate) {
        calculateFine(itemToEdit.dueDate, returnDate)
    }
    val statusTerlambat = fineAmountInt > 0
    val statusTepatWaktu = !statusTerlambat
    val fineAmountText = formatRupiah(fineAmountInt)

    val handleUpdate: () -> Unit = {
        val peminjamanIdInt = itemToEdit.loanId.toIntOrNull()

        if (peminjamanIdInt == null) {
            Log.e("ReturnEdit", "loanId bukan angka: ${itemToEdit.loanId}")
        } else {
            val fineAmount = calculateFine(itemToEdit.dueDate, returnDate)
            val isLate = fineAmount > 0
            val fineText = formatRupiah(fineAmount)

            val updatedItem = itemToEdit.copy(
                fine = fineText,
                isLate = isLate,
                returnDate = returnDate,
                keterangan = description.takeIf { it.isNotBlank() },
                proofUriString = proofUri?.toString()
            )

            coroutineScope.launch {
                try {
                    val request = ReturnRequest(
                        peminjamanId = peminjamanIdInt,
                        tanggalPengembalian = returnDate,
                        denda = fineAmount,
                        buktiKerusakanUrl = proofUri?.toString(),
                        keterangan = description.takeIf { it.isNotBlank() }
                    )

                    val response = apiService.updateReturn(itemToEdit.id, request)
                    if (response.success) {
                        onUpdate(updatedItem)
                    } else {
                        Log.e("ReturnEdit", "Update gagal: ${response.message}")
                    }
                } catch (e: Exception) {
                    Log.e("ReturnEdit", "Error update return: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        topBar = { AppBarHeader(title = "Edit Pengembalian", onBackClick = onBackClick) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                Text("Riwayat Peminjaman", fontWeight = FontWeight.Bold)
                EditHistoryCard(item = itemToEdit)

                DatePickerFields(
                    borrowDate = itemToEdit.borrowDate,
                    dueDate = itemToEdit.dueDate
                )
            }

            item {
                Text(
                    "Tanggal Pengembalian",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                DateField(returnDate) {
                    val currentDate = try {
                        dateFormat.parse(returnDate)
                    } catch (_: Exception) {
                        null
                    }

                    val calendar = Calendar.getInstance().apply {
                        if (currentDate != null) time = currentDate
                    }

                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)

                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val pickedCal = Calendar.getInstance()
                            pickedCal.set(y, m, d)
                            returnDate = dateFormat.format(pickedCal.time)
                        },
                        year,
                        month,
                        day
                    ).show()
                }

                val fineColor =
                    if (fineAmountInt > 0) MaterialTheme.colorScheme.error else FineGreen
                Text(
                    "Denda Dihitung: $fineAmountText",
                    fontWeight = FontWeight.Bold,
                    color = fineColor,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                UploadProofSection(
                    onUploadClick = { imagePickerLauncher.launch("image/*") },
                    proofUri = proofUri,
                    proofFileName = proofFileName
                )
            }

            item {
                Text("Keterangan (Opsional)", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Masukkan keterangan, contoh: buku robek") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }

            item {
                Text("Status Pengembalian", fontWeight = FontWeight.SemiBold)

                StatusSelection(
                    isTepatWaktuSelected = statusTepatWaktu,
                    isTerlambatSelected = statusTerlambat,
                    onTepatWaktuClick = { },
                    onTerlambatClick = { }
                )

                if (statusTerlambat) {
                    val daysLate = try {
                        TimeUnit.DAYS.convert(
                            dateFormat.parse(returnDate)!!.time -
                                    dateFormat.parse(itemToEdit.dueDate)!!.time,
                            TimeUnit.MILLISECONDS
                        )
                    } catch (_: Exception) {
                        0L
                    }
                    Text(
                        "Perhatian: Buku ini terlambat $daysLate hari.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = handleUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        "Simpan Perubahan",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// =========================================================
// 7. NAVIGASI UTAMA APLIKASI
// =========================================================

@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Scaffold(topBar = { AppBarHeader(title = title) }) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Halaman $title", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Suppress("unused")
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
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.title,
                                        tint = if (selected)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = {
                                    Text(
                                        item.title,
                                        color = if (selected)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                composable(
                    BottomNavItem.Home.route,
                    enterTransition = { NavAnimations.tabEnter() },
                    exitTransition = { NavAnimations.tabExit() }
                ) { PlaceholderScreen("Home") }

                composable(
                    BottomNavItem.Books.route,
                    enterTransition = { NavAnimations.tabEnter() },
                    exitTransition = { NavAnimations.tabExit() }
                ) { PlaceholderScreen("Buku") }

                composable(
                    BottomNavItem.Borrowing.route,
                    enterTransition = { NavAnimations.tabEnter() },
                    exitTransition = { NavAnimations.tabExit() }
                ) { PlaceholderScreen("Peminjaman") }

                composable(
                    BottomNavItem.Members.route,
                    enterTransition = { NavAnimations.tabEnter() },
                    exitTransition = { NavAnimations.tabExit() }
                ) { PlaceholderScreen("Anggota") }

                composable(
                    BottomNavItem.Return.route,
                    enterTransition = { NavAnimations.tabEnter() },
                    exitTransition = { NavAnimations.tabExit() }
                ) {
                    ReturnScreen()
                }
            }
        }
    }
}
