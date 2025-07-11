package com.example.waveoffoodadmin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.waveoffoodadmin.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {
    private var binding: ActivityStartBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding!!.root)


        binding!!.nextButton.setOnClickListener { v ->
            val isLogin = getSharedPreferences("LOGIN_REF", MODE_PRIVATE)
                .getBoolean("LOGIN_REF", false)

            val intent = if (isLogin) {
                Intent(this@StartActivity, MainActivity::class.java)
            } else {
                Intent(this@StartActivity, LoginActivity::class.java)
            }
            startActivity(intent)
            finish()
        }
    }
}
