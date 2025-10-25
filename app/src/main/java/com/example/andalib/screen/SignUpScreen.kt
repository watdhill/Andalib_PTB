package com.example.andalib.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.andalib.components.AndalibTextField
import com.example.andalib.components.AndalibButton
import com.example.andalib.components.ClickableAuthText
import com.example.andalib.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpClicked: () -> Unit = {},
    onLoginClicked: () -> Unit = {},
    onBackClicked: () -> Unit = {}
) {
    var nama by remember { mutableStateOf("") }
    var nip by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("") }
    var agreedToTerms by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sign up",
                        color = AndalibWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = AndalibWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AndalibDarkBlue
                )
            )
        },
        containerColor = AndalibWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // --- Judul ---
            Text(
                text = "Buat Akun",
                style = MaterialTheme.typography.headlineSmall,
                color = AndalibDarkBlue,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Silakan buat akun baru.",
                style = MaterialTheme.typography.bodyMedium,
                color = AndalibGray,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(32.dp))

            // --- Ilustrasi dengan dots ---
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(AndalibBackground, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // User icon dengan background navy
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(AndalibDarkBlue, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "User Icon",
                            modifier = Modifier.size(40.dp),
                            tint = AndalibWhite
                        )
                    }
                }

                // Decorative dots
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val colors = listOf(
                        Color(0xFF6B9BD1), // Blue
                        Color(0xFFE57373), // Red
                        Color(0xFF81C784), // Green
                        Color(0xFFFFB74D), // Orange
                        Color(0xFF9575CD)  // Purple
                    )

                    drawCircle(
                        color = colors[0],
                        radius = 6.dp.toPx(),
                        center = Offset(size.width * 0.75f, size.height * 0.2f)
                    )

                    drawCircle(
                        color = colors[1],
                        radius = 8.dp.toPx(),
                        center = Offset(size.width * 0.85f, size.height * 0.35f)
                    )

                    drawCircle(
                        color = colors[3],
                        radius = 7.dp.toPx(),
                        center = Offset(size.width * 0.8f, size.height * 0.7f)
                    )

                    drawCircle(
                        color = colors[2],
                        radius = 6.dp.toPx(),
                        center = Offset(size.width * 0.15f, size.height * 0.45f)
                    )

                    drawCircle(
                        color = colors[4],
                        radius = 5.dp.toPx(),
                        center = Offset(size.width * 0.25f, size.height * 0.25f)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- Input Nama ---
            AndalibTextField(
                value = nama,
                onValueChange = { nama = it },
                label = "Nama"
            )

            Spacer(Modifier.height(16.dp))

            // --- Input NIP ---
            AndalibTextField(
                value = nip,
                onValueChange = { nip = it },
                label = "NIP",
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(Modifier.height(16.dp))

            // --- Input Email ---
            AndalibTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email"
            )

            Spacer(Modifier.height(16.dp))

            // --- Dropdown Role ---
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedRole,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kata sandi") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AndalibDarkBlue,
                        focusedLabelColor = AndalibDarkBlue,
                        unfocusedBorderColor = AndalibGray.copy(alpha = 0.5f)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("Admin", "Guru", "Siswa").forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role) },
                            onClick = {
                                selectedRole = role
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Checkbox Terms ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = { agreedToTerms = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AndalibDarkBlue,
                        checkmarkColor = AndalibWhite
                    )
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Dengan membuat akun, Anda setuju dengan",
                        style = MaterialTheme.typography.bodySmall,
                        color = AndalibGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Syarat dan Ketentuan kami.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AndalibLightBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- Tombol Sign Up ---
            AndalibButton(
                text = "Sign up",
                onClick = onSignUpClicked
            )

            Spacer(Modifier.height(16.dp))

            // --- Link Login ---
            ClickableAuthText(
                prefixText = "Punya akun?",
                clickableText = "Log In",
                onClick = onLoginClicked,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun SignUpScreenPreview() {
    AndalibTheme {
        SignUpScreen()
    }
}