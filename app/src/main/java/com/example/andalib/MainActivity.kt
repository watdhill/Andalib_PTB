package com.example.andalib

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.andalib.navigation.AndalibNavigation
import com.example.andalib.ui.theme.AndalibTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndalibTheme {
                AndalibApp()
            }
        }
    }
}

@Composable
fun AndalibApp() {
    val navController = rememberNavController()
    AndalibNavigation(navController = navController)
}