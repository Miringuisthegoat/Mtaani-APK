package com.benjamin.mtaani.ui.screens.home

import android.R.attr.id
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.benjamin.mtaani.R
import com.benjamin.mtaani.navigation.ROUT_ABOUT
import com.benjamin.mtaani.navigation.ROUT_HOME
import com.benjamin.mtaani.navigation.ROUT_LOGIN
import com.benjamin.mtaani.navigation.ROUT_MAP
import com.benjamin.mtaani.navigation.ROUT_MY_REPORTS
import com.benjamin.mtaani.navigation.ROUT_PROFILE
import com.benjamin.mtaani.navigation.ROUT_REPORT_ISSUE
import com.benjamin.mtaani.navigation.issueDetailRoute
import com.benjamin.mtaani.ui.theme.KenyanGreen
import com.benjamin.mtaani.ui.theme.LightGreen
import com.benjamin.mtaani.ui.theme.SoftGreen
import kotlinx.coroutines.launch

// Data classes
data class IssueCategory(val name: String, val icon: ImageVector)
data class RecentReport(
    val id: String,
    val title: String,
    val location: String,
    val time: String,
    val status: String
)

@Composable
fun HomeScreen(navController: NavController) {
    val userName = "Benjamin"
    var selectedBottomIndex by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val categories = listOf(
        IssueCategory("Garbage", Icons.Default.Delete),
        IssueCategory("Potholes", Icons.Default.Warning),
        IssueCategory("Street Lights", Icons.Default.Star),
        IssueCategory("Water Leakage", Icons.Default.Info),
    )

    val recentReports = listOf(
        RecentReport("1", "Pothole on River Road", "Nairobi CBD", "Today, 8:30 AM", "In Progress"),
        RecentReport("2", "Garbage not collected at Koinange St.", "Koinange St, Nairobi", "Yesterday, 4:15 PM", "Resolved"),
        RecentReport("3", "Broken street light near the market", "Gikomba, Nairobi", "May 12, 2024", "Reported"),
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KenyanGreen)
                        .padding(24.dp)
                ) {
                    Column {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Mtaani Menu",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text("🗺️ Map Screen") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(ROUT_MAP)
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("📜 My Reports") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(ROUT_MY_REPORTS)
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    label = { Text("🔔 Updates Screen") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        /* Navigate to Updates */
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },

                    label = { Text("👤 Profile Screen") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(ROUT_PROFILE)
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = selectedBottomIndex == 0,
                        onClick = {
                            selectedBottomIndex = 0
                            navController.navigate(ROUT_HOME)
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = "About") },
                        label = { Text("About") },
                        selected = selectedBottomIndex == 1,
                        onClick = {
                            selectedBottomIndex = 1
                            navController.navigate(ROUT_ABOUT)
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "My Reports") },
                        label = { Text("My Reports") },
                        selected = selectedBottomIndex == 2,
                        onClick = {
                            selectedBottomIndex = 2
                            navController.navigate(ROUT_MY_REPORTS)
                        }
                    )
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                // Header
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KenyanGreen)
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "Open Menu",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Hello, $userName 👋",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Let's make our community better",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            // Logo
                            Image(
                                painter = painterResource(id = R.drawable.mtaani_logo),
                                contentDescription = "Mtaani Logo",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }

                // Report Button
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KenyanGreen)
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Button(
                            onClick = { navController.navigate(ROUT_REPORT_ISSUE)},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = KenyanGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Report an Issue",
                                color = KenyanGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Popular Issues
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Popular Issues",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Text(
                                "View all",
                                fontSize = 13.sp,
                                color = LightGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(categories) { category ->
                                CategoryItem(category = category)
                            }
                        }
                    }
                }

                // Recent Reports
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Reports",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Text(
                            "View all",
                            fontSize = 13.sp,
                            color = LightGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Report Cards
                items(recentReports) { report ->
                    ReportCard(
                        report = report,
                        onClick = {
                            navController.navigate(issueDetailRoute(report.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryItem(category: IssueCategory) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(SoftGreen, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.name,
                tint = KenyanGreen,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = category.name,
            fontSize = 11.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ReportCard(report: RecentReport, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Issue Icon placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(SoftGreen, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = KenyanGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    report.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    report.time,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusBadge(status = report.status)
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "In Progress" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "Resolved" -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
        else -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(rememberNavController())
}