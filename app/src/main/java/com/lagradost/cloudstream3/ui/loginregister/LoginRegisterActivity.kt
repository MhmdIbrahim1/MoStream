package com.lagradost.cloudstream3.ui.loginregister


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.firebase.FirebaseApp
import com.lagradost.cloudstream3.databinding.ActivityLoginRegiesterBinding


class LoginRegisterActivity : AppCompatActivity() {
    private val binding by lazy { ActivityLoginRegiesterBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize FirebaseApp
        FirebaseApp.initializeApp(this)

        setContentView(binding.root)


    }
}