package com.benjamin.mtaani.ui.screens.profile


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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.benjamin.mtaani.models.Issue
import com.benjamin.mtaani.models.User
import com.benjamin.mtaani.navigation.ROUT_LOGIN
import com.benjamin.mtaani.ui.theme.KenyanGreen
import com.benjamin.mtaani.ui.theme.LightGreen
import com.benjamin.mtaani.ui.theme.SoftGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val rtdb = remember { FirebaseDatabase.getInstance() }
    val currentUser = auth.currentUser

    var user by remember { mutableStateOf<User?>(null) }
    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editUsername by remember { mutableStateOf("") }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            try {
                // Fetch user from Realtime Database
                val snapshot = rtdb.reference.child("Users").child(uid).get().await()
                user = snapshot.getValue(User::class.java)
                editUsername = user?.username ?: ""

                // Fetch issues count
                val issueSnap = db.collection("issues")
                    .whereEqualTo("uid", uid)
                    .get()
                    .await()
                issues = issueSnap.documents.mapNotNull { it.toObject(Issue::class.java) }
            } catch (e: Exception) {
                // handle
            } finally {
                isLoading = false
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of Mtaani?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        auth.signOut()
                        navController.navigate(ROUT_LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Sign Out", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Username Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Update Username", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editUsername,
                    onValueChange = { editUsername = it },
                    label = { Text("Username") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = KenyanGreen,
                        focusedLabelColor = KenyanGreen
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentUser?.uid?.let { uid ->
                            rtdb.reference.child("Users").child(uid)
                                .child("username").setValue(editUsername)
                            user = user?.copy(username = editUsername)
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("Save", color = KenyanGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
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
        if (isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = KenyanGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Profile Header Card
                ProfileHeaderCard(
                    user = user,
                    email = currentUser?.email ?: "",
                    joinDate = currentUser?.metadata?.creationTimestamp ?: 0L
                )

                Spacer(Modifier.height(16.dp))

                // Stats Row
                IssueStatsRow(issues = issues)

                Spacer(Modifier.height(16.dp))

                // Account Section
                ProfileSection(title = "Account") {
                    ProfileRow(
                        icon = Icons.Default.Person,
                        label = "Username",
                        value = user?.username ?: "—"
                    )
                    ProfileDivider()
                    ProfileRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = currentUser?.email ?: "—"
                    )
                    ProfileDivider()
                    ProfileRow(
                        icon = Icons.Default.Shield,
                        label = "Role",
                        value = user?.role?.replaceFirstChar { it.uppercase() } ?: "User"
                    )
                    ProfileDivider()
                    ProfileRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Joined",
                        value = formatTimestamp(currentUser?.metadata?.creationTimestamp ?: 0L)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // App Section
                ProfileSection(title = "App") {
                    ProfileActionRow(
                        icon = Icons.Default.Info,
                        label = "About Mtaani",
                        onClick = { navController.navigate("about") }
                    )
                    ProfileDivider()
                    ProfileActionRow(
                        icon = Icons.Default.Share,
                        label = "Share App",
                        onClick = { /* share intent */ }
                    )
                    ProfileDivider()
                    ProfileActionRow(
                        icon = Icons.Default.Star,
                        label = "Rate on Play Store",
                        onClick = { /* rate intent */ }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Sign Out Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { showLogoutDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color.Red)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "Sign Out",
                            fontSize = 15.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // App version footer
                Text(
                    "Mtaani v1.0 • Sauti ya Wananchi",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ProfileHeaderCard(user: User?, email: String, joinDate: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(KenyanGreen, LightGreen)
                )
            )
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Avatar circle with initials
            val initials = user?.username?.take(2)?.uppercase() ?: email.take(2).uppercase()
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = user?.username ?: "Mwananchi",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = email,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Text(
                    text = user?.role?.replaceFirstChar { it.uppercase() } ?: "User",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun IssueStatsRow(issues: List<Issue>) {
    val total = issues.size
    val resolved = issues.count { it.status == "Resolved" }
    val totalUpvotes = issues.sumOf { it.upvotes }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(Modifier.weight(1f), value = total.toString(), label = "Reports")
        StatCard(Modifier.weight(1f), value = resolved.toString(), label = "Resolved")
        StatCard(Modifier.weight(1f), value = totalUpvotes.toString(), label = "Upvotes")
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, value: String, label: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = KenyanGreen)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            letterSpacing = 1.sp
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun ProfileRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = KenyanGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
        }
    }
}

@Composable
fun ProfileActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = KenyanGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ProfileDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 50.dp),
        color = Color(0xFFF0F0F0),
        thickness = 1.dp
    )
}

fun formatTimestamp(ts: Long): String {
    if (ts == 0L) return "Unknown"
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(ts))
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(rememberNavController())
}
