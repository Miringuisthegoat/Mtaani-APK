package com.benjamin.mtaani.data

import android.content.Context
import android.widget.Toast
import androidx.navigation.NavController
import com.benjamin.mtaani.models.User
import com.benjamin.mtaani.navigation.ROUT_HOME
import com.benjamin.mtaani.navigation.ROUT_LOGIN
import com.benjamin.mtaani.navigation.ROUT_REGISTER
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AuthViewModel(var navController: NavController, var context: Context) {
    private val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    fun signup(username: String, email: String, password: String, confirmpassword: String) {
        if (email.isBlank() || password.isBlank() || confirmpassword.isBlank()) {
            Toast.makeText(context, "Email and password cannot be blank", Toast.LENGTH_LONG).show()
        } else if (password != confirmpassword) {
            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_LONG).show()
        } else {
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                if (it.isSuccessful) {
                    val uid = mAuth.currentUser!!.uid

                    val userdata = User(
                        username = username,
                        email = email,
                        password = password,
                        uid = uid,
                        role = "user"
                    )

                    val regRef = FirebaseDatabase.getInstance().getReference("Users/$uid")
                    regRef.setValue(userdata).addOnCompleteListener { result ->
                        if (result.isSuccessful) {
                            Toast.makeText(context, "Registered Successfully! Welcome to Mtaani 🇰🇪", Toast.LENGTH_LONG).show()
                            navController.navigate(ROUT_HOME)
                        } else {
                            Toast.makeText(context, "${result.exception!!.message}", Toast.LENGTH_LONG).show()
                            navController.navigate(ROUT_REGISTER)
                        }
                    }
                } else {
                    Toast.makeText(context, "${it.exception!!.message}", Toast.LENGTH_LONG).show()
                    navController.navigate(ROUT_REGISTER)
                }
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Email and password cannot be blank", Toast.LENGTH_LONG).show()
        } else {
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Welcome back! 👋", Toast.LENGTH_SHORT).show()
                    navController.navigate(ROUT_HOME)
                } else {
                    Toast.makeText(context, "${task.exception!!.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun logout() {
        mAuth.signOut()
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        navController.navigate(ROUT_LOGIN) {
            popUpTo(ROUT_HOME) { inclusive = true }
        }
    }

    fun isLoggedIn(): Boolean = mAuth.currentUser != null

    fun getCurrentUserEmail(): String = mAuth.currentUser?.email ?: ""

    fun getCurrentUserUid(): String = mAuth.currentUser?.uid ?: ""
}