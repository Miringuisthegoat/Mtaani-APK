package com.benjamin.mtaani.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.benjamin.mtaani.ui.theme.KenyanGreen
import com.benjamin.mtaani.ui.theme.LightGreen

@Composable
fun AboutScreen(navController: NavController) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF9F9F9)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KenyanGreen)
                    .padding(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Mtaani Logo",
                            tint = KenyanGreen,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Mtaani",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Voice of the Community",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "Version 1.0.0",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AboutCard(
                title = "What is Mtaani?",
                content = "Mtaani is a community-driven civic reporting app that empowers Kenyan citizens to report local issues — such as broken roads, illegal dumping, faulty streetlights, and water problems — directly from their phones. Every report is pinned on a live map for the whole community to see and upvote.",
                icon = Icons.Default.Info
            )

            AboutCard(
                title = "How It Works",
                content = "1. Sign in with your Google account\n2. Browse reported issues on the map\n3. Tap + to report a new issue\n4. Select a category, take a photo, and describe the problem\n5. Submit — your community will see it instantly!",
                icon = Icons.AutoMirrored.Filled.List
            )

            AboutCard(
                title = "Your Privacy",
                content = "All reports are submitted anonymously. Your name is never displayed publicly. We are committed to protecting your personal information and ensuring you can report issues without fear.",
                icon = Icons.Default.Lock
            )

            AboutCard(
                title = "Community Power",
                content = "Upvote issues that matter most to you. The more upvotes an issue gets, the more visible it becomes — helping prioritize what needs fixing first in your neighborhood.",
                icon = Icons.Default.ThumbUp
            )

            AboutCard(
                title = "Our Vision",
                content = "We believe every Kenyan deserves a clean, safe, and well-maintained neighborhood. Mtaani bridges the gap between citizens and accountability, one report at a time. 🇰🇪",
                icon = Icons.Default.Star
            )

            // Developer Card
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "DEVELOPED BY",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "The BirdBox",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = KenyanGreen
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        InfoChip(label = "🇰🇪 Nairobi")
                        InfoChip(label = "📅 2026")
                        InfoChip(label = "🤖 Android")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legal Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Legal & Contact",
                        fontWeight = FontWeight.Bold,
                        color = KenyanGreen,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LegalRow(icon = Icons.Default.Email, text = "support@mtaani.app")
                    LegalRow(icon = Icons.Default.Lock, text = "Privacy Policy")
                    LegalRow(icon = Icons.Default.Info, text = "Terms of Service")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "© 2026 Mtaani. All rights reserved.",
                fontSize = 11.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
            Text(
                "Made with ❤️ in Nairobi, Kenya",
                fontSize = 11.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AboutCard(title: String, content: String, icon: ImageVector) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFFE8F5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = KenyanGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = KenyanGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    content,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun InfoChip(label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFE8F5E9)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = KenyanGreen,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LegalRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = LightGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text,
            fontSize = 13.sp,
            color = Color.DarkGray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    AboutScreen(rememberNavController())
}