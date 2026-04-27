package com.benjamin.mtaani.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.benjamin.mtaani.R
import com.benjamin.mtaani.navigation.ROUT_HOME
import com.benjamin.mtaani.navigation.ROUT_REGISTER
import com.benjamin.mtaani.ui.screens.home.HomeScreen
import com.benjamin.mtaani.ui.theme.KenyanGreen
import com.benjamin.mtaani.ui.theme.OLdNavy

@Composable
fun OnboardingScreen(navController: NavController){
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){

        Image(
            painter = painterResource(R.drawable.mtaani_logo),
            contentDescription = "img",
            modifier = Modifier.size(300.dp)







        )

        Spacer(modifier = Modifier.height(20.dp) )

        Text(
            text = "Welcome to Mtaani!!",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color =Black
        )
        Spacer(modifier = Modifier.height(20.dp) )
        
        Text(
            text = "Be a better citizen,",
            fontSize = 20.sp,

            )

        Spacer(modifier = Modifier.height(20.dp) )

        Text(
            text = "Mtaani is an Android-based civic issue reporting application designed to empower Kenyan citizens to actively participate in improving their local communities.\n,",
            fontSize = 16.sp,
            textAlign = TextAlign.Justify
        )
        Button(
            onClick = {navController.navigate(ROUT_REGISTER)},
            colors = ButtonDefaults.buttonColors(KenyanGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.width(350.dp)

        ) {
            Text(
                text = "Get Started",
                color =Black

            )
        }















    }




}
@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview(){
    OnboardingScreen(rememberNavController())
}














































@Preview(showBackground = true)
@Composable
fun OnBoardingPreview(){
    OnboardingScreen(rememberNavController())



}