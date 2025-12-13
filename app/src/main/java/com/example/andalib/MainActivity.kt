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
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.andalib.navigation.AndalibNavigation
import com.example.andalib.ui.theme.AndalibTheme

class MainActivity : ComponentActivity() {
    
    // Permission launcher untuk POST_NOTIFICATIONS
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "✅ Notifikasi diaktifkan!", Toast.LENGTH_SHORT).show()
        } else {
            // Permission denied - show toast and guide to settings
            Toast.makeText(
                this, 
                "⚠️ Notifikasi dinonaktifkan. Buka Settings untuk mengaktifkan.", 
                Toast.LENGTH_LONG
            ).show()
            
            // Auto-open app settings after 1 second
            android.os.Handler(mainLooper).postDelayed({
                openAppSettings()
            }, 1500)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission untuk Android 13+ (API 33+)
        requestNotificationPermission()
        
        enableEdgeToEdge()
        setContent {
            AndalibTheme {
                AndalibApp()
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
                    // Permission already granted
                }
                else -> {
                    // Request permission
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
            // Fallback to general settings if specific settings fail
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
