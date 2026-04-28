package com.benjamin.mtaani.ui.screens.community


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.benjamin.mtaani.models.Issue
import com.benjamin.mtaani.navigation.issueDetailRoute
import com.benjamin.mtaani.ui.screens.progress.ProgressUpdateDialog
import com.benjamin.mtaani.ui.theme.KenyanGreen
import com.benjamin.mtaani.ui.theme.LightGreen
import com.benjamin.mtaani.ui.theme.SoftGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedScreen(navController: NavController) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUid = auth.currentUser?.uid ?: ""

    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedSort by remember { mutableStateOf("Newest") }
    var searchQuery by remember { mutableStateOf("") }
    var dialogIssue by remember { mutableStateOf<Issue?>(null) }

    // Track which issues the current user has upvoted
    var upvotedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val categories = listOf("All", "Garbage", "Potholes", "Street Lights", "Water Leakage", "Drainage", "Other")
    val sortOptions = listOf("Newest", "Most Upvoted", "Critical First")

    LaunchedEffect(Unit) {
        try {
            val snapshot = db.collection("issues")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()

            issues = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Issue::class.java)?.copy(id = doc.id)
            }

            // Fetch this user's upvotes
            if (currentUid.isNotEmpty()) {
                val upvoteSnap = db.collection("upvotes")
                    .whereEqualTo("uid", currentUid)
                    .get().await()
                upvotedIds = upvoteSnap.documents.map { it.getString("issueId") ?: "" }.toSet()
            }
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    // Filter + sort pipeline
    val displayed = issues
        .filter { issue ->
            val matchCat = selectedCategory == "All" || issue.category == selectedCategory
            val matchSearch = searchQuery.isBlank() ||
                    issue.description.contains(searchQuery, ignoreCase = true) ||
                    issue.location.contains(searchQuery, ignoreCase = true) ||
                    issue.category.contains(searchQuery, ignoreCase = true)
            matchCat && matchSearch
        }
        .let { list ->
            when (selectedSort) {
                "Most Upvoted"  -> list.sortedByDescending { it.upvotes }
                "Critical First" -> list.sortedByDescending {
                    when (it.severity) { "Critical" -> 4; "High" -> 3; "Medium" -> 2; else -> 1 }
                }
                else -> list.sortedByDescending { it.timestamp }
            }
        }

    // Open to ALL — reporter, upvoter, general public
    dialogIssue?.let { issue ->
        ProgressUpdateDialog(
            issue = issue,
            onDismiss = { dialogIssue = null },
            onSuccess = { dialogIssue = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community Feed", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Search bar ───────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search issues…", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = KenyanGreen) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KenyanGreen,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            // ── Stats strip ──────────────────────────────────────────────
            FeedStatsStrip(issues = issues)

            // ── Sort chips ───────────────────────────────────────────────
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(sortOptions) { opt ->
                    FilterChip(
                        selected = selectedSort == opt,
                        onClick = { selectedSort = opt },
                        label = { Text(opt, fontSize = 12.sp) },
                        leadingIcon = {
                            val icon = when (opt) {
                                "Most Upvoted"   -> Icons.Default.ThumbUp
                                "Critical First" -> Icons.Default.Warning
                                else             -> Icons.Default.Schedule
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KenyanGreen,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            // ── Category chips ───────────────────────────────────────────
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LightGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Feed list ────────────────────────────────────────────────
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KenyanGreen)
                }
                displayed.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null,
                            modifier = Modifier.size(64.dp), tint = LightGreen.copy(alpha = 0.5f))
                        Spacer(Modifier.height(12.dp))
                        Text("No issues found", fontSize = 16.sp, color = KenyanGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayed, key = { it.id }) { issue ->
                        CommunityIssueCard(
                            issue = issue,
                            isUpvoted = issue.id in upvotedIds,
                            isOwner = issue.uid == currentUid,
                            onClick = { navController.navigate(issueDetailRoute(issue.id)) },
                            onUpvote = {
                                if (currentUid.isEmpty()) return@CommunityIssueCard
                                val issueRef = db.collection("issues").document(issue.id)
                                if (issue.id in upvotedIds) {
                                    // Undo upvote
                                    issueRef.update("upvotes", (issue.upvotes - 1).coerceAtLeast(0))
                                    db.collection("upvotes")
                                        .whereEqualTo("uid", currentUid)
                                        .whereEqualTo("issueId", issue.id)
                                        .get()
                                        .addOnSuccessListener { snap ->
                                            snap.documents.forEach { it.reference.delete() }
                                        }
                                    upvotedIds = upvotedIds - issue.id
                                    issues = issues.map { i ->
                                        if (i.id == issue.id) i.copy(upvotes = (i.upvotes - 1).coerceAtLeast(0)) else i
                                    }
                                } else {
                                    // Add upvote
                                    issueRef.update("upvotes", issue.upvotes + 1)
                                    db.collection("upvotes").add(
                                        mapOf("uid" to currentUid, "issueId" to issue.id)
                                    )
                                    upvotedIds = upvotedIds + issue.id
                                    issues = issues.map { i ->
                                        if (i.id == issue.id) i.copy(upvotes = i.upvotes + 1) else i
                                    }
                                }
                            },
                            onPostUpdate = { dialogIssue = issue }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Stats strip ──────────────────────────────────────────────────────────────

@Composable
fun FeedStatsStrip(issues: List<Issue>) {
    val open     = issues.count { it.status == "Reported" }
    val progress = issues.count { it.status == "In Progress" }
    val resolved = issues.count { it.status == "Resolved" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StripStat(Modifier.weight(1f), open,     "Open",        Color(0xFF1565C0))
        StripStat(Modifier.weight(1f), progress, "In Progress", Color(0xFFF57C00))
        StripStat(Modifier.weight(1f), resolved, "Resolved",    Color(0xFF388E3C))
    }
}

@Composable
fun StripStat(modifier: Modifier, count: Int, label: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = color)
        }
    }
}

// ── Community issue card ──────────────────────────────────────────────────────

@Composable
fun CommunityIssueCard(
    issue: Issue,
    isUpvoted: Boolean,
    isOwner: Boolean,
    onClick: () -> Unit,
    onUpvote: () -> Unit,
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

    val timeAgo = remember(issue.timestamp) { formatTimeAgo(issue.timestamp) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Header row ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category pill + owner badge
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = KenyanGreen.copy(alpha = 0.1f)
                    ) {
                        Text(
                            issue.category,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            color = KenyanGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (isOwner) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(20.dp), color = LightGreen.copy(alpha = 0.15f)) {
                            Text(
                                "You",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                color = LightGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                // Status badge
                Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(
                        issue.status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Description ──────────────────────────────────────────────
            Text(
                issue.description,
                fontSize = 13.sp,
                color = Color.DarkGray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // ── Location + severity + time ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        issue.location.take(26) + if (issue.location.length > 26) "…" else "",
                        fontSize = 11.sp, color = Color.Gray,
                        overflow = TextOverflow.Ellipsis, maxLines = 1
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).background(severityColor, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(issue.severity, fontSize = 11.sp, color = severityColor, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(3.dp))
                Text(timeAgo, fontSize = 10.sp, color = Color.LightGray)
            }

            // ── Photo ────────────────────────────────────────────────────
            if (issue.photoUrl.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                AsyncImage(
                    model = issue.photoUrl,
                    contentDescription = "Issue photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(Modifier.height(8.dp))

            // ── Action row ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upvote button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onUpvote,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isUpvoted) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                            contentDescription = "Upvote",
                            tint = if (isUpvoted) KenyanGreen else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "${issue.upvotes}",
                        fontSize = 12.sp,
                        color = if (isUpvoted) KenyanGreen else Color.Gray,
                        fontWeight = if (isUpvoted) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Email sent indicator
                if (issue.emailSent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = KenyanGreen, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Email sent", fontSize = 11.sp, color = KenyanGreen)
                    }
                }

                // Post progress update — open to everyone
                if (issue.status != "Resolved") {
                    TextButton(
                        onClick = onPostUpdate,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null,
                            modifier = Modifier.size(14.dp), tint = KenyanGreen)
                        Spacer(Modifier.width(4.dp))
                        Text("Update", fontSize = 12.sp, color = KenyanGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours   = TimeUnit.MILLISECONDS.toHours(diff)
    val days    = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1  -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours   < 24 -> "$hours hr ago"
        days    < 7  -> "$days day${if (days != 1L) "s" else ""} ago"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}