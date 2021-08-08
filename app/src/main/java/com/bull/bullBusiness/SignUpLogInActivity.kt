package com.bull.bullBusiness

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.bull.bullBusiness.databinding.ActivitySignUpLogInBinding
import com.bull.bullBusiness.fragments.signUpLogInActivity.CreateAccountFragment
import com.bull.bullBusiness.fragments.signUpLogInActivity.SignUpAndSignInFragment

class SignUpLogInActivity : AppCompatActivity() {private lateinit var binding: ActivitySignUpLogInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        val signUpAndSignInFragment = SignUpAndSignInFragment()
        supportFragmentManager.beginTransaction().replace(binding.SignInFragmentContainer.id, signUpAndSignInFragment).commit()

    }
}