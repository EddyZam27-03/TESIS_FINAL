package com.example.ensenando.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ensenando.databinding.ActivityWelcomeBinding
import com.example.ensenando.ui.auth.AuthActivity
import com.example.ensenando.ui.main.MainActivity
import com.example.ensenando.util.SecurityUtils

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ✅ FIX: Verificar si hay sesión guardada
        if (SecurityUtils.isLoggedIn(this)) {
            // Si hay sesión, ir directamente a MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.btnIniciarSesion.setOnClickListener {
            val intent = Intent(this, AuthActivity::class.java).apply {
                putExtra("mode", "login")
            }
            startActivity(intent)
        }
        
        binding.btnRegistrarse.setOnClickListener {
            val intent = Intent(this, AuthActivity::class.java).apply {
                putExtra("mode", "register")
            }
            startActivity(intent)
        }
    }
}


