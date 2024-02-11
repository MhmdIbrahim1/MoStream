package com.lagradost.cloudstream3.ui.loginregister

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lagradost.cloudstream3.utils.NetworkResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RegisterViewModel(
    private val firebaseAuth: FirebaseAuth,
    private val db: FirebaseFirestore
): ViewModel() {

    private val _register =
        MutableStateFlow<NetworkResult<UserSign>>(NetworkResult.UnSpecified())
    val register: Flow<NetworkResult<UserSign>> = _register

    private val _validation = Channel<RegisterFailedState>()
    val validation = _validation.receiveAsFlow()


    fun createAccountWithEmailAndPassword(user: UserSign, password: String) {
        if (checkValidation(user, password)) {
            runBlocking {
                _register.emit(NetworkResult.Loading())
            }
            user.email?.let {
                firebaseAuth.createUserWithEmailAndPassword(it, password)
                    .addOnSuccessListener {firebaseUser ->
                        firebaseUser.user?.let {
                            saveUserInfo(it.uid,user)
                        }
                    }
                    .addOnFailureListener {
                        _register.value = NetworkResult.Error(it.message.toString())
                    }
            }
        }else{
            val registerValidation = user.email?.let { validateEmail(it) }?.let {
                RegisterFailedState(
                    it,
                    validatePassword(password)
                )
            }
            viewModelScope.launch {
                if (registerValidation != null) {
                    _validation.send(registerValidation)
                }
            }
        }
    }

    private fun checkValidation(user: UserSign, password: String): Boolean {
        val emailValidation = user.email?.let { validateEmail(it) }
        val passwordValidation = validatePassword(password)
        return emailValidation is RegisterValidation.Success && passwordValidation is RegisterValidation.Success
    }

    private fun saveUserInfo(userUid: String,user: UserSign) {
        db.collection(USER_COLLECTION)
            .document(userUid)
            .set(user)
            .addOnSuccessListener {
                _register.value = NetworkResult.Success(user)
                sendConfirmationEmail()
            }
            .addOnFailureListener {
                _register.value = NetworkResult.Error(it.message.toString())
            }
    }

    private fun sendConfirmationEmail() {
        val user = firebaseAuth.currentUser
        user?.let {
            it.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        viewModelScope.launch {
                            _register.emit(NetworkResult.Success(UserSign()))
                        }
                    } else {
                        viewModelScope.launch {
                            _register.emit(NetworkResult.Error(task.exception?.message.toString()))
                        }
                    }
                }
        }
    }

    companion object {
        const val USER_COLLECTION = "users"
    }

}