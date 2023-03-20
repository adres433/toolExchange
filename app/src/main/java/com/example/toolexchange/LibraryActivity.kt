package com.example.toolexchange

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.toolexchange.databinding.ActivityLibraryBinding

class LibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLibraryBinding

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = ActivityLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}