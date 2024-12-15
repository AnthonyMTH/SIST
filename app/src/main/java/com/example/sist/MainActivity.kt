package com.example.sist

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.sist.fragments.InformationFragment
import com.example.sist.fragments.PanicButtonFragment
import com.example.sist.fragments.SettingsFragment


 class MainActivity : AppCompatActivity() {
     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         setContentView(R.layout.activity_main)

         // Configurar el toolbar
         val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
         setSupportActionBar(toolbar)

         // Cargar el fragment inicial (Botón de Pánico)
         if (savedInstanceState == null) {
             loadFragment(PanicButtonFragment())
         }
     }

     override fun onCreateOptionsMenu(menu: Menu?): Boolean {
         // Inflar el menú
         menuInflater.inflate(R.menu.menu_toolbar, menu)
         return true
     }

     override fun onOptionsItemSelected(item: MenuItem): Boolean {
         return when (item.itemId) {
             R.id.menu_information -> {
                 // Cargar el fragment de Manual
                 loadFragment(InformationFragment())
                 true
             }
             R.id.menu_settings -> {
                 // Cargar el fragment de Configuración
                 loadFragment(SettingsFragment())
                 true
             }
             else -> super.onOptionsItemSelected(item)
         }
     }

     // Método para cargar fragments dinámicamente
     private fun loadFragment(fragment: Fragment) {
         supportFragmentManager.beginTransaction()
             .replace(R.id.fragment_container, fragment)
             .addToBackStack(null)
             .commit()
     }
}