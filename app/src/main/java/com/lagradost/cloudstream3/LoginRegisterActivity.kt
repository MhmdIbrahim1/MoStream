package com.lagradost.cloudstream3

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.FirebaseApp

import com.lagradost.cloudstream3.CommonActivity.loadThemes
import com.lagradost.cloudstream3.databinding.ActivityLoginRegiesterBinding


class LoginRegisterActivity : AppCompatActivity() {
    private val binding by lazy { ActivityLoginRegiesterBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        loadThemes(this)

        super.onCreate(savedInstanceState)

        setContentView(binding.root)

    }


}