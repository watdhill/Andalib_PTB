package com.example.andalib.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.andalib.R
import com.example.andalib.ui.theme.AndalibDarkBlue
import com.example.andalib.ui.theme.AndalibWhite
import com.example.andalib.ui.theme.AndalibTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {

    // Memberikan delay 2 detik sebelum pindah
    LaunchedEffect(key1 = true) {
        delay(2000L)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AndalibDarkBlue), // Latar belakang utama (Header Biru Tua)
        contentAlignment = Alignment.Center
    ) {

        // Bagian Putih (Body Utama)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                // Curve Shape
                .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp))
                .background(AndalibWhite)
                .fillMaxHeight(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Pusatkan konten
        ) {
            // Logo Andalib: Ukuran besar di tengah
            Image(
                // Mengambil logo dari drawable/logo.jpg
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo Andalib",
                modifier = Modifier.size(600.dp)
            )
            // Teks "ANDALIB" dan Tombol "Selanjutnya" dihilangkan.
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    AndalibTheme {
        SplashScreen {}
    }
}