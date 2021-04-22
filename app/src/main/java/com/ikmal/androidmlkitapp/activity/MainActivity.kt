package com.ikmal.androidmlkitapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ikmal.androidmlkitapp.R
import com.ikmal.androidmlkitapp.fragment.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }
}