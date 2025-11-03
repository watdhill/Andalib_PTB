package com.example.andalib.screen.member // Pastikan package benar

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.andalib.R // Pastikan import R ada

// Impor helper yang benar dari Utils.kt
import com.example.andalib.createImageFile
import com.example.andalib.saveImageToInternalStorage

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Data Fakultas dan Jurusan
private val facultyMajorsMap: Map<String, List<String>> = mapOf(
    "Fakultas Hukum" to listOf("Ilmu Hukum"),
    "Fakultas Pertanian" to listOf("Agribisnis", "Agroteknologi", "Ilmu Tanah", "Proteksi tanaman", "Penyuluhan Pertanian", "Agroekoteknologi"),
    "Fakultas Kedokteran" to listOf("Pendidikan dokter", "Kebidanan", "Psikologi", "Ilmu Biomedis"),
    "Fakultas Matematika & Ilmu Pengetahuan Alam" to listOf("Kimia", "Biologi", "Matematika", "Fisika"),
    "Fakultas Ekonomi & Bisnis" to listOf("Ekonomi", "Manajemen", "Akuntansi"),
    "Fakultas Peternakan" to listOf("Peternakan"),
    "Fakultas Ilmu Budaya" to listOf("Ilmu Sejarah", "Sastra Inggris", "Sastra Indonesia", "Sastra Minangkabau", "Sastra Jepang"),
    "Fakultas Ilmu Sosial & Ilmu Politik" to listOf("Ilmu Politik", "Sosiologi", "Antropologi Sosial", "Ilmu Hubungan Internasional", "Ilmu Komunikasi", "Administrasi Publik"),
    "Fakultas Teknik" to listOf("Teknik Mesin", "Teknik Sipil", "Teknik Lingkungan", "Teknik Industri", "Teknik Elektro"),
    "Fakultas Farmasi" to listOf("Farmasi"),
    "Fakultas Teknologi Pertanian" to listOf("Teknologi pangan dan Hasil Pertanian", "Teknik Pertanian dan biosistem", "Teknologi Industri Pertanian"),
    "Fakultas Kesehatan Masyarakat" to listOf("Kesehatan Masyarakat", "Gizi"),
    "Fakultas Keperawatan" to listOf("Keperawatan"),
    "Fakultas Kedokteran Gigi" to listOf("Pendidikan Dokter Gigi"),
    "Fakultas Teknologi Informasi" to listOf("Sistem Informasi", "Informatika", "Teknik Komputer")
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen() {
    val context = LocalContext.current
    val database = remember { MemberDatabase(context) }

    var members by remember { mutableStateOf(emptyList<Member>()) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        Log.d("MembersScreen", "Memuat ulang data anggota...")
        members = database.getAllMembers()
    }

    var searchQuery by remember { mutableStateOf("") }
    var currentView by remember { mutableStateOf("list") }
    var selectedMember by remember { mutableStateOf<Member?>(null) }
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // State untuk form
    var formName by remember { mutableStateOf("") }
    var formNim by remember { mutableStateOf("") }
    var formGender by remember { mutableStateOf("") }
    var formFaculty by remember { mutableStateOf("") }
    var formMajor by remember { mutableStateOf("") }
    var formContact by remember { mutableStateOf("") }
    var formEmail by remember { mutableStateOf("") }
    var formPhotoPath by remember { mutableStateOf("") }

    val filteredMembers = if (searchQuery.isEmpty()) {
        members
    } else {
        database.searchMembers(searchQuery)
    }

    fun refreshMembersView() {
        refreshTrigger++
    }

    fun showNotif(message: String) {
        notificationMessage = message
        showNotification = true
    }

    fun resetForm() {
        formName = ""
        formNim = ""
        formGender = ""
        formFaculty = ""
        formMajor = ""
        formContact = ""
        formEmail = ""
        formPhotoPath = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentView) {
                            "add" -> "Tambah Anggota"
                            "edit" -> "Edit Anggota"
                            "detail" -> "Detail Anggota"
                            else -> "Daftar Anggota"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    if (currentView != "list") {
                        IconButton(onClick = {
                            currentView = "list"
                            selectedMember = null
                            resetForm()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentView == "list") {
                ExtendedFloatingActionButton(
                    onClick = {
                        resetForm()
                        currentView = "add"
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Tambah Anggota") },
                    text = { Text("Tambah Anggota") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            }
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
                ) { Text(notificationMessage) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000)
                    showNotification = false
                }
            }
        }
    ) { padding ->
        when (currentView) {
            "list" -> MemberListView(
                members = filteredMembers,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onMemberClick = { member ->
                    selectedMember = member
                    formName = member.name
                    formNim = member.nim
                    formGender = member.gender
                    formFaculty = member.faculty
                    formMajor = member.major
                    formContact = member.contact
                    formEmail = member.email
                    formPhotoPath = member.photoPath
                    currentView = "detail"
                },
                modifier = Modifier.padding(padding)
            )

            "detail" -> selectedMember?.let { member ->
                MemberDetailView(
                    member = member,
                    onEdit = { currentView = "edit" },
                    onDelete = { showDeleteDialog = true },
                    modifier = Modifier.padding(padding)
                )
            }

            "add", "edit" -> AddEditMemberView(
                isEdit = currentView == "edit",
                name = formName,
                nim = formNim,
                gender = formGender,
                faculty = formFaculty,
                major = formMajor,
                contact = formContact,
                email = formEmail,
                photoPath = formPhotoPath,
                onNameChange = { formName = it },
                onNimChange = { formNim = it },
                onGenderChange = { formGender = it },
                onFacultyChange = {
                    formFaculty = it
                    formMajor = ""
                },
                onMajorChange = { formMajor = it },
                onContactChange = { formContact = it },
                onEmailChange = { formEmail = it },
                onPhotoPathChange = { formPhotoPath = it },
                onSave = {
                    if (formName.isNotEmpty() && formNim.isNotEmpty() && formGender.isNotEmpty() && formFaculty.isNotEmpty() && formMajor.isNotEmpty() && formContact.isNotEmpty()) {
                        val memberData = Member(
                            id = selectedMember?.id ?: 0,
                            name = formName,
                            nim = formNim,
                            gender = formGender,
                            faculty = formFaculty,
                            major = formMajor,
                            contact = formContact,
                            email = formEmail,
                            photoPath = formPhotoPath,
                            registrationDate = selectedMember?.registrationDate ?: ""
                        )

                        if (currentView == "add") {
                            database.insertMember(memberData)
                            showNotif("✓ Anggota baru berhasil ditambahkan!")
                        } else {
                            database.updateMember(memberData)
                            showNotif("✓ Data anggota berhasil diperbarui!")
                        }
                        refreshMembersView()
                        currentView = "list"
                        resetForm()
                    } else {
                        showNotif("⚠️ Harap isi semua field yang wajib (*)")
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
            title = { Text("Hapus Anggota") },
            text = { Text("Yakin ingin menghapus data ${selectedMember?.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedMember?.let { member ->
                        if (member.photoPath.isNotEmpty()) {
                            try {
                                File(member.photoPath).delete()
                            } catch (e: Exception) {
                                Log.e("MembersScreen", "Gagal hapus file foto: ${member.photoPath}", e)
                            }
                        }
                        database.deleteMember(member.id)
                        refreshMembersView()
                        showNotif("✓ Anggota berhasil dihapus!")
                        currentView = "list"
                        selectedMember = null
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

// --- Composable untuk Tampilan Daftar Anggota ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberListView(
    members: List<Member>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onMemberClick: (Member) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Cari Nama atau NIM...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp)
        ) {
            if (members.isEmpty() && searchQuery.isEmpty()) {
                item {
                    Text(
                        "Belum ada data anggota. Tekan tombol '+' untuk menambah.",
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (members.isEmpty() && searchQuery.isNotEmpty()) {
                item {
                    Text(
                        "Anggota '$searchQuery' tidak ditemukan.",
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(members, key = { it.id }) { member ->
                    MemberItem(member = member, onClick = { onMemberClick(member) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// --- Composable untuk Satu Item Anggota di Daftar ---
@Composable
fun MemberItem(member: Member, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val defaultImage = if (member.gender == "Perempuan") {
                R.drawable.default_wanita
            } else {
                R.drawable.default_pria
            }

            AsyncImage(
                model = if (member.photoPath.isNotEmpty()) File(member.photoPath) else defaultImage,
                contentDescription = "Foto ${member.name}",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop,
                onError = { Log.e("MemberItem", "Gagal load gambar: ${member.photoPath}", it.result.throwable) },
                placeholder = painterResource(id = defaultImage)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(member.nim, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(2.dp))
                // --- PERBAIKAN DI SINI ---
                Text(
                    text = member.faculty, // Hanya tampilkan fakultas
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
                // --- AKHIR PERBAIKAN ---
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}


// --- Composable untuk Tampilan Detail Anggota ---
@Composable
fun MemberDetailView(
    member: Member,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            val defaultImage = if (member.gender == "Perempuan") {
                R.drawable.default_wanita
            } else {
                R.drawable.default_pria
            }

            AsyncImage(
                model = if (member.photoPath.isNotEmpty()) File(member.photoPath) else defaultImage,
                contentDescription = "Foto ${member.name}",
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop,
                onError = { Log.e("MemberDetail", "Gagal load gambar: ${member.photoPath}", it.result.throwable) },
                placeholder = painterResource(id = defaultImage)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(member.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("NIM: ${member.nim}", fontSize = 16.sp, color = Color.Gray)
            Text(
                text = "Terdaftar: ${member.registrationDate}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            InfoCard(
                gender = member.gender,
                faculty = member.faculty,
                major = member.major,
                contact = member.contact,
                email = member.email
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun InfoCard(gender: String, faculty: String, major: String, contact: String, email: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRowMember(icon = Icons.Default.Wc, label = "Jenis Kelamin", value = gender)
            DetailRowMember(icon = Icons.Default.School, label = "Fakultas", value = faculty)
            DetailRowMember(icon = Icons.AutoMirrored.Filled.Notes, label = "Jurusan", value = major)
            DetailRowMember(icon = Icons.Default.Phone, label = "Kontak", value = contact)
            DetailRowMember(icon = Icons.Default.Email, label = "Email", value = email)
        }
    }
}

@Composable
fun DetailRowMember(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    if (value.isNotEmpty()) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Text(
                text = value,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 36.dp)
            )
        }
        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberView(
    isEdit: Boolean,
    name: String,
    nim: String,
    gender: String,
    faculty: String,
    major: String,
    contact: String,
    email: String,
    photoPath: String,
    onNameChange: (String) -> Unit,
    onNimChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onFacultyChange: (String) -> Unit,
    onMajorChange: (String) -> Unit,
    onContactChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhotoPathChange: (String) -> Unit,
    onSave: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    var facultyExpanded by remember { mutableStateOf(false) }
    var majorExpanded by remember { mutableStateOf(false) }

    val facultyList = remember { facultyMajorsMap.keys.sorted() }

    val majorList = remember(faculty) {
        facultyMajorsMap[faculty] ?: emptyList()
    }

    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = saveImageToInternalStorage(context, it, "member_photo")
            if (savedPath.isNotEmpty()) {
                onPhotoPathChange(savedPath)
            } else {
                Log.e("AddEditMemberView", "Gagal menyimpan gambar dari galeri")
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempCameraImageUri?.let { uri ->
                val savedPath = saveImageToInternalStorage(context, uri, "member_photo_cam")
                if (savedPath.isNotEmpty()) {
                    onPhotoPathChange(savedPath)
                } else {
                    Log.e("AddEditMemberView", "Gagal menyimpan gambar dari kamera")
                }
            }
        }
        tempCameraImageUri = null
    }


    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = if (isEdit) "Edit Data Anggota" else "Tambah Anggota Baru",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            val defaultImage = if (gender == "Perempuan") {
                R.drawable.default_wanita
            } else {
                R.drawable.default_pria
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = if (photoPath.isNotEmpty()) File(photoPath) else defaultImage,
                    contentDescription = "Foto Profil",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { showImageSourceDialog = true },
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = defaultImage)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { showImageSourceDialog = true }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih Foto")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nama Lengkap *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = nim,
                onValueChange = onNimChange,
                label = { Text("NIM *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Jenis Kelamin *",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    RadioButton(
                        selected = gender == "Laki-laki",
                        onClick = { onGenderChange("Laki-laki") }
                    )
                    Text("Laki-laki", Modifier.padding(end = 16.dp).clickable { onGenderChange("Laki-laki") })

                    RadioButton(
                        selected = gender == "Perempuan",
                        onClick = { onGenderChange("Perempuan") }
                    )
                    Text("Perempuan", Modifier.clickable { onGenderChange("Perempuan") })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = facultyExpanded,
                onExpandedChange = { facultyExpanded = !facultyExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = faculty.ifEmpty { "Pilih Fakultas *" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fakultas *") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = facultyExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = facultyExpanded,
                    onDismissRequest = { facultyExpanded = false }
                ) {
                    facultyList.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                onFacultyChange(selectionOption)
                                facultyExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = majorExpanded,
                onExpandedChange = {
                    if (faculty.isNotEmpty()) {
                        majorExpanded = !majorExpanded
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = major.ifEmpty { "Pilih Jurusan *" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Jurusan *") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = majorExpanded) },
                    enabled = faculty.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
                ExposedDropdownMenu(
                    expanded = majorExpanded,
                    onDismissRequest = { majorExpanded = false }
                ) {
                    majorList.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                onMajorChange(selectionOption)
                                majorExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = contact,
                onValueChange = onContactChange,
                label = { Text("Nomor Kontak *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = name.isNotEmpty() && nim.isNotEmpty() && gender.isNotEmpty() && faculty.isNotEmpty() && major.isNotEmpty() && contact.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEdit) "Simpan Perubahan" else "Tambah Anggota")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Pilih Sumber Gambar") },
            text = { Text("Ambil gambar dari galeri atau kamera?") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        galleryLauncher.launch("image/*")
                        showImageSourceDialog = false
                    }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Galeri")
                    }
                    Button(onClick = {
                        val photoFile: File? = try {
                            createImageFile(context, "member_photo_cam")
                        } catch (ex: Exception) {
                            Log.e("AddEditMemberView", "Gagal membuat file gambar", ex)
                            null
                        }

                        photoFile?.let {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                it
                            )
                            tempCameraImageUri = uri
                            cameraLauncher.launch(uri)
                        } ?: run {
                            Log.e("AddEditMemberView", "Tidak bisa membuat URI untuk kamera")
                        }
                        showImageSourceDialog = false
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Kamera")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) { Text("Batal") }
            }
        )
    }
}