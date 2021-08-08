package com.bull.bullBusiness

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.HandlerCompat.postDelayed
import com.bull.bullBusiness.databinding.ActivitySplashScreenBinding
import com.bull.bullBusiness.fragments.signUpLogInActivity.SignUpAndSignInFragment
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.security.MessageDigest
import kotlin.math.sign

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        supportActionBar?.hide()

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        FirebaseApp.initializeApp(this)

        val auth = FirebaseAuth.getInstance()
        Log.i(TAG,"current user email : ${auth.currentUser?.uid}")

        postDelayed(
            Handler(Looper.getMainLooper()),
            {
                if (auth.currentUser?.uid != null){
                    launchMainActivity()
                } else {
                    launchSignUpLogInActivity()
                }
            },
            null,
            2500)
    }

    override fun onStart() {
        super.onStart()
        val d = binding.splashScreenImage.drawable as AnimatedVectorDrawable
        d.start()
    }

    private fun launchSignUpLogInActivity(){
        val intent = Intent(this, SignUpLogInActivity::class.java)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, binding.splashScreenImage,"appLogo")
        startActivity(intent, options.toBundle())
    }

    private fun launchMainActivity(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "SplashScreenActivity"
    }
}