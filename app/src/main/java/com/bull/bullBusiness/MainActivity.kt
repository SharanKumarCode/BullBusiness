package com.bull.bullBusiness

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.bull.bullBusiness.databinding.ActivityMainBinding
import com.bullSaloon.bull.viewModel.MainActivityViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var dataViewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        supportActionBar?.hide()

        createNotificationChannel()

        dataViewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

//        setting bottom navigation menu

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment)
        val navController = navHostFragment?.findNavController()

        if (navController != null) {
            binding.bottomNavigationView.setupWithNavController(navController)
        }
    }

    override fun onBackPressed() {

        val destinationID = supportFragmentManager.findFragmentById(R.id.fragment)?.childFragmentManager?.fragments?.get(0)?.childFragmentManager?.findFragmentById(R.id.fragmentSaloonContainerView)?.findNavController()?.currentDestination?.id

        if (destinationID == R.id.cameraFragment){

            val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment)?.childFragmentManager?.fragments?.get(0)?.childFragmentManager?.findFragmentById(R.id.fragmentSaloonContainerView)
            val navController = navHostFragment?.findNavController()
            navController?.navigate(R.id.saloonItemFragment)

        } else {
            super.onBackPressed()
        }
    }

    fun setActionBarBottomBarVisibility(type : String){
        if (type == "hide"){
            binding.bottomAppBar.performHide()
            binding.topAppBar.visibility = View.GONE

            val param = binding.fragment.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0,0,0,0)
            binding.fragment.layoutParams = param

        } else if (type == "show") {

            binding.bottomAppBar.performShow()
            binding.topAppBar.visibility = View.VISIBLE

            val param = binding.fragment.layoutParams as ViewGroup.MarginLayoutParams
            val tv = TypedValue()
            if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                val actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
                param.setMargins(0,actionBarHeight,0,actionBarHeight)
                binding.fragment.layoutParams = param
            }
        }
    }

    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name = "Bull App"
            val descriptionText = "Notification channel for Bull Saloon App"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name , importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "TAGMainActivity"
        private const val NOTIFICATION_CHANNEL_ID = "100"
    }
}