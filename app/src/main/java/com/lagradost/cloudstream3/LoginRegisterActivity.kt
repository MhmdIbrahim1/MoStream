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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize FirebaseApp
        FirebaseApp.initializeApp(this)

        setContentView(binding.root)

    }



}