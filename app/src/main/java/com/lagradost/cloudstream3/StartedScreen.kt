package com.lagradost.cloudstream3

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieDrawable
import com.google.firebase.FirebaseApp
import com.lagradost.cloudstream3.databinding.ActivityStartedScreenBinding
import com.lagradost.cloudstream3.ui.account.AccountSelectActivity


class StartedScreen : AppCompatActivity() {
    private val binding by lazy { ActivityStartedScreenBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val lottieAnimationView = binding.logo
        lottieAnimationView.setAnimation(R.raw.logo)
        lottieAnimationView.repeatCount = LottieDrawable.INFINITE
        lottieAnimationView.playAnimation()

        Handler(Looper.myLooper()!!).postDelayed({
            startActivity(Intent(this@StartedScreen, LoginRegisterActivity::class.java))
            finish()
        }, 3000)


    }

}