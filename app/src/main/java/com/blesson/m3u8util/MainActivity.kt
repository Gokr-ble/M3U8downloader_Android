package com.blesson.m3u8util

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.ListFragment
import androidx.lifecycle.ViewModelProvider
import com.blesson.m3u8util.model.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val listFragment = VideoListFragment()

    private var isFileLocked = true

    private lateinit var currentFragment: Fragment

    private lateinit var mainViewModel: MainViewModel

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

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val btnLockFile: ImageView = findViewById(R.id.btn_lock_file)
        btnLockFile.setOnClickListener {
            if (isFileLocked) {
                isFileLocked = false
                mainViewModel.setFileLocked(false)
                btnLockFile.setImageResource(R.drawable.ic_unlocked)
            } else {
                isFileLocked = true
                mainViewModel.setFileLocked(true)
                btnLockFile.setImageResource(R.drawable.ic_locked)
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
            currentFragment = fragment
        }
    }

}