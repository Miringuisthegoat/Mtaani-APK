package com.benjamin.mtaani.ui.screens.maps

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.benjamin.mtaani.models.Issue
import com.benjamin.mtaani.navigation.ROUT_ISSUE_DETAIL
import com.benjamin.mtaani.ui.theme.KenyanGreen
import com.benjamin.mtaani.ui.theme.SoftGreen
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale

// ── Data ──────────────────────────────────────────────────────────────────────

data class MapIssue(val issue: Issue, val latLng: GeoPoint)

data class CategoryFilter(val label: String, val icon: ImageVector, val color: Color)

val categoryFilters = listOf(
    CategoryFilter("All",      Icons.Default.FilterList, KenyanGreen),
    CategoryFilter("Roads",    Icons.Default.Route,      Color(0xFFE53935)),
    CategoryFilter("Water",    Icons.Default.WaterDrop,  Color(0xFF1565C0)),
    CategoryFilter("Garbage",  Icons.Default.Delete,     Color(0xFF6D4C41)),
    CategoryFilter("Lighting", Icons.Default.LightMode,  Color(0xFFF9A825)),
    CategoryFilter("Other",    Icons.Default.MoreHoriz,  Color(0xFF546E7A)),
)

// Maps category → a hex color string for OSM markers
fun categoryColorHex(category: String): Int = when (category.lowercase()) {
    "roads"    -> android.graphics.Color.RED
    "water"    -> android.graphics.Color.BLUE
    "garbage"  -> android.graphics.Color.rgb(109, 76, 65)
    "lighting" -> android.graphics.Color.rgb(249, 168, 37)
    else       -> android.graphics.Color.rgb(27, 94, 32) // KenyanGreen
}

enum class MapMode { VIEW, PICK_LOCATION }

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Configure OSMDroid user agent (required — use your app package)
    Configuration.getInstance().userAgentValue = context.packageName

    val nairobi = GeoPoint(-1.2921, 36.8219)

    // Detect mode: did we come from ReportIssueScreen?
    val mode = remember {
        if (navController.previousBackStackEntry
                ?.destination?.route?.contains("report") == true
        ) MapMode.PICK_LOCATION else MapMode.VIEW
    }

    // ── State ────────────────────────────────────────────────────────────────
    var issues by remember { mutableStateOf<List<MapIssue>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedIssue by remember { mutableStateOf<MapIssue?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var totalCount by remember { mutableIntStateOf(0) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // PICK mode state
    var pickedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var pickedAddress by remember { mutableStateOf("") }
    var isGeocoding by remember { mutableStateOf(false) }

    // Hold a reference to the MapView so overlays can be updated
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val locationOverlayRef = remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // ── Permission launcher ──────────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            locationOverlayRef.value?.enableMyLocation()
            locationOverlayRef.value?.enableFollowLocation()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // ── Load Firestore issues ────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        if (mode == MapMode.VIEW) {
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
                        MapIssue(issue, GeoPoint(lat, lng))
                    }
                    issues = loaded
                    totalCount = loaded.size
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        } else {
            isLoading = false
        }
    }

    val filtered = remember(issues, selectedFilter) {
        if (selectedFilter == "All") issues
        else issues.filter { it.issue.category.equals(selectedFilter, ignoreCase = true) }
    }

    // ── Reverse geocode when pin is dropped ──────────────────────────────────
    LaunchedEffect(pickedPoint) {
        val point = pickedPoint ?: return@LaunchedEffect
        isGeocoding = true
        pickedAddress = reverseGeocode(context, point)
        isGeocoding = false
    }

    // ── Update map markers when filter changes ───────────────────────────────
    LaunchedEffect(filtered) {
        val mapView = mapViewRef.value ?: return@LaunchedEffect
        // Remove old issue markers (keep location overlay)
        mapView.overlays.removeAll { it is Marker }
        addIssueMarkers(
            mapView = mapView,
            issues = filtered,
            context = context,
            onMarkerClick = { mapIssue -> selectedIssue = mapIssue }
        )
        mapView.invalidate()
    }

    // ── UI ───────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (mode == MapMode.PICK_LOCATION) "Pin Your Location" else "Issue Map",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            if (mode == MapMode.PICK_LOCATION) "Tap anywhere to drop a pin"
                            else "$totalCount reports across Nairobi",
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
                    // Jump to user GPS
                    IconButton(onClick = {
                        if (hasLocationPermission) {
                            locationOverlayRef.value?.enableFollowLocation()
                        } else {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "My location", tint = Color.White)
                    }
                    // Reset to Nairobi
                    IconButton(onClick = {
                        mapViewRef.value?.controller?.animateTo(nairobi)
                    }) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset", tint = Color.White)
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

            // ── OSMDroid MapView via AndroidView ─────────────────────────
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(14.0)
                        controller.setCenter(nairobi)

                        // My location overlay (blue dot)
                        val locationOverlay = MyLocationNewOverlay(
                            GpsMyLocationProvider(ctx), this
                        )
                        if (hasLocationPermission) {
                            locationOverlay.enableMyLocation()
                            locationOverlay.enableFollowLocation()
                        }
                        overlays.add(locationOverlay)
                        locationOverlayRef.value = locationOverlay

                        // Tap listener for PICK mode
                        if (mode == MapMode.PICK_LOCATION) {
                            val tapOverlay = object : org.osmdroid.views.overlay.Overlay() {
                                override fun onSingleTapConfirmed(
                                    e: android.view.MotionEvent,
                                    mapView: MapView
                                ): Boolean {
                                    val projection = mapView.projection
                                    val geoPoint = projection.fromPixels(
                                        e.x.toInt(), e.y.toInt()
                                    ) as GeoPoint
                                    pickedPoint = geoPoint

                                    // Remove old pick marker, add new one
                                    mapView.overlays.removeAll { overlay ->
                                        overlay is Marker && overlay.id == "pick_marker"
                                    }
                                    val marker = Marker(mapView).apply {
                                        id = "pick_marker"
                                        position = geoPoint
                                        title = "Selected location"
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    }
                                    mapView.overlays.add(marker)
                                    mapView.invalidate()
                                    return true
                                }
                            }
                            overlays.add(tapOverlay)
                        }

                        mapViewRef.value = this
                    }
                },
                update = { mapView ->
                    // Re-add markers when filtered list changes
                    // (handled by LaunchedEffect above via mapViewRef)
                }
            )

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

            // ── Category filters — VIEW mode only ────────────────────────
            if (mode == MapMode.VIEW) {
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
            }

            // ── PICK mode: address preview + confirm button ───────────────
            if (mode == MapMode.PICK_LOCATION) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedVisibility(visible = pickedPoint != null) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = KenyanGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (isGeocoding) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = KenyanGreen
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Finding address…", fontSize = 13.sp, color = Color.Gray)
                                } else {
                                    Text(
                                        pickedAddress.ifBlank { "Unknown location" },
                                        fontSize = 13.sp,
                                        color = Color.DarkGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val point = pickedPoint ?: return@Button
                            val locationString = pickedAddress.ifBlank {
                                "${point.latitude},${point.longitude}"
                            }
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("selected_location", locationString)
                            navController.popBackStack()
                        },
                        enabled = pickedPoint != null && !isGeocoding,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KenyanGreen)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm Location", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            // ── VIEW mode: stats bar ─────────────────────────────────────
            if (mode == MapMode.VIEW) {
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
                        VerticalDivider(Modifier.height(30.dp), thickness = 1.dp, color = Color.LightGray)
                        MapStatItem(
                            count = filtered.count { it.issue.status == "In Progress" },
                            label = "In Progress",
                            color = Color(0xFFF9A825)
                        )
                        VerticalDivider(Modifier.height(30.dp), thickness = 1.dp, color = Color.LightGray)
                        MapStatItem(
                            count = filtered.count { it.issue.status == "Resolved" },
                            label = "Resolved",
                            color = Color(0xFF43A047)
                        )
                        VerticalDivider(Modifier.height(30.dp), thickness = 1.dp, color = Color.LightGray)
                        MapStatItem(
                            count = filtered.size,
                            label = "Showing",
                            color = KenyanGreen
                        )
                    }
                }

                // Selected issue card
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
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun addIssueMarkers(
    mapView: MapView,
    issues: List<MapIssue>,
    context: Context,
    onMarkerClick: (MapIssue) -> Unit
) {
    issues.forEach { mapIssue ->
        val marker = Marker(mapView).apply {
            position = mapIssue.latLng
            title = mapIssue.issue.category
            snippet = mapIssue.issue.description.take(60)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerClickListener { _, _ ->
                onMarkerClick(mapIssue)
                true
            }
        }
        mapView.overlays.add(marker)
    }
}

private suspend fun reverseGeocode(context: Context, point: GeoPoint): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            var result = ""
            geocoder.getFromLocation(point.latitude, point.longitude, 1) { addresses ->
                result = addresses.firstOrNull()?.let { addr ->
                    listOfNotNull(
                        addr.thoroughfare,
                        addr.subLocality,
                        addr.locality
                    ).joinToString(", ")
                } ?: "${point.latitude}, ${point.longitude}"
            }
            delay(600)
            result.ifBlank { "${point.latitude}, ${point.longitude}" }
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1)
            addresses?.firstOrNull()?.let { addr ->
                listOfNotNull(
                    addr.thoroughfare,
                    addr.subLocality,
                    addr.locality
                ).joinToString(", ")
            } ?: "${point.latitude}, ${point.longitude}"
        }
    } catch (e: Exception) {
        "${point.latitude}, ${point.longitude}"
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
fun MapStatItem(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 10.sp, color = Color.Gray)
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
        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(SoftGreen),
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IssueChip(label = issue.status, color = statusColor)
                IssueChip(label = issue.severity, color = severityColor)
                if (issue.emailSent) IssueChip(label = "Email Sent", color = KenyanGreen)
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