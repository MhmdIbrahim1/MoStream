package com.lagradost.cloudstream3

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lagradost.cloudstream3.databinding.ActivityStartedScreenBinding
import com.lagradost.cloudstream3.ui.account.AccountSelectActivity
import com.lagradost.cloudstream3.ui.loginregister.RegisterViewModel
import com.lagradost.cloudstream3.ui.loginregister.RegisterViewModel.Companion.USER_COLLECTION


class StartedScreen : AppCompatActivity() {
    private val binding by lazy { ActivityStartedScreenBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize FirebaseApp
        FirebaseApp.initializeApp(this)

        setContentView(binding.root)
        getUserStatusFromDatabase()



        val lottieAnimationView = binding.logo
        lottieAnimationView.setAnimation(R.raw.logo)
        lottieAnimationView.repeatCount = LottieDrawable.INFINITE
        lottieAnimationView.playAnimation()

        Handler(Looper.myLooper()!!).postDelayed({
            startActivity(Intent(this@StartedScreen, LoginRegisterActivity::class.java))
            finish()
        }, 3000)

    }
    private fun getUserStatusFromDatabase() {
        val auth = FirebaseAuth.getInstance()
        val firestore = Firebase.firestore

        // Check if the user is authenticated
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Fetch user status from Firestore
            firestore.collection(USER_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val userStatus = documentSnapshot.getString("userStatus")
                        if (userStatus == "banned") {
                            logoutCurrentUser()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Error fetching user status: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
        } else {
            // User is not authenticated
            // Do nothing
        }
    }

    private fun logoutCurrentUser() {
        val auth = FirebaseAuth.getInstance()
        auth.signOut()

        // create AlertDialog
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Your Subscriptions have expired. renew your subscription to continue using the app")
            .setCancelable(false)
            .setPositiveButton("Ok") { _, _ ->
                startActivity(Intent(this@StartedScreen, LoginRegisterActivity::class.java))
                finish()
            }
        val alert = dialogBuilder.create()
        alert.setTitle("Session Expired")
        alert.show()
    }

}
