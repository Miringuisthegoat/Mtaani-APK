package com.benjamin.mtaani.ui.screens.detail


import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.benjamin.mtaani.models.Issue
import com.benjamin.mtaani.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(navController: NavController, issueId: String) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    val uid = auth.currentUser?.uid ?: ""

    var issue by remember { mutableStateOf<Issue?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasUpvoted by remember { mutableStateOf(false) }
    var upvoteCount by remember { mutableStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(issueId) {
        try {
            val doc = db.collection("issues").document(issueId).get().await()
            issue = doc.toObject(Issue::class.java)?.copy(id = doc.id)
            upvoteCount = issue?.upvotes ?: 0

            // Check upvote status
            val upvoteDoc = db.collection("issues").document(issueId)
                .collection("upvotes").document(uid).get().await()
            hasUpvoted = upvoteDoc.exists()
        } catch (e: Exception) {
            // handle
        } finally {
            isLoading = false
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Report", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this report? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        db.collection("issues").document(issueId).delete()
                            .addOnSuccessListener {
                                navController.popBackStack()
                            }
                            .addOnFailureListener {
                                isDeleting = false
                                showDeleteDialog = false
                            }
                    }
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Red)
                    } else {
                        Text("Delete", color = Color.Red)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        issue?.category ?: "Issue Detail",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Only show delete if user owns this issue
                    if (issue?.uid != null && issue?.uid == uid) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KenyanGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = SoftGreen,
        floatingActionButton = {
            issue?.let { i ->
                if (i.location.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(i.location)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                            context.startActivity(mapIntent)
                        },
                        icon = { Icon(Icons.Default.Map, contentDescription = null) },
                        text = { Text("View on Map") },
                        containerColor = KenyanGreen,
                        contentColor = Color.White
                    )
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = KenyanGreen)
            }
        } else if (issue == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null,
                        tint = Color.Gray, modifier = Modifier.size(60.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Issue not found", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            val i = issue!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Photo Hero
                if (i.photoUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        AsyncImage(
                            model = i.photoUrl,
                            contentDescription = "Issue photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient overlay at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                                        startY = 100f
                                    )
                                )
                        )
                        // Status badge over image
                        StatusBadgeOverlay(status = i.status, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp))
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    // Title Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = i.category,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = KenyanGreen
                            )
                            Text(
                                text = formatDetailDate(i.timestamp),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        if (i.photoUrl.isEmpty()) {
                            StatusBadgeOverlay(status = i.status)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Severity + Upvotes row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SeverityChip(severity = i.severity)

                        // Upvote button
                        UpvoteButton(
                            count = upvoteCount,
                            hasUpvoted = hasUpvoted,
                            onUpvote = {
                                val issueRef = db.collection("issues").document(issueId)
                                val upvoteRef = issueRef.collection("upvotes").document(uid)
                                if (hasUpvoted) {
                                    upvoteRef.delete()
                                    issueRef.update("upvotes", FieldValue.increment(-1))
                                    upvoteCount--
                                    hasUpvoted = false
                                } else {
                                    upvoteRef.set(mapOf("uid" to uid))
                                    issueRef.update("upvotes", FieldValue.increment(1))
                                    upvoteCount++
                                    hasUpvoted = true
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFE0E0E0))
                    Spacer(Modifier.height(16.dp))

                    // Description
                    DetailSection(title = "Description", icon = Icons.Default.Description) {
                        Text(
                            text = i.description,
                            fontSize = 15.sp,
                            color = Color.DarkGray,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Location
                    DetailSection(title = "Location", icon = Icons.Default.LocationOn) {
                        Text(
                            text = i.location,
                            fontSize = 15.sp,
                            color = Color.DarkGray
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Status Timeline
                    DetailSection(title = "Status", icon = Icons.Default.Timeline) {
                        StatusTimeline(status = i.status)
                    }

                    // Email Sent badge
                    if (i.emailSent == true) {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = KenyanGreen.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.MarkEmailRead,
                                    contentDescription = null,
                                    tint = KenyanGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Email Sent to County",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = KenyanGreen
                                    )
                                    Text(
                                        "Formal email sent to info@nairobi.go.ke",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(100.dp)) // FAB clearance
                }
            }
        }
    }
}

@Composable
fun StatusBadgeOverlay(status: String, modifier: Modifier = Modifier) {
    val (color, icon) = when (status) {
        "Resolved" -> Pair(Color(0xFF388E3C), Icons.Default.CheckCircle)
        "In Progress" -> Pair(Color(0xFFF57C00), Icons.Default.HourglassTop)
        else -> Pair(Color(0xFF1565C0), Icons.Default.ReportProblem)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(status, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SeverityChip(severity: String) {
    val color = when (severity) {
        "Critical" -> Color(0xFFB71C1C)
        "High" -> Color(0xFFFECB2E)
        "Low" -> Color(0xFF43A047)
        else -> Color(0xFFF57C00)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text("$severity Severity", fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun UpvoteButton(count: Int, hasUpvoted: Boolean, onUpvote: () -> Unit) {
    val tint = if (hasUpvoted) KenyanGreen else Color.Gray
    OutlinedButton(
        onClick = onUpvote,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = tint
        ),
        border = BorderStroke(1.5.dp, tint),
        shape = RoundedCornerShape(20.dp)
    ) {
        Icon(
            if (hasUpvoted) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
            contentDescription = "Upvote",
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text("$count", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = KenyanGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = KenyanGreen,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun StatusTimeline(status: String) {
    val steps = listOf("Reported", "In Progress", "Resolved")
    val currentStep = steps.indexOf(status).takeIf { it >= 0 } ?: 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = index <= currentStep
            val isComplete = index < currentStep

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            if (isActive) KenyanGreen else Color(0xFFE0E0E0),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isComplete) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            (index + 1).toString(),
                            fontSize = 12.sp,
                            color = if (isActive) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    step,
                    fontSize = 10.sp,
                    color = if (isActive) KenyanGreen else Color.Gray,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }

            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(2.dp)
                        .background(
                            if (index < currentStep) KenyanGreen else Color(0xFFE0E0E0)
                        )
                )
            }
        }
    }
}

fun formatDetailDate(ts: Long): String {
    if (ts == 0L) return ""
    return SimpleDateFormat("EEEE, MMM dd yyyy • hh:mm a", Locale.getDefault()).format(Date(ts))
}