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
                // PERUBAHAN: Hanya butuh onLoginSuccess yang dipicu oleh ViewModel
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
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
                // PERBAIKAN: Ganti 'onSignUpSuccess' menjadi 'onSignUpComplete'
                // dan arahkan navigasi ke LoginScreen
                onSignUpComplete = {
                    // Setelah signup berhasil, kembali ke LoginScreen.
                    // Gunakan popUpTo untuk membersihkan tumpukan Sign Up.
                    navController.navigate(Screen.Login.route) {
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