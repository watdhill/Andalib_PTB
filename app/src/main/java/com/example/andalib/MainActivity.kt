package com.example.andalib

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.andalib.navigation.AndalibNavigation
import com.example.andalib.ui.theme.AndalibTheme
import com.example.andalib.ui.theme.LocalThemePreferences
import com.example.andalib.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "✅ Notifikasi diaktifkan!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "⚠️ Notifikasi dinonaktifkan. Buka Settings untuk mengaktifkan.",
                Toast.LENGTH_LONG
            ).show()

            android.os.Handler(mainLooper).postDelayed({
                openAppSettings()
            }, 1500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        enableEdgeToEdge()
        setContent {
            // ThemePreferences harus dibuat DI dalam setContent (Composable scope)
            val themePrefs = remember { ThemePreferences(applicationContext) }
            val isDark by themePrefs.isDarkTheme.collectAsState(initial = false)

            CompositionLocalProvider(
                LocalThemePreferences provides themePrefs
            ) {
                // Pakai isDark yang sudah di-collect dari DataStore
                AndalibTheme(
                    dynamicColor = false
                ) {
                    AndalibApp()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // already granted
                }

                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }
}

@Composable
fun AndalibApp() {
    val navController = rememberNavController()
    AndalibNavigation(navController = navController)
}
