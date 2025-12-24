package com.example.andalib.screen

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.andalib.components.AndalibTextField
import com.example.andalib.components.AndalibPasswordField
import com.example.andalib.components.AndalibButton
import com.example.andalib.components.ClickableAuthText
import com.example.andalib.screen.pengembalian.AndalibTheme
import com.example.andalib.screen.signup.SignUpViewModel
import com.example.andalib.screen.signup.SignUpViewModelFactory
import com.example.andalib.ui.theme.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpComplete: () -> Unit = {},
    onLoginClicked: () -> Unit = {},
    onBackClicked: () -> Unit = {}
) {
    val context = LocalContext.current

    val viewModel: SignUpViewModel = viewModel(
        factory = SignUpViewModelFactory(context.applicationContext)
    )


    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }


    val signUpState by viewModel.signUpState.collectAsState()
    val isLoading = signUpState is SignUpViewModel.SignUpUiState.Loading


    LaunchedEffect(key1 = signUpState) {
        when (signUpState) {
            is SignUpViewModel.SignUpUiState.Success -> {
                val message = (signUpState as SignUpViewModel.SignUpUiState.Success).message
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()


                name = ""
                email = ""
                password = ""


                onSignUpComplete()
            }
            is SignUpViewModel.SignUpUiState.Error -> {
                val message = (signUpState as SignUpViewModel.SignUpUiState.Error).message
                Toast.makeText(context, "Gagal Registrasi: $message", Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }


    val handleSignUp = {
        viewModel.signup(name, email, password)
    }

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
                    IconButton(onClick = onBackClicked, enabled = !isLoading) {
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
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "User Icon",
                            modifier = Modifier.size(40.dp),
                            tint = AndalibWhite
                        )
                    }
                }


                Canvas(modifier = Modifier.fillMaxSize()) {
                    val colors = listOf(
                        Color(0xFF6B9BD1),
                        Color(0xFFE57373),
                        Color(0xFF81C784),
                        Color(0xFFFFB74D),
                        Color(0xFF9575CD)
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


            AndalibTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nama Lengkap"
            )

            Spacer(Modifier.height(16.dp))


            AndalibTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(Modifier.height(16.dp))


            AndalibPasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                keyboardActions = KeyboardActions(
                    onDone = { handleSignUp() }
                )
            )

            Spacer(Modifier.height(32.dp)) // Tambahkan spacer yang hilang


            AndalibButton(
                text = if (isLoading) "Memproses..." else "Sign up",
                onClick = { handleSignUp() }
            )

            Spacer(Modifier.height(16.dp))


            ClickableAuthText(
                prefixText = "Punya akun?",
                clickableText = "Log In",
                onClick = onLoginClicked,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }


    if (isLoading) {
        AlertDialog(
            onDismissRequest = { /* Tidak bisa di-dismiss saat loading */ },
            title = { Text("Memproses Registrasi") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Menghubungi server...")
                }
            },
            confirmButton = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun SignUpScreenPreview() {
    AndalibTheme {
        SignUpScreen()
    }
}
