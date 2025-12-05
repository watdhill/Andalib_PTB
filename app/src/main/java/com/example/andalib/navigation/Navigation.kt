package com.example.andalib.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

// Mengganti import screen yang belum ada, asumsikan berada di root package 'com.example.andalib.screen
import com.example.andalib.screen.HomeScreen
import com.example.andalib.screen.LoginScreen
import com.example.andalib.screen.SignUpScreen
import com.example.andalib.screen.StartScreen

/**
 * Sealed class untuk define semua routes dalam aplikasi
 */
sealed class Screen(val route: String) {
    object Start : Screen("start_route") // Menggunakan sufiks _route agar lebih eksplisit
    object Login : Screen("login_route")
    object SignUp : Screen("signup_route")
    object Main : Screen("main_route") // Main screen dengan bottom navigation
}

@Composable
fun AndalibNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Start.route // Menggunakan route dari sealed class
    ) {
        // --- 1. Start Screen (Splash/Initial) ---
        composable(route = Screen.Start.route) {
            StartScreen(
                onNavigateToLogin = {
                    // Navigasi ke Login dan hapus Start dari back stack
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                    }
                }
            )
        }

        // --- 2. Login Screen ---
        composable(route = Screen.Login.route) {
            LoginScreen(
                // ✅ Navigasi SUKSES: Langsung ke Main dan hapus semua tumpukan login
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true } // Bersihkan Login
                        popUpTo(Screen.Start.route) { inclusive = true } // Opsional: Bersihkan Start
                    }
                },
                // Navigasi ke Sign Up
                onSignUpClicked = {
                    navController.navigate(Screen.SignUp.route)
                },
                // Tombol Kembali
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }

        // --- 3. Sign Up Screen ---
        composable(route = Screen.SignUp.route) {
            SignUpScreen(
                // ✅ Navigasi KOMPLIT: Kembali ke Login Screen
                onSignUpComplete = {
                    // Kembali ke Login Screen dan hapus SignUp dari back stack.
                    // Gunakan popUpTo dengan inclusive=true jika SignUpScreen tidak boleh ada di tumpukan.
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                // Navigasi ke Login (Jika ada tombol "Sudah punya akun?")
                onLoginClicked = {
                    // Kembali ke Login, jika Login sudah ada di tumpukan, hindari duplikasi.
                    navController.popBackStack(Screen.Login.route, inclusive = false)
                },
                // Tombol Kembali
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }

        // --- 4. Main Screen (Halaman Utama) ---
        composable(route = Screen.Main.route) {
            HomeScreen()
        }
    }
}