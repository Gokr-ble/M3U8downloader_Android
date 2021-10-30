package com.blesson.m3u8util

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.ListFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val listFragment = VideoListFragment()

    private lateinit var currentFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initComponent()
    }

    private fun initComponent() {
        setDefaultFragment(homeFragment)

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
    }

    private fun setDefaultFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment, fragment)
            .commit()
        currentFragment = fragment
    }

    private fun changeFragment(fragment: Fragment) {
        // 以下方法Fragment会重载
//        val fragmentManager = supportFragmentManager
//        fragmentManager.beginTransaction().apply {
//            replace(R.id.fragment, fragment)
//            commit()
//        }
        // 以下方法Fragment不会重载
        if (currentFragment != fragment) {
            val transaction = supportFragmentManager.beginTransaction()
            if (!fragment.isAdded) {
                transaction.hide(currentFragment).add(R.id.fragment, fragment).commit()
            } else {
                transaction.hide(currentFragment).show(fragment).commit()
            }

//            if (fragment is HomeFragment) {
//                if (!fragment.isAdded) {
//                    transaction.hide(currentFragment).add(R.id.fragment, fragment).commit()
//                } else {
//                    transaction.hide(currentFragment).show(fragment).commit()
//                }
//            } else if (fragment is VideoListFragment) {
//                transaction.replace(R.id.fragment, fragment).commit()
//            }
            currentFragment = fragment
        }
    }

}