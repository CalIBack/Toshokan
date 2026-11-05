package com.example.animeapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

// Toolbar + pantallas + barra inferior
class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var bottomInset = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val root = findViewById<android.view.View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            bottomInset = systemBars.bottom
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, rootBottomPadding())
            insets
        }

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNav)
        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.searchFragment, R.id.topFragment, R.id.favoritesFragment)
        )
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(bottomNav, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            toolbar.isVisible = destination.id != R.id.homeFragment
            bottomNav.isVisible = destination.id != R.id.animeDetailFragment
            root.setPadding(root.paddingLeft, root.paddingTop, root.paddingRight, rootBottomPadding(destination.id))
        }
    }

    // Margen inferior solo en el detalle (la barra ya se ajusta sola)
    private fun rootBottomPadding(
        destinationId: Int? = if (::navController.isInitialized) navController.currentDestination?.id else null
    ): Int = if (destinationId == R.id.animeDetailFragment) bottomInset else 0

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}
