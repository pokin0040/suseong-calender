package com.example.businessdaycalc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        } else {
            // Default tab
            if (savedInstanceState == null) {
                bottomNavigation.selectedItemId = R.id.nav_calc
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
