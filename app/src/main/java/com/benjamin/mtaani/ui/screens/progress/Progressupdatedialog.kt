package com.benjamin.mtaani.ui.screens.progress


import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.benjamin.mtaani.models.Issue
import com.benjamin.mtaani.ui.theme.KenyanGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun ProgressUpdateDialog(
    issue: Issue,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    var caption by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { photoUri = it } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Post Progress Update", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KenyanGreen)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "Show the community the current state of: ${issue.category}",
                    fontSize = 12.sp, color = Color.Gray
                )

                Spacer(Modifier.height(16.dp))

                // Photo picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, if (photoUri != null) KenyanGreen else Color.LightGray, RoundedCornerShape(12.dp))
                        .clickable { photoLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null,
                                tint = Color.Gray, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Tap to add a photo", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Caption
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Describe what you see…", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KenyanGreen,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    maxLines = 3
                )

                Spacer(Modifier.height(16.dp))

                // Submit
                Button(
                    onClick = {
                        if (photoUri == null) {
                            Toast.makeText(context, "Please add a photo", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            isUploading = true
                            try {
                                // Upload photo to Storage
                                val photoRef = storage.reference
                                    .child("progressUpdates/${issue.id}/${UUID.randomUUID()}.jpg")
                                photoRef.putFile(photoUri!!).await()
                                val downloadUrl = photoRef.downloadUrl.await().toString()

                                // Save update document
                                db.collection("issues")
                                    .document(issue.id)
                                    .collection("progressUpdates")
                                    .add(
                                        mapOf(
                                            "uid"       to uid,
                                            "caption"   to caption.trim(),
                                            "photoUrl"  to downloadUrl,
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                    ).await()

                                onSuccess()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KenyanGreen),
                    enabled = !isUploading
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Uploading…", color = Color.White)
                    } else {
                        Text("Post Update", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}