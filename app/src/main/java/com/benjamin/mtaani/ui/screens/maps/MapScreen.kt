package com.benjamin.mtaani.ui.screens.maps

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.benjamin.mtaani.ui.theme.KenyanGreen
import kotlinx.coroutines.launch

/**
 * A standard Google Maps screen following the "Map with Marker" pattern.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Initial position: Nairobi, Kenya
    val nairobi = LatLng(-1.286389, 36.817223)
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(nairobi, 15f)
    }

    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var selectedLocation by remember { mutableStateOf(nairobi) }
    var showMenu by remember { mutableStateOf(false) }

    // Permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted }
    )

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Select Issue Location", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold 
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
                actions = {
                    // Map Layer Selector
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.Layers, 
                            contentDescription = "Map Layers", 
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu, 
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Default") }, 
                            onClick = { mapType = MapType.NORMAL; showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Satellite") }, 
                            onClick = { mapType = MapType.SATELLITE; showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Terrain") }, 
                            onClick = { mapType = MapType.TERRAIN; showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Hybrid") }, 
                            onClick = { mapType = MapType.HYBRID; showMenu = false }
                        )
                    }
                    
                    // Confirm selection action
                    IconButton(onClick = {
                        val locStr = "${selectedLocation.latitude}, ${selectedLocation.longitude}"
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("selected_location", locStr)
                        navController.popBackStack()
                    }) {
                        Icon(
                            Icons.Default.Check, 
                            contentDescription = "Confirm", 
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KenyanGreen
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (hasLocationPermission) {
                        try {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                location?.let {
                                    val currentLatLng = LatLng(it.latitude, it.longitude)
                                    selectedLocation = currentLatLng
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f)
                                        )
                                    }
                                }
                            }
                        } catch (e: SecurityException) {
                            e.printStackTrace()
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                containerColor = KenyanGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // The core Google Maps component
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = mapType,
                    isMyLocationEnabled = hasLocationPermission
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false // Handled by our custom FAB
                ),
                onMapClick = { latLng ->
                    selectedLocation = latLng
                }
            ) {
                // Marker representing the issue location
                Marker(
                    state = MarkerState(position = selectedLocation),
                    title = "Report Issue Here",
                    snippet = "Tap to move the pin",
                    draggable = true
                )
            }
            
            // Helpful overlay text
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                color = Color.White.copy(alpha = 0.9f),
                tonalElevation = 4.dp
            ) {
                Text(
                    "Tap on the map to place the pin.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }
        }
    }
}
