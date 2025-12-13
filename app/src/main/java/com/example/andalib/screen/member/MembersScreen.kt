package com.example.andalib.screen.member

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.andalib.R
import com.example.andalib.data.network.MemberService
import com.example.andalib.data.network.createMemberService
import com.example.andalib.data.TokenManager
import com.example.andalib.ui.theme.AndalibDarkBlue
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Service API
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { createMemberService(tokenManager) }

    // State Data
    var members by remember { mutableStateOf(emptyList<MemberApi>()) }
    var isLoading by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // Load Data
    LaunchedEffect(refreshTrigger) {
        isLoading = true
        try {
            // Note: Pastikan RetrofitClient.create... mengembalikan service yang benar memuat method getAllMembers
            // Jika RetrofitClient Anda hanya return AuthService, Anda perlu menambah method createMemberService di sana.
            // Untuk contoh ini saya asumsikan apiService sudah benar.
            // SEMENTARA: Menggunakan try-catch manual karena ViewModel belum ada di prompt ini
            val response = apiService.getAllMembers()
            if (response.success) {
                members = response.data
            } else {
                Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MembersScreen", "Error loading members", e)
            // Toast.makeText(context, "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // State UI
    var searchQuery by remember { mutableStateOf("") }
    var currentView by remember { mutableStateOf("list") } // list, add, edit, detail
    var selectedMember by remember { mutableStateOf<MemberApi?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // State Form
    var formName by remember { mutableStateOf("") }
    var formNim by remember { mutableStateOf("") }
    var formGender by remember { mutableStateOf(Gender.LAKI_LAKI) }
    var formFaculty by remember { mutableStateOf(Faculty.HUKUM) }
    var formMajor by remember { mutableStateOf(Major.ILMU_HUKUM) }
    var formContact by remember { mutableStateOf("") }
    var formEmail by remember { mutableStateOf("") }
    var formPhotoPath by remember { mutableStateOf("") }

    val filteredMembers = if (searchQuery.isEmpty()) members else members.filter {
        it.name.contains(searchQuery, true) || it.nim.contains(searchQuery, true)
    }

    fun refreshMembersView() { refreshTrigger++ }

    fun resetForm() {
        formName = ""; formNim = ""; formContact = ""; formEmail = ""; formPhotoPath = ""
        formGender = Gender.LAKI_LAKI
        formFaculty = Faculty.HUKUM
        formMajor = Major.ILMU_HUKUM
    }

    // Fungsi Simpan ke API
    fun saveMember(isEdit: Boolean) {
        scope.launch {
            isLoading = true
            try {
                // Check for duplicate NIM when adding new member
                if (!isEdit) {
                    val isDuplicate = members.any { it.nim == formNim }
                    if (isDuplicate) {
                        Toast.makeText(context, "NIM tersebut sudah terdaftar", Toast.LENGTH_SHORT).show()
                        isLoading = false
                        return@launch
                    }
                }
                
                val namePart = formName.toRequestBody("text/plain".toMediaTypeOrNull())
                val nimPart = formNim.toRequestBody("text/plain".toMediaTypeOrNull())
                val genderPart = formGender.name.toRequestBody("text/plain".toMediaTypeOrNull())
                val facultyPart = formFaculty.name.toRequestBody("text/plain".toMediaTypeOrNull())
                val majorPart = formMajor.name.toRequestBody("text/plain".toMediaTypeOrNull())
                val contactPart = formContact.toRequestBody("text/plain".toMediaTypeOrNull())
                val emailPart = formEmail.toRequestBody("text/plain".toMediaTypeOrNull())

                val photoPart = if (formPhotoPath.isNotEmpty()) {
                    val file = File(formPhotoPath)
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("photo", file.name, requestFile)
                } else null

                val response = if (isEdit && selectedMember != null) {
                    apiService.updateMember(
                        targetNim = selectedMember!!.nim,
                        name = namePart,
                        gender = genderPart,
                        faculty = facultyPart,
                        major = majorPart,
                        contact = contactPart,
                        email = emailPart,
                        photo = photoPart
                    )
                } else {
                    apiService.createMember(
                        nim = nimPart,
                        name = namePart,
                        gender = genderPart,
                        faculty = facultyPart,
                        major = majorPart,
                        contact = contactPart,
                        email = emailPart,
                        photo = photoPart
                    )
                }

                if (response.success) {
                    Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                    refreshMembersView()
                    currentView = "list"
                    resetForm()
                } else {
                    Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MembersScreen", "Error saving member", e)
                Toast.makeText(context, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteMember() {
        selectedMember?.let { member ->
            scope.launch {
                try {
                    val response = apiService.deleteMember(member.nim)
                    if (response.success) {
                        Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                        refreshMembersView()
                        currentView = "list"
                        selectedMember = null
                    } else {
                        Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal hapus: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentView) {
                            "add" -> "Tambah Anggota"
                            "edit" -> "Edit Anggota"
                            "detail" -> "Detail Anggota"
                            else -> "Daftar Anggota"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AndalibDarkBlue,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    if (currentView != "list") {
                        IconButton(onClick = {
                            currentView = "list"; selectedMember = null; resetForm()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when (currentView) {
                "list" -> MemberListView(
                    members = filteredMembers,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onMemberClick = { member ->
                        try {
                            selectedMember = member
                            // Populate Form
                            formName = member.name.ifEmpty { "" }
                            formNim = member.nim.ifEmpty { "" }
                            // Try parse enum, default if fail
                            formGender = try { 
                                Gender.valueOf(member.gender.uppercase()) 
                            } catch (e: Exception) { 
                                Log.w("MembersScreen", "Failed to parse gender: ${member.gender}", e)
                                Gender.LAKI_LAKI 
                            }
                            formFaculty = try { 
                                Faculty.valueOf(member.faculty.uppercase()) 
                            } catch (e: Exception) { 
                                Log.w("MembersScreen", "Failed to parse faculty: ${member.faculty}", e)
                                Faculty.HUKUM 
                            }
                            formMajor = try { 
                                Major.valueOf(member.major.uppercase()) 
                            } catch (e: Exception) { 
                                Log.w("MembersScreen", "Failed to parse major: ${member.major}", e)
                                Major.ILMU_HUKUM 
                            }
                            formContact = member.contact.ifEmpty { "" }
                            formEmail = member.email ?: ""
                            formPhotoPath = "" // Reset photo path for edit (photoUrl used for display)
                            currentView = "detail"
                        } catch (e: Exception) {
                            Log.e("MembersScreen", "Error selecting member", e)
                            Toast.makeText(context, "Gagal memuat detail anggota: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteClick = { member ->
                        selectedMember = member
                        showDeleteDialog = true
                    },
                    onAddClick = { resetForm(); currentView = "add" }
                )
                "detail" -> {
                    if (selectedMember != null) {
                        MemberDetailView(
                            member = selectedMember!!,
                            onEdit = { currentView = "edit" },
                            onDelete = { showDeleteDialog = true }
                        )
                    } else {
                        // Fallback jika selectedMember null
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Data anggota tidak ditemukan", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { currentView = "list" }) {
                                Text("Kembali ke Daftar")
                            }
                        }
                    }
                }
                "add", "edit" -> AddEditMemberView(
                    isEdit = currentView == "edit",
                    name = formName, onNameChange = { formName = it },
                    nim = formNim, onNimChange = { formNim = it },
                    gender = formGender, onGenderChange = { formGender = it },
                    faculty = formFaculty, onFacultyChange = { formFaculty = it },
                    major = formMajor, onMajorChange = { formMajor = it },
                    contact = formContact, onContactChange = { formContact = it },
                    email = formEmail, onEmailChange = { formEmail = it },
                    photoPath = formPhotoPath, onPhotoPathChange = { formPhotoPath = it },
                    photoUrl = selectedMember?.photoUrl,
                    onSave = {
                        if (formName.isNotEmpty() && formNim.isNotEmpty() && formContact.isNotEmpty()) {
                            saveMember(currentView == "edit")
                        } else {
                            Toast.makeText(context, "Harap isi field wajib (*)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    context = context
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Anggota") },
            text = { Text("Yakin ingin menghapus ${selectedMember?.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteMember()
                    showDeleteDialog = false
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun MemberListView(
    members: List<MemberApi>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onMemberClick: (MemberApi) -> Unit,
    onDeleteClick: (MemberApi) -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
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
                    text = "Anggota Perpustakaan",
                    modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                // Member List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (members.isEmpty()) {
                        item {
                            Text(
                                "Data tidak ditemukan.",
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        }
                    } else {
                        items(members) { member ->
                            MemberItem(
                                member = member,
                                onClick = { onMemberClick(member) },
                                onDelete = { onDeleteClick(member) }
                            )
                        }
                    }
                }
                
                // Add Member Button at bottom
                Button(
                    onClick = onAddClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AndalibDarkBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Tambah Anggota", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MemberItem(
    member: MemberApi,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            val defaultPhoto = try {
                if (member.gender.uppercase() == "LAKI_LAKI") R.drawable.default_pria else R.drawable.default_wanita
            } catch (e: Exception) {
                R.drawable.default_pria
            }
            AsyncImage(
                model = member.photoUrl ?: defaultPhoto,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFE0E0E0), CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(defaultPhoto),
                error = painterResource(defaultPhoto)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info Section
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = member.nim,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Faculty Badge
                val facultyEnum = try {
                    Faculty.valueOf(member.faculty.uppercase())
                } catch (e: Exception) {
                    Faculty.HUKUM
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = facultyEnum.getBadgeColor().copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = member.faculty.replace("_", " "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = facultyEnum.getBadgeColor()
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Edit Button
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFF59D), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFF57C00)
                    )
                }
                
                // Delete Button  
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFCDD2), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

@Composable
fun MemberDetailView(member: MemberApi, onEdit: () -> Unit, onDelete: () -> Unit) {
    // Safe values untuk menghindari crash
    val safeName = member.name.ifEmpty { "-" }
    val safeNim = member.nim.ifEmpty { "-" }
    val safeGender = try {
        if (member.gender.isNotEmpty()) member.gender.replace("_", " ") else "-"
    } catch (e: Exception) {
        Log.e("MemberDetailView", "Error processing gender", e)
        "-"
    }
    val safeFaculty = try {
        if (member.faculty.isNotEmpty()) member.faculty.replace("_", " ") else "-"
    } catch (e: Exception) {
        Log.e("MemberDetailView", "Error processing faculty", e)
        "-"
    }
    val safeMajor = try {
        if (member.major.isNotEmpty()) member.major.replace("_", " ") else "-"
    } catch (e: Exception) {
        Log.e("MemberDetailView", "Error processing major", e)
        "-"
    }
    val safeContact = member.contact.ifEmpty { "-" }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Determine default photo based on gender
        val defaultPhoto = try {
            if (member.gender.uppercase() == "LAKI_LAKI") R.drawable.default_pria else R.drawable.default_wanita
        } catch (e: Exception) {
            R.drawable.default_pria
        }
        AsyncImage(
            model = member.photoUrl ?: defaultPhoto,
            contentDescription = null,
            modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.LightGray),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(defaultPhoto),
            error = painterResource(defaultPhoto)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = safeName, 
            fontSize = 22.sp, 
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "NIM: $safeNim", 
            fontSize = 16.sp, 
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(Icons.Default.Person, "Gender", safeGender)
                DetailRow(Icons.Default.School, "Fakultas", safeFaculty)
                DetailRow(Icons.AutoMirrored.Filled.Notes, "Jurusan", safeMajor)
                DetailRow(Icons.Default.Phone, "Kontak", safeContact)
                if (!member.email.isNullOrEmpty()) {
                    DetailRow(Icons.Default.Email, "Email", member.email)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp)); Text("Edit")
            }
            Button(onClick = onDelete, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Hapus")
            }
        }
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberView(
    isEdit: Boolean,
    name: String, onNameChange: (String) -> Unit,
    nim: String, onNimChange: (String) -> Unit,
    gender: Gender, onGenderChange: (Gender) -> Unit,
    faculty: Faculty, onFacultyChange: (Faculty) -> Unit,
    major: Major, onMajorChange: (Major) -> Unit,
    contact: String, onContactChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    photoPath: String, onPhotoPathChange: (String) -> Unit,
    photoUrl: String? = null,
    onSave: () -> Unit,
    context: Context
) {
    var facultyExpanded by remember { mutableStateOf(false) }
    var majorExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }

    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onPhotoPathChange(saveImageToInternalStorage(context, it, "member")) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) tempUri?.let { onPhotoPathChange(saveImageToInternalStorage(context, it, "member_cam")) }
    }
    
    // ✅ Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera
            val uri = createImageFileUri(context)
            if (uri != null) {
                tempUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Gagal membuat file foto", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Permission denied
            Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_LONG).show()
        }
    }

    // Filter majors based on selected faculty
    val availableMajors = remember(faculty) { faculty.getMajors() }

    // Auto-reset major when faculty changes if current major is not in the new faculty's majors
    LaunchedEffect(faculty) {
        if (major !in faculty.getMajors()) {
            onMajorChange(faculty.getMajors().first())
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.LightGray).clickable { showImageSourceDialog = true }, contentAlignment = Alignment.Center) {
                // Determine default photo based on gender
                val defaultPhoto = if (gender == Gender.LAKI_LAKI) R.drawable.default_pria else R.drawable.default_wanita
                val model = if (photoPath.isNotEmpty()) File(photoPath) else photoUrl ?: defaultPhoto
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(defaultPhoto),
                    error = painterResource(defaultPhoto)
                )
            }
            TextButton(onClick = { showImageSourceDialog = true }) {
                Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Ubah Foto")
            }
            Spacer(Modifier.height(16.dp))

            // Input Nama
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nama Lengkap *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(8.dp))

            // Input NIM
            OutlinedTextField(
                value = nim,
                onValueChange = onNimChange,
                label = { Text("NIM *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isEdit, // Biasanya NIM tidak boleh diubah saat edit
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(8.dp))

            // Radio Button Gender
            Text("Jenis Kelamin *", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Gender.values().forEach { genderOption ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 1.dp,
                                color = if (gender == genderOption) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(
                                if (gender == genderOption) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                            )
                            .clickable { onGenderChange(genderOption) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = gender == genderOption,
                            onClick = { onGenderChange(genderOption) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = genderOption.name.replace("_", " "),
                            fontSize = 14.sp,
                            color = if (gender == genderOption) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Dropdown Fakultas
            ExposedDropdownMenuBox(expanded = facultyExpanded, onExpandedChange = { facultyExpanded = !facultyExpanded }) {
                OutlinedTextField(
                    value = faculty.name.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fakultas *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = facultyExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(expanded = facultyExpanded, onDismissRequest = { facultyExpanded = false }) {
                    Faculty.values().forEach { item ->
                        DropdownMenuItem(text = { Text(item.name.replace("_", " ")) }, onClick = { onFacultyChange(item); facultyExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Dropdown Jurusan (Major)
            ExposedDropdownMenuBox(expanded = majorExpanded, onExpandedChange = { majorExpanded = !majorExpanded }) {
                OutlinedTextField(
                    value = major.name.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Jurusan *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = majorExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(expanded = majorExpanded, onDismissRequest = { majorExpanded = false }) {
                    availableMajors.forEach { item ->
                        DropdownMenuItem(text = { Text(item.name.replace("_", " ")) }, onClick = { onMajorChange(item); majorExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Kontak
            OutlinedTextField(
                value = contact,
                onValueChange = onContactChange,
                label = { Text("Nomor Kontak *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(8.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(24.dp))

            Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Simpan Data")
            }
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Pilih Foto") },
            confirmButton = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { galleryLauncher.launch("image/*"); showImageSourceDialog = false }) { Text("Galeri") }
                    TextButton(onClick = {
                        showImageSourceDialog = false
                        // ✅ Request camera permission first
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }) { Text("Kamera") }
                }
            }
        )
    }
}

// Helper functions (same as before)
fun saveImageToInternalStorage(context: Context, uri: Uri, prefix: String): String {
    val file = File(context.filesDir, "${prefix}_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output -> input.copyTo(output) }
    }
    return file.absolutePath
}

fun createImageFileUri(context: Context): Uri? {
    val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
    return try {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    } catch (e: Exception) { null }
}