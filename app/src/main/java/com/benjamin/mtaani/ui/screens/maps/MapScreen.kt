package com.benjamin.mtaani.ui.screens.maps

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import com.benjamin.mtaani.models.Issue
import com.benjamin.mtaani.ui.theme.KenyanGreen
import com.benjamin.mtaani.ui.theme.SoftGreen
import com.benjamin.mtaani.navigation.ROUT_ISSUE_DETAIL

// ── Data ─────────────────────────────────────────────────────────────────────

data class MapIssue(
    val issue: Issue,
    val latLng: LatLng
)

data class CategoryFilter(
    val label: String,
    val icon: ImageVector,
    val color: Color
)

val categoryFilters = listOf(
    CategoryFilter("All",      Icons.Default.FilterList, KenyanGreen),
    CategoryFilter("Roads",    Icons.Default.Route,      Color(0xFFE53935)),
    CategoryFilter("Water",    Icons.Default.WaterDrop,  Color(0xFF1565C0)),
    CategoryFilter("Garbage",  Icons.Default.Delete,     Color(0xFF6D4C41)),
    CategoryFilter("Lighting", Icons.Default.LightMode,  Color(0xFFF9A825)),
    CategoryFilter("Other",    Icons.Default.MoreHoriz,  Color(0xFF546E7A)),
)

fun categoryColor(category: String): Float = when (category.lowercase()) {
    "roads"    -> BitmapDescriptorFactory.HUE_RED
    "water"    -> BitmapDescriptorFactory.HUE_AZURE
    "garbage"  -> BitmapDescriptorFactory.HUE_ORANGE
    "lighting" -> BitmapDescriptorFactory.HUE_YELLOW
    else       -> BitmapDescriptorFactory.HUE_GREEN
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController) {

    val nairobi = LatLng(-1.2921, 36.8219)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(nairobi, 13f)
    }

    var issues by remember { mutableStateOf<List<MapIssue>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedIssue by remember { mutableStateOf<MapIssue?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var totalCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("issues")
            .get()
            .addOnSuccessListener { snapshot ->
                val loaded = snapshot.documents.mapNotNull { doc ->
                    val issue = doc.toObject(Issue::class.java) ?: return@mapNotNull null
                    val parts = issue.location.split(",")
                    val lat = parts.getOrNull(0)?.toDoubleOrNull()
                        ?: (-1.2921 + (Math.random() - 0.5) * 0.05)
                    val lng = parts.getOrNull(1)?.toDoubleOrNull()
                        ?: (36.8219 + (Math.random() - 0.5) * 0.05)
                    MapIssue(issue, LatLng(lat, lng))
                }
                issues = loaded
                totalCount = loaded.size
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    val filtered = remember(issues, selectedFilter) {
        if (selectedFilter == "All") issues
        else issues.filter { it.issue.category.equals(selectedFilter, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Issue Map",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            "$totalCount reports across Nairobi",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(nairobi, 13f)
                    }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Reset", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KenyanGreen)
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Google Map ───────────────────────────────────────────────
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false
                )
            ) {
                filtered.forEach { mapIssue ->
                    Marker(
                        state = MarkerState(position = mapIssue.latLng),
                        title = mapIssue.issue.category,
                        snippet = mapIssue.issue.description.take(60),
                        icon = BitmapDescriptorFactory.defaultMarker(
                            categoryColor(mapIssue.issue.category)
                        ),
                        onClick = {
                            selectedIssue = mapIssue
                            false
                        }
                    )
                }
            }

            // ── Loading overlay ──────────────────────────────────────────
            AnimatedVisibility(
                visible = isLoading,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = KenyanGreen,
                            strokeWidth = 2.dp
                        )
                        Text("Loading reports…", color = KenyanGreen, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // ── Category Filter Pills ────────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoryFilters) { filter ->
                    val selected = selectedFilter == filter.label
                    FilterChip(
                        onClick = { selectedFilter = filter.label },
                        label = { Text(filter.label, fontSize = 12.sp) },
                        selected = selected,
                        leadingIcon = {
                            Icon(
                                filter.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = filter.color,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color.DarkGray
                        ),
                        modifier = Modifier.shadow(
                            if (selected) 4.dp else 1.dp,
                            RoundedCornerShape(50)
                        )
                    )
                }
            }

            // ── Stats Bar ────────────────────────────────────────────────
            // FIX: replaced deprecated Divider with VerticalDivider
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = if (selectedIssue != null) 200.dp else 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    )
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MapStatItem(
                        count = filtered.count { it.issue.status == "Reported" },
                        label = "Reported",
                        color = Color(0xFFE53935)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(30.dp),
                        thickness = 1.dp,
                        color = Color.LightGray
                    )
                    MapStatItem(
                        count = filtered.count { it.issue.status == "In Progress" },
                        label = "In Progress",
                        color = Color(0xFFF9A825)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(30.dp),
                        thickness = 1.dp,
                        color = Color.LightGray
                    )
                    MapStatItem(
                        count = filtered.count { it.issue.status == "Resolved" },
                        label = "Resolved",
                        color = Color(0xFF43A047)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(30.dp),
                        thickness = 1.dp,
                        color = Color.LightGray
                    )
                    MapStatItem(
                        count = filtered.size,
                        label = "Showing",
                        color = KenyanGreen
                    )
                }
            }

            // ── Selected Issue Bottom Card ───────────────────────────────
            AnimatedVisibility(
                visible = selectedIssue != null,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                selectedIssue?.let { mapIssue ->
                    IssuePreviewCard(
                        mapIssue = mapIssue,
                        onDismiss = { selectedIssue = null },
                        onViewDetail = {
                            navController.navigate("$ROUT_ISSUE_DETAIL/${mapIssue.issue.id}")
                        }
                    )
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
fun MapStatItem(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = color
        )
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun IssuePreviewCard(
    mapIssue: MapIssue,
    onDismiss: () -> Unit,
    onViewDetail: () -> Unit
) {
    val issue = mapIssue.issue

    val statusColor = when (issue.status) {
        "Resolved"    -> Color(0xFF43A047)
        "In Progress" -> Color(0xFFF9A825)
        else          -> Color(0xFFE53935)
    }
    val severityColor = when (issue.severity) {
        "Critical" -> Color(0xFFB71C1C)
        "High"     -> Color(0xFFE53935)
        "Low"      -> Color(0xFF43A047)
        else       -> Color(0xFFF9A825)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SoftGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = KenyanGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(issue.category, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(issue.location.take(35), fontSize = 11.sp, color = Color.Gray)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = issue.description.take(100) +
                        if (issue.description.length > 100) "…" else "",
                fontSize = 13.sp,
                color = Color.DarkGray,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(10.dp))

            // FIX: emailSent is a String field — compare with "true"
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IssueChip(label = issue.status, color = statusColor)
                IssueChip(label = issue.severity, color = severityColor)
                // ✅ Direct Boolean check
                if (issue.emailSent) {
                    IssueChip(label = "Email Sent", color = KenyanGreen)
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onViewDetail,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = KenyanGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("View Full Report")
            }
        }
    }
}

@Composable
fun IssueChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}