package com.benjamin.mtaani.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.benjamin.mtaani.R
import com.benjamin.mtaani.data.AuthViewModel
import com.benjamin.mtaani.navigation.ROUT_LOGIN
import com.benjamin.mtaani.ui.theme.KenyanGreen
import com.benjamin.mtaani.ui.theme.KenyanGreen

@Composable
fun RegisterScreen(navController: NavController){
    val context = LocalContext.current
    val authViewModel = remember { AuthViewModel(navController, context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .paint(painter = painterResource(R.drawable.background), contentScale = ContentScale.FillBounds),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){

        Image(
            painter = painterResource(R.drawable.kenya),
            contentDescription = "img",
            modifier = Modifier
                .size(200.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Join us and start your journey today!!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(10.dp))

        //Variables
        var username by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmpassword by remember { mutableStateOf("") }

        //Username
        OutlinedTextField(
            value = username,
            onValueChange = {username = it},
            modifier = Modifier.width(350.dp),
            leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = "") },
            placeholder = {Text("Enter Username")},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                unfocusedBorderColor = KenyanGreen,
                focusedBorderColor = Color.Black ,
                unfocusedLeadingIconColor = KenyanGreen,
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        //Email
        OutlinedTextField(
            value = email,
            onValueChange = {email = it},
            modifier = Modifier.width(350.dp),
            leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = "") },
            placeholder = {Text("Enter E-mail Address")},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                unfocusedBorderColor = KenyanGreen,
                focusedBorderColor = Color.Black,
                unfocusedLeadingIconColor = KenyanGreen,
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        //Password
        OutlinedTextField(
            value = password,
            onValueChange = {password= it},
            modifier = Modifier.width(350.dp),
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "") },
            placeholder = {Text("Password")},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                unfocusedBorderColor = KenyanGreen,
                focusedBorderColor = Color.Black,
                unfocusedLeadingIconColor = KenyanGreen,
            ),
            visualTransformation = PasswordVisualTransformation(),
        )

        Spacer(modifier = Modifier.height(10.dp))

        //Confirm Password
        OutlinedTextField(
            value = confirmpassword,
            onValueChange = {confirmpassword = it},
            modifier = Modifier.width(350.dp),
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "") },
            placeholder = {Text("Confirm Password")},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                unfocusedBorderColor = KenyanGreen,
                focusedBorderColor = Color.Black,
                unfocusedLeadingIconColor = KenyanGreen,
            ),
            visualTransformation = PasswordVisualTransformation(),
        )

        Spacer(modifier = Modifier.height(10.dp))

        //Button
        Button(
            onClick = {
                authViewModel.signup(username, email, password, confirmpassword)
            },
            colors = ButtonDefaults.buttonColors(KenyanGreen),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                text = "Register",
                color =White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick ={navController.navigate(ROUT_LOGIN) }) {
            Text(
                text = "Already have an Account? Log-in",
                fontSize = 15.sp,
                color =Black
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview(){
    RegisterScreen(rememberNavController())
}