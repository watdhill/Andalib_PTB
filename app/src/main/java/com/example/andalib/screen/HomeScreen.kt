package com.example.andalib.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.andalib.R
import com.example.andalib.bottomNavItems
import com.example.andalib.ui.theme.ThemePreferences
import com.example.andalib.screen.Borrowing.BorrowingScreen
import com.example.andalib.screen.member.MembersScreen
import com.example.andalib.screen.pengembalian.ReturnScreen
import com.example.andalib.ui.theme.AndalibDarkBlue
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.andalib.ui.theme.LocalThemePreferences
import com.example.andalib.data.TokenManager
import com.example.andalib.data.network.createDashboardService
import com.example.andalib.data.network.DashboardStats
import com.example.andalib.data.network.RecentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val drawerItems = listOf(
        DrawerItem("Settings", "settings", Icons.Default.Settings),
        DrawerItem("About Us", "about", Icons.Default.Info)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                currentRoute = currentRoute,
                items = drawerItems,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    ) {
        Scaffold(
            bottomBar = {
                // Kalau lagi di settings/about, bottom bar tetap ditampilkan (boleh).
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("home") {
                    HomeContent(
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }

                composable("books") { BookScreen() }
                composable("borrowing") { BorrowingScreen() }
                composable("return") { ReturnScreen() }
                composable("members") { MembersScreen() }

                // Sidebar pages
                composable("settings") {
                    SettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("about") {
                    AboutUsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

private data class DrawerItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun DrawerContent(
    currentRoute: String,
    items: List<DrawerItem>,
    onItemClick: (String) -> Unit
) {
    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
        modifier = Modifier.width(280.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        // Header drawer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Andalib",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Menu",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(10.dp))
        Divider()
        Spacer(Modifier.height(6.dp))

        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationDrawerItem(
                label = { Text(item.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                selected = selected,
                onClick = { onItemClick(item.route) },
                icon = { Icon(item.icon, contentDescription = item.title) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        Spacer(Modifier.height(8.dp))
        Divider()

        Spacer(Modifier.height(10.dp))
        Text(
            text = "© Andalib",
            modifier = Modifier.padding(horizontal = 18.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
    }
}

// =====================
// HOME CONTENT (Dashboard)
// =====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var stats by remember { mutableStateOf<DashboardStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Load dashboard stats
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            val token = tokenManager.getToken()
            if (token != null) {
                val service = createDashboardService(token)
                val response = withContext(Dispatchers.IO) {
                    service.getDashboardStats()
                }
                if (response.success) {
                    stats = response.data
                } else {
                    errorMessage = response.message
                }
            } else {
                errorMessage = "Token tidak ditemukan"
            }
        } catch (e: Exception) {
            errorMessage = "Gagal memuat data: ${e.message}"
            android.util.Log.e("HomeContent", "Error loading dashboard", e)
        } finally {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard Perpustakaan",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AndalibDarkBlue,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Selamat Datang di Perpustakaan",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (stats != null) {
                // Statistics Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Buku",
                        value = stats!!.totalBooks.toString(),
                        icon = Icons.Default.Book,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Dipinjam",
                        value = stats!!.activeBorrowings.toString(),
                        icon = Icons.Default.ShoppingCart,
                        color = Color(0xFFFFA726),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Anggota",
                        value = stats!!.totalMembers.toString(),
                        icon = Icons.Default.People,
                        color = Color(0xFF66BB6A),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Terlambat",
                        value = stats!!.overdueBorrowings.toString(),
                        icon = Icons.Default.Warning,
                        color = Color(0xFFEF5350),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Aktivitas Terkini",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (stats!!.recentActivities.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📚 Tidak ada aktivitas terbaru",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    stats!!.recentActivities.forEach { activity ->
                        ActivityCard(activity = activity)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = color
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ActivityCard(activity: RecentActivity) {
    val statusColor = when (activity.status) {
        "returned" -> Color(0xFF66BB6A)
        "overdue" -> Color(0xFFEF5350)
        else -> Color(0xFFFFA726)
    }
    
    val statusText = when (activity.status) {
        "returned" -> "Dikembalikan"
        "overdue" -> "Terlambat"
        else -> "Dipinjam"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.bookTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${activity.memberName} (${activity.memberNim})",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Pinjam: ${formatDate(activity.borrowDate)}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.2f),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// Helper function to format date
private fun formatDate(dateString: String): String {
    return try {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            .parse(dateString)
        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(date ?: return dateString)
    } catch (e: Exception) {
        dateString.substring(0, 10)
    }
}

// =====================
// SETTINGS SCREEN (Theme)
// =====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    onBack: () -> Unit
) {
    val themePrefs = LocalThemePreferences.current
    val isDark by themePrefs.isDarkTheme.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Dark Mode", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Ganti tampilan aplikasi",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { checked ->
                            scope.launch {
                                themePrefs.setDarkTheme(checked)
                            }
                        }
                    )
                }
            }
        }
    }
}
// =====================
// ABOUT US SCREEN
// =====================
private data class TeamMember(
    val name: String,
    val nim: String,
    val photoRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutUsScreen(
    onBack: () -> Unit
) {
    // GANTI data ini sesuai tim kamu + drawable kamu
    val team = remember {
        listOf(
            TeamMember("Ikhwan Hamidi", "2311521003", R.drawable.ikhwan),
            TeamMember("Mashia Zavira S", "2311522028", R.drawable.vira),
            TeamMember("Rahil Akram H", "2311523012", R.drawable.rahil),
            TeamMember("Della Khairunnisa", "2311523032", R.drawable.della),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Us", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header like screenshot style
            Text(
                text = "ABOUT US",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Andalib adalah aplikasi perpustakaan untuk membantu pengelolaan buku, peminjaman, " +
                        "pengembalian, serta administrasi anggota secara lebih cepat dan rapi.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(22.dp))

            Text(
                text = "THE TEAM",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            // Grid 2 kolom (mirip layout tim di gambar)
            val rows = team.chunked(2)
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    row.forEach { member ->
                        TeamCard(
                            member = member,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TeamCard(
    member: TeamMember,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(member.photoRes),
                    contentDescription = member.name,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = member.name,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = member.nim,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
