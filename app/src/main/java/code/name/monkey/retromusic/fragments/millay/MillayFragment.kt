/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.fragments.millay

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Fragmento principal de Millay (réplica 1:1 ReFreezer).
 * Contiene un ViewPager2 con 3 pestañas Deezer + un 4º botón inferior para regresar al reproductor local Milla:
 *  1. 🏠 Home — Flow Bubbles, Continue Streaming, Discover, Top Charts
 *  2. 🔍 Search — Búsqueda Deezer HQ
 *  3. 📚 Library — Gestor de Descargas y Favoritos
 *  4. 📻 Milla Player — Regresa al reproductor nativo local
 */
class MillayFragment : AbsMainActivityFragment(R.layout.fragment_millay) {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.viewPager)
        bottomNav = view.findViewById(R.id.bottomNavigation)

        setupViewPager()
        setupBottomNav()
    }

    private fun setupViewPager() {
        viewPager.adapter = MillayPagerAdapter(this)
        viewPager.offscreenPageLimit = 2
        viewPager.isUserInputEnabled = true // Permite deslizar entre pantallas ReFreezer

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> bottomNav.menu.findItem(R.id.millay_nav_home)?.isChecked = true
                    1 -> bottomNav.menu.findItem(R.id.millay_nav_search)?.isChecked = true
                    2 -> bottomNav.menu.findItem(R.id.millay_nav_library)?.isChecked = true
                }
            }
        })
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.millay_nav_home -> {
                    viewPager.setCurrentItem(0, true)
                    true
                }
                R.id.millay_nav_search -> {
                    viewPager.setCurrentItem(1, true)
                    true
                }
                R.id.millay_nav_library -> {
                    viewPager.setCurrentItem(2, true)
                    true
                }
                R.id.millay_nav_milla_player -> {
                    // Volver al reproductor/librería nativa de Milla
                    try {
                        findNavController().navigate(R.id.action_song)
                    } catch (e: Exception) {
                        try {
                            findNavController().navigateUp()
                        } catch (ex: Exception) {
                            // Ignore fallback
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

    override fun onMenuItemSelected(item: MenuItem): Boolean = false

    // ---------------------------------------------------------------------------
    // Adaptador del ViewPager2 interno de ReFreezer
    // ---------------------------------------------------------------------------
    private inner class MillayPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> MillayHomeFragment()
            1 -> MillaySearchFragment()
            2 -> MillayDownloadsFragment()
            else -> MillayHomeFragment()
        }
    }
}
