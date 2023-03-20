package com.example.toolexchange


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.toolexchange.databinding.ActivityLoginBinding
import kotlinx.coroutines.*


class LoginActivity : AppCompatActivity() {

    private lateinit var bind: ActivityLoginBinding
    private lateinit var job : Job
    private lateinit var job1 : Job
    private lateinit var mso365 : O365

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        //odniesienia do pól formularza ustawień
        bind = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(bind.root)

        job = CoroutineScope(Dispatchers.IO).launch {

            mso365 = O365(applicationContext, this@LoginActivity)
            job.cancelAndJoin()
        }
        job1 = CoroutineScope(Dispatchers.IO).launch {
            var i = 10
            while (i < 100)
            {
                delay(100)
                bind.progressBar.incrementProgressBy(10)
                i += 10
                if(i == 100)
                    i = 0
                if (mso365.succes || mso365.fail)
                    break
            }

            if (mso365.succes)
                exitLogin()
            else if (mso365.fail)
            {
                startActivity(Intent(applicationContext, IntroActivity::class.java))
                finish()
            }
            job1.cancelAndJoin()
        }
    }


    private fun exitLogin()
    {
        Log.d("_JOB", "JOB1: ${job1.isCancelled} OR ${job1.isCompleted}")
        Log.d("_JOB", "JOB: ${job.isCancelled} OR ${job.isCompleted}")
        startActivity(Intent(applicationContext, MainActivity::class.java).putExtra("logToken", mso365.getToken()))
        finish()
    }
}