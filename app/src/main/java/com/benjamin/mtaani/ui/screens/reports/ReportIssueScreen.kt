package com.benjamin.mtaani.ui.screens.reports

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.benjamin.mtaani.navigation.ROUT_MAP
import com.benjamin.mtaani.ui.theme.KenyanGreen
import com.benjamin.mtaani.ui.theme.SoftGreen
import kotlinx.coroutines.launch

data class Category(val name: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueScreen(navController: NavController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val categories = listOf(
        Category("Garbage", Icons.Default.Delete),
        Category("Potholes", Icons.Default.Warning),
        Category("Street Lights", Icons.Default.Star),
        Category("Water Leakage", Icons.Default.Info),
        Category("Drainage", Icons.Default.WaterDrop),
        Category("Other", Icons.Default.MoreVert)
    )

    var selectedCategory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Koinange St, Nairobi — Accurate to 10m") }
    var isLoading by remember { mutableStateOf(false) }
    var isImprovingDescription by remember { mutableStateOf(false) }
    var severity by remember { mutableStateOf("") }

    val capturedPhotos = remember { mutableStateListOf<Bitmap>() }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            capturedPhotos.add(it)
        }
    }

    // Observe result from MapScreen
    val result = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>("selected_location")
        ?.observeAsState()

    LaunchedEffect(result?.value) {
        result?.value?.let { pickedLocation ->
            location = pickedLocation
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Report an Issue",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KenyanGreen
                )
            )
        }
    ) { paddingValues ->
        // Refactored the UI into smaller section composables to resolve rendering issues
        // and improve maintainability.
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF5F5F5))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Category Section
            CategorySection(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            // Location Section
            LocationSection(
                location = location,
                onMapClick = {
                    navController.navigate(ROUT_MAP)
                }
            )

            // Add Photos Section
            PhotoSection(
                capturedPhotos = capturedPhotos,
                onAddPhotoClick = {
                    cameraLauncher.launch()
                }
            )

            // Description Section
            DescriptionSection(
                description = description,
                onDescriptionChange = { description = it },
                selectedCategory = selectedCategory,
                severity = severity,
                isImprovingDescription = isImprovingDescription,
                onImproveDescription = {
                    scope.launch {
                        isImprovingDescription = true
                        description = GeminiHelper.improveDescription(
                            description,
                            selectedCategory
                        )
                        isImprovingDescription = false
                    }
                }
            )


            // Submit Button
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            // Step 1: Estimate severity
                            severity = GeminiHelper.estimateSeverity(description, selectedCategory)

                            // Step 2: Generate formal email
                            val emailContent = GeminiHelper.generateCountyEmail(
                                category = selectedCategory,
                                description = description,
                                location = location
                            )

                            // Step 3: Parse subject and body
                            val subject = EmailSender.parseSubject(emailContent)
                            val body = EmailSender.parseBody(emailContent)

                            // Step 4: Open email app
                            val intent = EmailSender.getEmailIntent(subject, body)
                            context.startActivity(intent)

                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KenyanGreen
                ),
                enabled = selectedCategory.isNotEmpty() &&
                        description.isNotEmpty() &&
                        !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "AI is generating email...",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Submit Report",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CategorySection(
    categories: List<Category>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Select Category",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.take(3).forEach { category ->
                    CategoryChip(
                        category = category,
                        isSelected = selectedCategory == category.name,
                        onClick = { onCategorySelected(category.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.drop(3).forEach { category ->
                    CategoryChip(
                        category = category,
                        isSelected = selectedCategory == category.name,
                        onClick = { onCategorySelected(category.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun LocationSection(location: String, onMapClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Location",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = KenyanGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        location,
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Accurate to 10m",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(SoftGreen, RoundedCornerShape(12.dp))
                    .clickable { onMapClick() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        tint = KenyanGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Map preview (Open Maps to pin)", color = KenyanGreen, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun PhotoSection(
    capturedPhotos: List<Bitmap>,
    onAddPhotoClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Add Photos",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Display captured photos
                capturedPhotos.forEach { bitmap ->
                    PhotoSlot(bitmap = bitmap)
                }

                // Add button (only show if less than 3 photos)
                if (capturedPhotos.size < 3) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(2.dp, Color.LightGray, RoundedCornerShape(12.dp))
                            .clickable { onAddPhotoClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Photo",
                            tint = Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DescriptionSection(
    description: String,
    onDescriptionChange: (String) -> Unit,
    selectedCategory: String,
    severity: String,
    isImprovingDescription: Boolean,
    onImproveDescription: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Description",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                // AI Improve Button
                if (description.isNotEmpty() && selectedCategory.isNotEmpty()) {
                    TextButton(onClick = onImproveDescription) {
                        if (isImprovingDescription) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = KenyanGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = KenyanGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "AI Improve",
                                color = KenyanGreen,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = { Text("Tell us more about the issue...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = KenyanGreen,
                    unfocusedBorderColor = Color.LightGray
                ),
                maxLines = 5
            )
            // Severity badge
            if (severity.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val severityColor = when (severity) {
                    "Low" -> Color(0xFF4CAF50)
                    "Medium" -> Color(0xFFFF9800)
                    "High" -> Color(0xFFFF5722)
                    "Critical" -> Color(0xFFD32F2F)
                    else -> Color.Gray
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = severityColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        "⚠️ Severity: $severity",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = severityColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(
                2.dp,
                if (isSelected) KenyanGreen else Color.LightGray,
                RoundedCornerShape(12.dp)
            )
            .background(
                if (isSelected) SoftGreen else Color.White,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.name,
            tint = if (isSelected) KenyanGreen else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            fontSize = 10.sp,
            color = if (isSelected) KenyanGreen else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun PhotoSlot(bitmap: Bitmap? = null) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(SoftGreen, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = KenyanGreen,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportIssueScreenPreview() {
    ReportIssueScreen(rememberNavController())
}