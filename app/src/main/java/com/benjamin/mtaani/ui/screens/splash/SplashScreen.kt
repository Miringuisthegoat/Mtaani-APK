package com.benjamin.mtaani.ui.screens.splash

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import com.benjamin.mtaani.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp



@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun SplashScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    coroutineScope.launch {
        delay(6000)
        //navController.navigate(ROUT_LOGIN)
    }
    Column(
        Modifier
            .background(color = Color(0xFFFFFFFF)) // Kenyan green
            .fillMaxSize(),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(R.drawable.glass),
            contentDescription = "img",
            modifier = Modifier.size(300.dp))


        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Mtaani",
            color = Color.Black,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sauti ya Wananchi",
            color = Color.Black.copy(alpha = 0.8f),
            fontSize = 16.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(rememberNavController())
}