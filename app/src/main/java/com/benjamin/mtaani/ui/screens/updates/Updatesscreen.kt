package com.benjamin.mtaani.ui.screens.updates

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.benjamin.mtaani.models.Issue
import com.benjamin.mtaani.ui.theme.KenyanGreen
import com.benjamin.mtaani.ui.theme.SoftGreen
import java.text.SimpleDateFormat
import java.util.*

// ── Update model ──────────────────────────────────────────────────────────────

data class IssueUpdate(
    val issue: Issue,
    val changeType: UpdateType,
    val previousValue: String = "",
    val newValue: String = ""
)

enum class UpdateType(
    val label: String,
    val icon: ImageVector,
    val color: Color,
) {
    STATUS_CHANGE(
        label = "Status Update",
        icon = Icons.Default.Sync,
        color = Color(0xFF1565C0),
    ),
    EMAIL_SENT(
        label = "Email Sent",
        icon = Icons.Default.Email,
        color = Color(0xFF6A1B9A),
    ),
    RESOLVED(
        label = "Resolved",
        icon = Icons.Default.CheckCircle,
        color = Color(0xFF2E7D32),
    ),
    NEW_REPORT(
        label = "New Report",
        icon = Icons.Default.AddCircle,
        color = KenyanGreen,
    ),
    UPVOTE(
        label = "Community Support",
        icon = Icons.Default.ThumbUp,
        color = Color(0xFFF57F17),
    )
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(navController: NavController) {

    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var updates by remember { mutableStateOf<List<IssueUpdate>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All Updates", "My Issues")

    // FIX: use rememberPullToRefreshState() with no args (M3 stable API)
    val pullToRefreshState = rememberPullToRefreshState()

    fun loadUpdates() {
        FirebaseFirestore.getInstance()
            .collection("issues")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snapshot ->
                val loaded = mutableListOf<IssueUpdate>()
                snapshot.documents.forEach { doc ->
                    val issue = doc.toObject(Issue::class.java) ?: return@forEach
                    // ✅ Direct Boolean check
                    if (issue.emailSent) {
                        loaded.add(IssueUpdate(issue, UpdateType.EMAIL_SENT))
                    }
                    when (issue.status) {
                        "Resolved" ->
                            loaded.add(IssueUpdate(issue, UpdateType.RESOLVED))
                        "In Progress" ->
                            loaded.add(
                                IssueUpdate(
                                    issue, UpdateType.STATUS_CHANGE,
                                    previousValue = "Reported",
                                    newValue = "In Progress"
                                )
                            )
                        else ->
                            loaded.add(IssueUpdate(issue, UpdateType.NEW_REPORT))
                    }
                    if (issue.upvotes > 0) {
                        loaded.add(
                            IssueUpdate(
                                issue, UpdateType.UPVOTE,
                                newValue = issue.upvotes.toString()
                            )
                        )
                    }
                }
                updates = loaded.sortedByDescending { it.issue.timestamp }
                isLoading = false
                isRefreshing = false
            }
            .addOnFailureListener {
                isLoading = false
                isRefreshing = false
            }
    }

    LaunchedEffect(Unit) { loadUpdates() }

    val displayedUpdates = remember(updates, selectedTab, currentUid) {
        if (selectedTab == 1) updates.filter { it.issue.uid == currentUid }
        else updates
    }

    val resolvedCount  = remember(updates) { updates.count { it.changeType == UpdateType.RESOLVED } }
    val inProgressCount = remember(updates) { updates.count { it.changeType == UpdateType.STATUS_CHANGE } }
    val emailCount     = remember(updates) { updates.count { it.changeType == UpdateType.EMAIL_SENT } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Updates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            "${displayedUpdates.size} activity items",
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
                        isRefreshing = true
                        loadUpdates()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KenyanGreen)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Summary Banner ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KenyanGreen)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryCard(count = resolvedCount,    label = "Resolved",    color = Color(0xFF81C784))
                SummaryCard(count = inProgressCount,  label = "In Progress", color = Color(0xFFFFD54F))
                SummaryCard(count = emailCount,       label = "Emails Sent", color = Color(0xFF90CAF9))
            }

            // ── Tabs ─────────────────────────────────────────────────────
            // FIX: replaced TabRowDefaults.SecondaryIndicator (deprecated) with Box indicator
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SoftGreen,
                contentColor = KenyanGreen,
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp)
                            .background(
                                KenyanGreen,
                                RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                            )
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // ── Content with Pull-to-Refresh ─────────────────────────────
            // FIX: PullToRefreshBox replaces deprecated PullToRefreshContainer + nestedScroll pattern
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    loadUpdates()
                },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = KenyanGreen)
                                Spacer(Modifier.height(12.dp))
                                Text("Loading activity…", color = Color.Gray)
                            }
                        }
                    }

                    displayedUpdates.isEmpty() -> {
                        EmptyUpdatesState(isMyIssues = selectedTab == 1)
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            val grouped = displayedUpdates.groupBy { update ->
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    .format(Date(update.issue.timestamp))
                            }
                            grouped.forEach { (date, dayUpdates) ->
                                item { DateHeader(date = date) }
                                items(dayUpdates) { update ->
                                    UpdateTimelineItem(update = update)
                                }
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
fun SummaryCard(count: Int, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = count.toString(),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DateHeader(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // FIX: HorizontalDivider replaces deprecated Divider
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(50))
                .background(SoftGreen)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(date, fontSize = 11.sp, color = KenyanGreen, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
    }
}

@Composable
fun UpdateTimelineItem(update: IssueUpdate) {
    val type = update.changeType

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline spine
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(type.color.copy(alpha = 0.12f))
                    .border(1.5.dp, type.color.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    tint = type.color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(type.color.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
            )
        }

        // Content card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(type.color.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            type.label,
                            fontSize = 10.sp,
                            color = type.color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = formatRelativeTime(update.issue.timestamp),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "${update.issue.category} — ${update.issue.location.take(40)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF212121)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = update.issue.description.take(80) +
                            if (update.issue.description.length > 80) "…" else "",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )

                // Status change arrows
                if (update.changeType == UpdateType.STATUS_CHANGE &&
                    update.previousValue.isNotEmpty()
                ) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(update.previousValue, Color.Gray)
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.Gray
                        )
                        StatusPill(update.newValue, KenyanGreen)
                    }
                }

                // Upvote detail
                if (update.changeType == UpdateType.UPVOTE && update.newValue.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "👍 ${update.newValue} people support this report",
                        fontSize = 11.sp,
                        color = Color(0xFFF57F17)
                    )
                }

                // Severity bar
                Spacer(Modifier.height(6.dp))
                val severityColor = when (update.issue.severity) {
                    "Critical" -> Color(0xFFB71C1C)
                    "High"     -> Color(0xFFE53935)
                    "Low"      -> Color(0xFF43A047)
                    else       -> Color(0xFFF9A825)
                }
                val filledBars = when (update.issue.severity) {
                    "Critical" -> 4; "High" -> 3; "Medium" -> 2; else -> 1
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(6.dp, 10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (index < filledBars) severityColor
                                    else Color.LightGray
                                )
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(update.issue.severity, fontSize = 10.sp, color = severityColor)
                }
            }
        }
    }
}

@Composable
fun StatusPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyUpdatesState(isMyIssues: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SoftGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.NotificationsNone,
                    contentDescription = null,
                    tint = KenyanGreen,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                text = if (isMyIssues) "No updates on your reports" else "No activity yet",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF212121)
            )
            Text(
                text = if (isMyIssues)
                    "When county updates your reported issues, you'll see them here."
                else
                    "Community activity and status changes will appear here.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ── Utils ─────────────────────────────────────────────────────────────────────

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000          -> "Just now"
        diff < 3_600_000       -> "${diff / 60_000}m ago"
        diff < 86_400_000      -> "${diff / 3_600_000}h ago"
        diff < 7 * 86_400_000  -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}