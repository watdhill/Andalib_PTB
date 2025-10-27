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
import androidx.compose.material.icons.automirrored.filled.Notes // Ganti jika ikon tidak ada
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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

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
    var formFaculty by remember { mutableStateOf("") } // State Fakultas
    var formMajor by remember { mutableStateOf("") } // State Jurusan
    var formContact by remember { mutableStateOf("") }
    var formEmail by remember { mutableStateOf("") }
    var formPhotoPath by remember { mutableStateOf("") }

    val filteredMembers = if (searchQuery.isEmpty()) {
        members
    } else {
        database.searchMembers(searchQuery) // Gunakan search dari DB
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
        formFaculty = "" // Reset Fakultas
        formMajor = "" // Reset Jurusan
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
                actions = {
                    if (currentView == "list") {
                        IconButton(onClick = {
                            resetForm()
                            currentView = "add"
                        }) {
                            Icon(Icons.Default.Add, "Tambah Anggota", tint = Color.White)
                        }
                    }
                },
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
        snackbarHost = {
            // ... (Snackbar code remains the same) ...
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
                    // Isi form untuk persiapan detail/edit
                    formName = member.name
                    formNim = member.nim
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
                    onEdit = { currentView = "edit" }, // Langsung ke edit karena form sudah diisi
                    onDelete = { showDeleteDialog = true },
                    modifier = Modifier.padding(padding)
                )
            }

            "add", "edit" -> AddEditMemberView(
                isEdit = currentView == "edit",
                // Berikan state form
                name = formName,
                nim = formNim,
                faculty = formFaculty, // <-- Berikan state Fakultas
                major = formMajor,     // <-- Berikan state Jurusan
                contact = formContact,
                email = formEmail,
                photoPath = formPhotoPath,
                // Berikan lambda untuk update state form
                onNameChange = { formName = it },
                onNimChange = { formNim = it },
                onFacultyChange = { formFaculty = it }, // <-- Berikan lambda Fakultas
                onMajorChange = { formMajor = it },     // <-- Berikan lambda Jurusan
                onContactChange = { formContact = it },
                onEmailChange = { formEmail = it },
                onPhotoPathChange = { formPhotoPath = it },
                onSave = {
                    // Validasi
                    if (formName.isNotEmpty() && formNim.isNotEmpty() && formFaculty.isNotEmpty() && formMajor.isNotEmpty() && formContact.isNotEmpty()) {
                        val memberData = Member(
                            id = selectedMember?.id ?: 0,
                            name = formName,
                            nim = formNim,
                            faculty = formFaculty, // <-- Sertakan Fakultas
                            major = formMajor,     // <-- Sertakan Jurusan
                            contact = formContact,
                            email = formEmail,
                            photoPath = formPhotoPath
                            // registrationDate akan diisi otomatis oleh DB jika menggunakan DEFAULT CURRENT_TIMESTAMP
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

    // Dialog Konfirmasi Hapus
    if (showDeleteDialog) {
        // ... (AlertDialog code remains the same) ...
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Anggota") },
            text = { Text("Yakin ingin menghapus data ${selectedMember?.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedMember?.let { member ->
                        // Hapus file foto jika ada sebelum hapus dari DB
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
        // Search Bar (tetap sama)
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

        // Daftar Anggota
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (members.isEmpty() && searchQuery.isEmpty()) { // Tampil jika list kosong & tidak sedang mencari
                item {
                    Text(
                        "Belum ada data anggota. Tekan tombol '+' untuk menambah.",
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (members.isEmpty() && searchQuery.isNotEmpty()) { // Tampil jika hasil cari kosong
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
            // Foto Profil Bulat
            AsyncImage(
                model = if (member.photoPath.isNotEmpty()) File(member.photoPath) else R.drawable.default_pria, // Tampilkan default jika kosong
                contentDescription = "Foto ${member.name}",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray), // Background jika gambar gagal load
                contentScale = ContentScale.Crop,
                onError = { Log.e("MemberItem", "Gagal load gambar: ${member.photoPath}", it.result.throwable) },
                placeholder = painterResource(id = R.drawable.default_pria) // Placeholder default
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info Anggota
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(member.nim, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(2.dp))
                Text( // Tampilkan Fakultas & Jurusan
                    text = "${member.faculty} / ${member.major}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            // Icon panah kanan (opsional)
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
            // Foto Profil Besar
            AsyncImage(
                model = if (member.photoPath.isNotEmpty()) File(member.photoPath) else R.drawable.default_pria,
                contentDescription = "Foto ${member.name}",
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop,
                onError = { Log.e("MemberDetail", "Gagal load gambar: ${member.photoPath}", it.result.throwable) },
                placeholder = painterResource(id = R.drawable.default_pria)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nama Besar
            Text(member.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            // NIM di bawah nama
            Text("NIM: ${member.nim}", fontSize = 16.sp, color = Color.Gray)
            // Tanggal Registrasi
            Text(
                text = "Terdaftar: ${member.registrationDate}", // Tampilkan tanggal
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Detail Info dalam Card
            InfoCard( // Gunakan Composable InfoCard
                faculty = member.faculty,
                major = member.major,
                contact = member.contact,
                email = member.email
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Edit & Hapus
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
            Spacer(modifier = Modifier.height(16.dp)) // Jarak bawah
        }
    }
}

// Card Info Detail (dipisah agar rapi)
@Composable
fun InfoCard(faculty: String, major: String, contact: String, email: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRowMember(icon = Icons.Default.School, label = "Fakultas", value = faculty)
            DetailRowMember(icon = Icons.AutoMirrored.Filled.Notes, label = "Jurusan", value = major) // Ikon notes
            DetailRowMember(icon = Icons.Default.Phone, label = "Kontak", value = contact)
            DetailRowMember(icon = Icons.Default.Email, label = "Email", value = email) // Tambahkan Email
        }
    }
}

// Baris Detail dengan Ikon (dipisah agar rapi)
@Composable
fun DetailRowMember(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    if (value.isNotEmpty()) { // Hanya tampilkan jika ada value
        Column(modifier = Modifier.padding(bottom = 12.dp)) { // Tambah padding bawah
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
            Text( // Value di bawah label
                text = value,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 36.dp) // Indentasi value
            )
        }
        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp) // Pemisah
    }
}


// --- Composable untuk Form Tambah/Edit Anggota (PERBAIKAN FAKULTAS & JURUSAN) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberView(
    isEdit: Boolean,
    name: String,
    nim: String,
    faculty: String, // State untuk Fakultas
    major: String,   // State untuk Jurusan
    contact: String,
    email: String,
    photoPath: String,
    onNameChange: (String) -> Unit,
    onNimChange: (String) -> Unit,
    onFacultyChange: (String) -> Unit, // Lambda untuk Fakultas
    onMajorChange: (String) -> Unit,   // Lambda untuk Jurusan
    onContactChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhotoPathChange: (String) -> Unit,
    onSave: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    var facultyExpanded by remember { mutableStateOf(false) } // State dropdown fakultas
    // Daftar Fakultas
    val facultyList = remember { // Gunakan remember agar list tidak dibuat ulang
        listOf(
            "Fakultas Kedokteran", "Fakultas Kedokteran Gigi", "Fakultas Farmasi",
            "Fakultas Pertanian", "Fakultas Peternakan", "Fakultas Ekonomi dan Bisnis",
            "Fakultas MIPA", "Fakultas ISIP", "Fakultas Teknik",
            "Fakultas Ilmu Budaya", "Fakultas Teknologi Informasi", "Fakultas Keperawatan",
            "Fakultas Kesehatan Masyarakat", "Fakultas Teknologi Pertanian", "Fakultas Hukum"
        )
    }

    // State dan launcher untuk image picker
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) } // Simpan URI kamera sementara

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = saveImageToInternalStorage(context, it, "member_photo")
            if (savedPath.isNotEmpty()) {
                onPhotoPathChange(savedPath)
            } else {
                Log.e("AddEditMemberView", "Gagal menyimpan gambar dari galeri")
                // Tampilkan pesan error jika perlu
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
                    // Tampilkan pesan error jika perlu
                }
            }
        }
        tempCameraImageUri = null // Reset URI sementara
    }

    // Fungsi untuk membuat URI file sementara untuk kamera
    fun createTempImageFileUri(context: Context): Uri? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = context.getExternalFilesDir("Pictures") // Simpan di external biar mudah diakses
        return try {
            val file = File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // Sesuaikan dengan authority di AndroidManifest
                file
            )
        } catch (ex: Exception) {
            Log.e("CreateUri", "Gagal membuat file sementara", ex)
            null
        }
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

            // --- Bagian Pilih Foto ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = if (photoPath.isNotEmpty()) File(photoPath) else R.drawable.default_pria,
                    contentDescription = "Foto Profil",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { showImageSourceDialog = true }, // Klik gambar buka dialog
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.default_pria)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { showImageSourceDialog = true }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih Foto")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Form Fields ---
            OutlinedTextField( // Nama
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nama Lengkap *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField( // NIM
                value = nim,
                onValueChange = onNimChange,
                label = { Text("NIM *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // --- Dropdown Fakultas ---
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

            // --- Input Jurusan (Teks Biasa) ---
            OutlinedTextField(
                value = major,
                onValueChange = onMajorChange,
                label = { Text("Jurusan *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField( // Kontak
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

            OutlinedTextField( // Email (Opsional)
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

            // --- Tombol Simpan ---
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = name.isNotEmpty() && nim.isNotEmpty() && faculty.isNotEmpty() && major.isNotEmpty() && contact.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEdit) "Simpan Perubahan" else "Tambah Anggota")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // --- Dialog Pilih Sumber Gambar ---
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
                        // Buat URI baru setiap kali kamera dibuka
                        val uri = createTempImageFileUri(context)
                        if (uri != null) {
                            tempCameraImageUri = uri // Simpan URI untuk callback kamera
                            cameraLauncher.launch(uri)
                        } else {
                            Log.e("AddEditMemberView", "Tidak bisa membuat URI untuk kamera")
                            // Tampilkan pesan error jika perlu
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


// Fungsi helper untuk menyimpan gambar (mirip di BookScreen, tambahkan prefix nama file)
fun saveImageToInternalStorage(context: Context, uri: Uri, prefix: String): String {
    var inputStream: InputStream? = null
    var outputStream: FileOutputStream? = null
    // Gunakan timestamp unik untuk nama file
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "${prefix}_${timeStamp}.jpg"
    // Simpan di direktori file internal aplikasi
    val file = File(context.filesDir, fileName)
    var savedPath = ""

    try {
        inputStream = context.contentResolver.openInputStream(uri)
        outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        savedPath = file.absolutePath // Dapatkan path absolut file yang disimpan
        Log.d("ImageSave", "Gambar disimpan di: $savedPath")
    } catch (e: Exception) {
        Log.e("ImageSave", "Gagal menyimpan gambar dari URI: $uri", e)
        // Hapus file jika proses penyimpanan gagal sebagian
        if (file.exists()) {
            file.delete()
        }
    } finally {
        // Pastikan stream ditutup
        try {
            inputStream?.close()
        } catch (e: Exception) {
            Log.e("ImageSave", "Gagal menutup inputStream", e)
        }
        try {
            outputStream?.close()
        } catch (e: Exception) {
            Log.e("ImageSave", "Gagal menutup outputStream", e)
        }
    }
    return savedPath // Kembalikan path file yang disimpan atau string kosong jika gagal
}


// Fungsi untuk membuat URI file sementara (diperlukan untuk kamera)
fun createImageFileUri(context: Context): Uri? {
    return try {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        // Simpan di cache directory internal
        val storageDir: File = context.cacheDir
        val file = File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
        // Dapatkan URI menggunakan FileProvider
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Authority harus sama dengan di AndroidManifest dan file_paths.xml
            file
        )
    } catch (ex: Exception) {
        Log.e("CreateImageFile", "Error creating image file URI", ex)
        null
    }
}
