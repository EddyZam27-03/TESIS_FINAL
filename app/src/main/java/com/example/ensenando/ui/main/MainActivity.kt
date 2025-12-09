package com.example.ensenando.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.ensenando.R
import com.example.ensenando.databinding.ActivityMainBinding
import com.example.ensenando.util.SecurityUtils
import com.example.ensenando.work.SyncManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        binding.bottomNavigation.setupWithNavController(navController)
        
        // Si es administrador, agregar opción de administración
        val rol = SecurityUtils.getUserRol(this)
        if (rol == "administrador") {
            val menu = binding.bottomNavigation.menu
            menu.add(0, R.id.adminFragment, 1, "Administración")
                .setIcon(android.R.drawable.ic_menu_manage)
        }
        
        // Iniciar sincronización periódica
        SyncManager.startPeriodicSync(this)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        SyncManager.stopPeriodicSync(this)
    }
}

