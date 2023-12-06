package com.lagradost.cloudstream3

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.lagradost.cloudstream3.databinding.ActivityLoginRegiesterBinding
import com.lagradost.cloudstream3.ui.account.AccountSelectActivity
import com.lagradost.cloudstream3.utils.NetworkResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginRegisterActivity : AppCompatActivity() {
    private val binding by lazy { ActivityLoginRegiesterBinding.inflate(layoutInflater) }

    // Manually provide dependencies here
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Create LoginViewModel manually with provided dependencies
    private val viewModel = LoginViewModel(firebaseAuth)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize FirebaseApp
        FirebaseApp.initializeApp(this)

        setContentView(binding.root)

        login()
        observeLogin()



    }

    // funtion to loign user with email and password in firebase authentication
    // the email is tes1@gmail.com and password 123456
    private fun login() {
        binding.apply {
            loginBtn.setOnClickListener {
                val email = edEmail.text.toString().trim()
                val password = edPassword.text.toString()
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    if (email.equals(
                            "test1@gmail.com",
                            ignoreCase = true
                        ) && password == "123456"
                    ) {
                        viewModel.login(email, password)
                    } else {
                        Snackbar.make(
                            it,
                            "Please enter correct email and password",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Snackbar.make(
                        it,
                        "Please enter email and password",
                        Snackbar.LENGTH_SHORT
                    ).show()

                }
            }
        }
    }

    private fun observeLogin() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.login.collectLatest {
                    when (it) {
                        is NetworkResult.Loading -> {
                            binding.progressBar2.visibility = View.VISIBLE
                            binding.loginBtn.visibility = View.GONE
                        }

                        is NetworkResult.Success -> {
                            binding.progressBar2.visibility = View.GONE
                            binding.loginBtn.visibility = View.VISIBLE
                            Intent(
                                this@LoginRegisterActivity,
                                AccountSelectActivity::class.java
                            ).also { intent ->
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }
                        }

                        is NetworkResult.Error -> {
                            binding.progressBar2.visibility = View.GONE
                            binding.loginBtn.visibility = View.VISIBLE

                        }
                        else -> Unit
                    }
                }
            }
        }


    }
}