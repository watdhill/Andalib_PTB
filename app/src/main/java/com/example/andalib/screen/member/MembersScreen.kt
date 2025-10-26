package com.example.andalib.screen.member

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen() {
    val context = LocalContext.current
    val database = remember { MemberDatabase(context) }

    var members by remember { mutableStateOf(database.getAllMembers()) }
    var searchQuery by remember { mutableStateOf("") }
    var currentView by remember { mutableStateOf("list") }
    var selectedMember by remember { mutableStateOf<Member?>(null) }
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var formName by remember { mutableStateOf("") }
    var formNim by remember { mutableStateOf("") }
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

    fun refreshMembers() {
        members = database.getAllMembers()
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
                            else -> "Manajemen Anggota"
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
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Tambah Anggota",
                                tint = Color.White
                            )
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
                    },
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(notificationMessage, color = Color.White)
                    }
                }
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
                    currentView = "detail"
                },
                modifier = Modifier.padding(padding)
            )

            "detail" -> selectedMember?.let { member ->
                MemberDetailView(
                    member = member,
                    onEdit = {
                        formName = member.name
                        formNim = member.nim
                        formMajor = member.major
                        formContact = member.contact
                        formEmail = member.email
                        formPhotoPath = member.photoPath
                        currentView = "edit"
                    },
                    onDelete = {
                        showDeleteDialog = true
                    },
                    modifier = Modifier.padding(padding)
                )
            }

            "add", "edit" -> AddEditMemberView(
                isEdit = currentView == "edit",
                name = formName,
                nim = formNim,
                major = formMajor,
                contact = formContact,
                email = formEmail,
                photoPath = formPhotoPath,
                onNameChange = { formName = it },
                onNimChange = { formNim = it },
                onMajorChange = { formMajor = it },
                onContactChange = { formContact = it },
                onEmailChange = { formEmail = it },
                onPhotoPathChange = { formPhotoPath = it },
                onSave = {
                    if (formName.isNotEmpty() && formNim.isNotEmpty() &&
                        formMajor.isNotEmpty() && formContact.isNotEmpty()) {

                        if (currentView == "add") {
                            val newMember = Member(
                                name = formName,
                                nim = formNim,
                                major = formMajor,
                                faculty = formFaculty,
                                contact = formContact,
                                email = formEmail,
                                photoPath = formPhotoPath
                            )
                            val result = database.insertMember(newMember)
                            if (result > 0) {
                                showNotif("✓ Anggota berhasil ditambahkan!")
                            } else {
                                showNotif("✗ Gagal menambahkan anggota")
                            }
                        } else {
                            selectedMember?.let { member ->
                                val updatedMember = member.copy(
                                    name = formName,
                                    nim = formNim,
                                    major = formMajor,
                                    contact = formContact,
                                    email = formEmail,
                                    photoPath = formPhotoPath
                                )
                                database.updateMember(updatedMember)
                                showNotif("✓ Data anggota berhasil diperbarui!")
                            }
                        }
                        refreshMembers()
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
            title = { Text("Hapus Anggota") },
            text = { Text("Apakah Anda yakin ingin menghapus anggota ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedMember?.let { member ->
                            database.deleteMember(member.id)
                            refreshMembers()
                            showNotif("✓ Anggota berhasil dihapus!")
                            currentView = "list"
                            selectedMember = null
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hapus")
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
                .padding(16.dp),
            placeholder = { Text("Cari nama, NIM, atau jurusan...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (members.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = Color.Gray.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Belum ada anggota",
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
                items(members) { member ->
                    MemberItem(member = member, onClick = { onMemberClick(member) })
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun MemberItem(member: Member, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (member.photoPath.isNotEmpty() && File(member.photoPath).exists()) {
                    val bitmap = remember(member.photoPath) {
                        BitmapFactory.decodeFile(member.photoPath)
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(35.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = member.nim,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = member.major,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

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
            // Photo
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (member.photoPath.isNotEmpty() && File(member.photoPath).exists()) {
                    val bitmap = remember(member.photoPath) {
                        BitmapFactory.decodeFile(member.photoPath)
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(70.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Info Cards
            InfoCard(icon = Icons.Default.Person, label = "Nama", value = member.name)
            InfoCard(icon = Icons.Default.Badge, label = "NIM", value = member.nim)
            InfoCard(icon = Icons.Default.School, label = "Jurusan", value = member.major)
            InfoCard(icon = Icons.Default.Phone, label = "Kontak", value = member.contact)
            if (member.email.isNotEmpty()) {
                InfoCard(icon = Icons.Default.Email, label = "Email", value = member.email)
            }
            InfoCard(
                icon = Icons.Default.CalendarToday,
                label = "Tanggal Daftar",
                value = member.registrationDate
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberView(
    isEdit: Boolean,
    name: String,
    nim: String,
    major: String,
    contact: String,
    email: String,
    photoPath: String,
    onNameChange: (String) -> Unit,
    onNimChange: (String) -> Unit,
    onMajorChange: (String) -> Unit,
    onContactChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhotoPathChange: (String) -> Unit,
    onSave: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val majors = listOf(
        "Teknik Informatika",
        "Sistem Informasi",
        "Teknik Elektro",
        "Teknik Mesin",
        "Manajemen",
        "Akuntansi"
    )

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = saveImageToInternalStorage(context, it, "member")
            onPhotoPathChange(savedPath)
        }
    }

    // Camera Launcher
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri.value?.let { uri ->
                val savedPath = saveImageToInternalStorage(context, uri, "member")
                onPhotoPathChange(savedPath)
            }
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

            // Photo Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoPath.isNotEmpty() && File(photoPath).exists()) {
                        val bitmap = remember(photoPath) {
                            BitmapFactory.decodeFile(photoPath)
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Photo Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(70.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { showImageSourceDialog = true }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih Foto")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form Fields
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nama Lengkap *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = nim,
                onValueChange = onNimChange,
                label = { Text("NIM *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }
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
                    label = { Text("Jurusan *") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
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
                label = { Text("Nomor Kontak *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotEmpty() && nim.isNotEmpty() &&
                        major.isNotEmpty() && contact.isNotEmpty()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEdit) "Simpan Perubahan" else "Tambah Anggota")
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
                            val photoFile = createImageFile(context, "member")
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

fun saveImageToInternalStorage(context: Context, uri: Uri, prefix: String): String {
    val inputStream = context.contentResolver.openInputStream(uri)
    val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, fileName)

    inputStream?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }

    return file.absolutePath
}

fun createImageFile(context: Context, prefix: String): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "${prefix}_${timeStamp}.jpg"
    return File(context.filesDir, fileName)
}