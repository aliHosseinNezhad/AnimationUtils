package com.gamapp.animateutils

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import com.gamapp.animateutils.databinding.ActivityMainBinding
import com.gamapp.animationutils.AnimateUtils

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    var bool = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(viewBinding.root)
        viewBinding.showButton.setOnClickListener {
            viewBinding.switchView.animationUtils.start(if (bool) AnimateUtils.Direction.STE else AnimateUtils.Direction.ETS)
            bool = !bool
        }
    }
}