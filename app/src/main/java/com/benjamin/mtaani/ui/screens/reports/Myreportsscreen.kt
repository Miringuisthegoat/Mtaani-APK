package com.benjamin.mtaani.ui.screens.reports

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.benjamin.mtaani.models.Issue
import com.benjamin.mtaani.navigation.ROUT_COMMUNITY_FEED
import com.benjamin.mtaani.navigation.issueDetailRoute
import com.benjamin.mtaani.ui.screens.progress.ProgressUpdateDialog
import com.benjamin.mtaani.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid ?: ""

    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var updateCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }
    var dialogIssue by remember { mutableStateOf<Issue?>(null) }

    val filters = listOf("All", "Reported", "In Progress", "Resolved")

    fun reload() { isLoading = true }

    LaunchedEffect(uid, isLoading) {
        if (!isLoading) return@LaunchedEffect
        if (uid.isEmpty()) { isLoading = false; return@LaunchedEffect }
        try {
            val snapshot = db.collection("issues")
                .whereEqualTo("uid", uid)
                .get().await()

            val fetched = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Issue::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.timestamp }

            issues = fetched

            val counts = mutableMapOf<String, Int>()
            for (issue in fetched) {
                val upSnap = db.collection("issues")
                    .document(issue.id)
                    .collection("progressUpdates")
                    .get().await()
                counts[issue.id] = upSnap.size()
            }
            updateCounts = counts
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    val filteredIssues = if (selectedFilter == "All") issues
    else issues.filter { it.status == selectedFilter }

    // ── Progress update dialog — open to ALL community members ──────────────
    // No reporter/upvoter check: anyone logged in can post a progress photo.
    dialogIssue?.let { issue ->
        ProgressUpdateDialog(
            issue = issue,
            onDismiss = { dialogIssue = null },
            onSuccess = {
                dialogIssue = null
                reload()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Reports", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(ROUT_COMMUNITY_FEED) }) {
                        Icon(Icons.Default.Groups, contentDescription = "Community Feed", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KenyanGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = SoftGreen
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ReportSummaryRow(issues = issues)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KenyanGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KenyanGreen)
                }
                filteredIssues.isEmpty() -> EmptyReportsState(selectedFilter)
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredIssues, key = { it.id }) { issue ->
                        MyReportCard(
                            issue = issue,
                            updateCount = updateCounts[issue.id] ?: 0,
                            onClick = { navController.navigate(issueDetailRoute(issue.id)) },
                            onPostUpdate = { dialogIssue = issue }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun ReportSummaryRow(issues: List<Issue>) {
    val total = issues.size
    val resolved = issues.count { it.status == "Resolved" }
    val inProgress = issues.count { it.status == "In Progress" }

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryCard(Modifier.weight(1f), total,      "Total",       KenyanGreen,         Icons.AutoMirrored.Filled.ListAlt)
        SummaryCard(Modifier.weight(1f), inProgress, "In Progress", Color(0xFFF57C00),   Icons.Default.HourglassTop)
        SummaryCard(Modifier.weight(1f), resolved,   "Resolved",    Color(0xFF388E3C),   Icons.Default.CheckCircle)
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(count.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun MyReportCard(
    issue: Issue,
    updateCount: Int,
    onClick: () -> Unit,
    onPostUpdate: () -> Unit
) {
    val statusColor = when (issue.status) {
        "Resolved"    -> Color(0xFF388E3C)
        "In Progress" -> Color(0xFFF57C00)
        else          -> Color(0xFF1565C0)
    }
    val severityColor = when (issue.severity) {
        "Critical" -> Color(0xFFB71C1C)
        "High"     -> Color(0xFFE53935)
        "Low"      -> Color(0xFF43A047)
        else       -> Color(0xFFF57C00)
    }

    val daysSince = TimeUnit.MILLISECONDS
        .toDays(System.currentTimeMillis() - issue.timestamp).toInt()
    val nextUpdateDue = (updateCount + 1) * 5
    val daysUntilNext = (nextUpdateDue - daysSince).coerceAtLeast(0)
    val canPostNow = daysSince >= nextUpdateDue && issue.status != "Resolved"

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(5.dp).fillMaxHeight().background(statusColor))

            Column(modifier = Modifier.padding(14.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = issue.category,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = KenyanGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatusBadge(status = issue.status, color = statusColor)
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = issue.description,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = issue.location.take(28) + if (issue.location.length > 28) "…" else "",
                            fontSize = 11.sp, color = Color.Gray
                        )
                    }
                    SeverityTag(severity = issue.severity, color = severityColor)
                }

                if (issue.photoUrl.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    AsyncImage(
                        model = issue.photoUrl,
                        contentDescription = "Issue photo",
                        modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(10.dp))

                ProgressUpdateRow(
                    updateCount = updateCount,
                    canPostNow = canPostNow,
                    daysUntilNext = daysUntilNext,
                    isResolved = issue.status == "Resolved",
                    onPostUpdate = onPostUpdate
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ThumbUp, contentDescription = null, tint = LightGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${issue.upvotes} upvotes", fontSize = 11.sp, color = Color.Gray)
                    }
                    if (issue.emailSent == true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = KenyanGreen, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Email sent", fontSize = 11.sp, color = KenyanGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressUpdateRow(
    updateCount: Int,
    canPostNow: Boolean,
    daysUntilNext: Int,
    isResolved: Boolean,
    onPostUpdate: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = KenyanGreen, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    "$updateCount update${if (updateCount != 1) "s" else ""} posted",
                    fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                when {
                    isResolved -> "Issue resolved ✓"
                    canPostNow -> "Update due now!"
                    else       -> "Next update in $daysUntilNext day${if (daysUntilNext != 1) "s" else ""}"
                },
                fontSize = 11.sp,
                color = when {
                    isResolved -> Color(0xFF388E3C)
                    canPostNow -> Color(0xFFF57C00)
                    else       -> Color.Gray
                }
            )
        }

        // Button visible to ALL users (reporter, upvoters, general public)
        if (!isResolved) {
            Button(
                onClick = onPostUpdate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canPostNow) KenyanGreen else Color(0xFFE0E0E0),
                    contentColor   = if (canPostNow) Color.White  else Color.Gray
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    if (canPostNow) "Post Update" else "Add Early",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, color: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SeverityTag(severity: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(severity, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EmptyReportsState(filter: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(72.dp), tint = LightGreen.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text(
            if (filter == "All") "No reports yet" else "No $filter reports",
            fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = KenyanGreen
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your submitted reports will appear here.",
            fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center
        )
    }
}