package com.example.andalib.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.andalib.components.AndalibTextField
import com.example.andalib.components.AndalibPasswordField
import com.example.andalib.components.AndalibButton
import com.example.andalib.components.ClickableAuthText
import com.example.andalib.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginClicked: (String, String) -> Unit = { _, _ -> },
    onSignUpClicked: () -> Unit = {},
    onBackClicked: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Log in",
                        color = AndalibWhite,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
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
                actions = {
                    IconButton(onClick = { /* TODO: info action */ }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = AndalibWhite.copy(alpha = 0.8f)
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "Selamat Datang",
                style = MaterialTheme.typography.headlineSmall,
                color = AndalibDarkBlue,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Masuk ke akun Anda untuk melanjutkan",
                style = MaterialTheme.typography.bodyMedium,
                color = AndalibGray,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(40.dp))

            // --- Ilustrasi Lingkaran ---
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(AndalibBackground, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(AndalibDarkBlue, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Lock Icon",
                            modifier = Modifier.size(35.dp),
                            tint = AndalibWhite
                        )
                    }
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val colors = listOf(
                        Color(0xFF6B9BD1), Color(0xFFE57373), Color(0xFF81C784),
                        Color(0xFFFFB74D), Color(0xFF9575CD)
                    )
                    drawCircle(colors[0], 6.dp.toPx(), Offset(size.width * 0.75f, size.height * 0.2f))
                    drawCircle(colors[1], 8.dp.toPx(), Offset(size.width * 0.85f, size.height * 0.35f))
                    drawCircle(colors[3], 7.dp.toPx(), Offset(size.width * 0.8f, size.height * 0.7f))
                    drawCircle(colors[2], 6.dp.toPx(), Offset(size.width * 0.15f, size.height * 0.45f))
                    drawCircle(colors[4], 5.dp.toPx(), Offset(size.width * 0.25f, size.height * 0.25f))
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- Input Email ---
            AndalibTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email"
            )

            Spacer(Modifier.height(16.dp))

            // --- Input Password ---
            AndalibPasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                keyboardActions = KeyboardActions(
                    onDone = { onLoginClicked(email, password) }
                )
            )

            Spacer(Modifier.height(32.dp))

            // --- Tombol Login ---
            AndalibButton(
                text = "Log in",
                onClick = { onLoginClicked(email, password) }
            )

            Spacer(Modifier.weight(1f))

            // --- Link Sign Up ---
            ClickableAuthText(
                prefixText = "Belum punya akun?",
                clickableText = "Sign Up",
                onClick = onSignUpClicked,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun LoginScreenPreview() {
    AndalibTheme {
        LoginScreen()
    }
}
