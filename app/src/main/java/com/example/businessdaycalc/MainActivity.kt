package com.example.businessdaycalc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val requestCalendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment != null) {
                supportFragmentManager.beginTransaction()
                    .detach(currentFragment)
                    .attach(currentFragment)
                    .commit()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestCalendarPermission()

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_calc -> {
                    loadFragment(CalculatorFragment())
                    true
                }
                R.id.nav_holiday -> {
                    loadFragment(HolidayFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }

        // Handle intent from Widget
        val openTab = intent.getStringExtra("OPEN_TAB")
        if (openTab == "HOLIDAY") {
            bottomNavigation.selectedItemId = R.id.nav_holiday
        } else if (openTab == "CALC") {
            bottomNavigation.selectedItemId = R.id.nav_calc
        } else {
            // Default tab
            if (savedInstanceState == null) {
                bottomNavigation.selectedItemId = R.id.nav_calc
            }
        }
    }

    private fun checkAndRequestCalendarPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            requestCalendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
