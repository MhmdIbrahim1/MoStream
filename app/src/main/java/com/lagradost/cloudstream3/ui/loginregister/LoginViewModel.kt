package com.lagradost.cloudstream3.ui.loginregister

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lagradost.cloudstream3.utils.NetworkResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val auth: FirebaseAuth
): ViewModel() {

    private val _login = MutableSharedFlow<NetworkResult<String>>()
    val login = _login.asSharedFlow()

    private val _resetPassword = MutableSharedFlow<NetworkResult<Boolean>>()
    val resetPassword = _resetPassword.asSharedFlow()

    private val _navigateState = MutableStateFlow(0)
    val navigateState = _navigateState.asStateFlow()

    private val userStatus = MutableStateFlow("")
    val userStatusFlow = userStatus.asStateFlow()
    companion object {
        const val MAIN_ACTIVITY = 23
    }

    fun login(email: String, password: String) {
        // Check for empty fields
        if (email.isEmpty() || password.isEmpty()) {
            viewModelScope.launch {
                _login.emit(NetworkResult.Error("Email or password cannot be empty"))
            }
            return
        }

        viewModelScope.launch {
            _login.emit(NetworkResult.Loading())
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                handleLoginSuccess()
            }
            .addOnFailureListener {
                handleLoginFailure()
            }
    }


    fun resetPassword(email: String) {
        viewModelScope.launch {
            _resetPassword.emit(NetworkResult.Loading())
        }

        // Send password reset email
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                viewModelScope.launch {
                    _resetPassword.emit(NetworkResult.Success(true))
                }
            }
            .addOnFailureListener {
                viewModelScope.launch {
                    _resetPassword.emit(NetworkResult.Error(it.message.toString()))
                }
            }
    }


    private fun handleLoginSuccess() {
        if (auth.currentUser?.isEmailVerified == true) {
            viewModelScope.launch {
                _login.emit(NetworkResult.Success("Login Success"))
            }
        } else {
            viewModelScope.launch {
                _login.emit(NetworkResult.Error("Please verify your email to login"))
            }
        }
    }


    private fun handleLoginFailure() {
        viewModelScope.launch {
            // if there is an error, show snackbar
            _login.emit(NetworkResult.Error("Login Failed: Incorrect email or password"))
        }
    }

    fun resendVerificationEmail() {
        val user = auth.currentUser
        user?.let {
            it.sendEmailVerification()
                .addOnSuccessListener {
                    viewModelScope.launch {
                        _login.emit(NetworkResult.Success("Verification email sent"))
                    }
                }
                .addOnFailureListener { e ->
                    viewModelScope.launch {
                        _login.emit(NetworkResult.Error(e.message.toString()))
                    }
                }
        }
    }


}