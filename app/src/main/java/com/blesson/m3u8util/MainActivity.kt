package com.blesson.m3u8util

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.ListFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val listFragment = VideoListFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initComponent()
    }

    private fun initComponent() {
        val navigationView: BottomNavigationView = findViewById(R.id.bottom_nav)
        navigationView.setOnNavigationItemSelectedListener {
            if (it.itemId == R.id.navigation_home) {
                changeFragment(homeFragment)
            } else if (it.itemId == R.id.navigation_list) {
                changeFragment(listFragment)
            }
            true
        }
        navigationView.setOnNavigationItemReselectedListener {
            if (it.itemId == R.id.navigation_home) {
                changeFragment(homeFragment)
            } else if (it.itemId == R.id.navigation_list) {
                changeFragment(listFragment)
            }
        }
        changeFragment(homeFragment)
    }

    private fun changeFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction().apply {
            replace(R.id.fragment, fragment)
            commit()
        }
    }

}