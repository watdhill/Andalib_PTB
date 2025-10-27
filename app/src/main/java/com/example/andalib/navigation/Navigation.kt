package com.example.andalib.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.andalib.screen.HomeScreen
import com.example.andalib.screen.LoginScreen
import com.example.andalib.screen.SignUpScreen
import com.example.andalib.screen.StartScreen

/**
 * Sealed class untuk define semua routes dalam aplikasi
 */
sealed class Screen(val route: String) {
    object Start : Screen("start")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Main : Screen("main") // Main screen dengan bottom navigation
}

// Kredensial Admin
private const val ADMIN_EMAIL = "admin@andalib.com"
private const val ADMIN_PASS = "admin123"

/**
 * Navigation graph untuk Andalib app
 */
@Composable
fun AndalibNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Start.route
    ) {
        // Start Screen (Splash)
        composable(route = Screen.Start.route) {
            StartScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                    }
                }
            )
        }

        // Login Screen
        composable(route = Screen.Login.route) {
            LoginScreen(
                // PERUBAHAN: onLoginClicked sekarang menerima callback kegagalan
                onLoginClicked = { email, password, onFailure ->
                    // Logika pengecekan
                    if (email == ADMIN_EMAIL && password == ADMIN_PASS) {
                        // Login berhasil - navigasi ke MainScreen
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        // Login gagal - panggil callback untuk menampilkan Snackbar
                        onFailure("Login Gagal: Email atau Password salah")
                    }
                },
                onSignUpClicked = {
                    navController.navigate(Screen.SignUp.route)
                },
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }

        // Sign Up Screen
        composable(route = Screen.SignUp.route) {
            SignUpScreen(
                onSignUpClicked = {
                    // Setelah signup berhasil, langsung ke MainScreen
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onLoginClicked = {
                    navController.popBackStack(Screen.Login.route, inclusive = false)
                },
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }

        // Main Screen dengan Bottom Navigation
        composable(route = Screen.Main.route) {
            HomeScreen()
        }
    }
}


