package com.example.andalib.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.andalib.R
import com.example.andalib.ui.theme.AndalibTheme
import com.example.andalib.ui.theme.AndalibWhite
import kotlinx.coroutines.delay

@Composable
fun StartScreen(
    onNavigateToLogin: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onNavigateToLogin()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AndalibWhite
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo Andalib",
                modifier = Modifier.size(150.dp)
            )

        }
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun StartScreenPreview() {
    AndalibTheme {
        StartScreen()
    }
}