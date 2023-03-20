package com.example.toolexchange

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.view.Window

class IntroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_intro)
    }

    fun closeIntro(v: View)
    {
        startActivity(Intent(v.context, LoginActivity::class.java))
        finish()
    }
}