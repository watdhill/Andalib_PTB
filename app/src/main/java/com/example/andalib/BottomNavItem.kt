package com.example.andalib

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )

    object Books : BottomNavItem(
        route = "books",
        title = "Buku",
        icon = Icons.Default.Book
    )

    object Borrowing : BottomNavItem(
        route = "borrowing",
        title = "Peminjaman",
        icon = Icons.Default.BookmarkAdd
    )

    object Return : BottomNavItem(
        route = "return",
        title = "Pengembalian",
        icon = Icons.Default.AssignmentReturn

    )

    object Members : BottomNavItem(
        route = "members",
        title = "Anggota",
        icon = Icons.Default.People
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Books,
    BottomNavItem.Borrowing,
    BottomNavItem.Return,
    BottomNavItem.Members
)