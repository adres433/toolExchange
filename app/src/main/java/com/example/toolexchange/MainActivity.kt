package com.example.toolexchange

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.Window
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity()
{
    private var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

    }

    override fun onResume() {
        super.onResume()
        checkPermission(Intent(),true)
        if(intent.hasExtra("logToken"))
            token = intent.getStringExtra("logToken").toString()
    }


    fun onClickBtn(v: View) //obsługa przycisków - onClick
    {
        val id = v.resources.getResourceName(v.id)

        when {
            id.lastIndexOf("add") != -1 -> {
                val act = Intent(applicationContext, AddActivity::class.java)
                if(token.isNotEmpty())
                    act.putExtra("logToken", token)
                checkPermission(act)
            }
            id.lastIndexOf("del") != -1 -> {
                val act = Intent(applicationContext, DelActivity::class.java)
                if(token.isNotEmpty())
                    act.putExtra("logToken", token)
                checkPermission(act)
            }
            id.lastIndexOf("check") != -1 -> {
                val act = Intent(applicationContext, CheckActivity::class.java)
                if(token.isNotEmpty())
                    act.putExtra("logToken", token)
                checkPermission(act)
            }
            id.lastIndexOf("infoLibrary") != -1 -> {
                val act = Intent(applicationContext, LibraryActivity::class.java)
                if(token.isNotEmpty())
                    act.putExtra("logToken", token)
                startActivity(act)
            }
            else -> Toast.makeText(applicationContext, "Wystąpił błąd.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermission(who: Intent, check: Boolean = false)   //sprawdzenie uprawnień aplikacji
    {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
        {
            //uprawnienia przyznane
            if(!check)
                startActivity(who)
        }
        else
        {
            //brak uprawnień
            Snackbar.make(
                findViewById(R.id.mainLayout),
                getString(R.string.appPer),
                Snackbar.LENGTH_LONG
            ).setAction(getString(R.string.changePer))
            {
                val uriA = Uri.fromParts("package", packageName, null)
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uriA))
            }.show()
        }
    }
}