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
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Fragmento principal de Millay (ex Milla Internet).
 * Contiene un ViewPager2 con 3 pestañas:
 *  - 🏠 Inicio  — Flow Bubbles (Mood), Top Charts y Recomendados
 *  - 🔍 Buscar  — Búsqueda HQ con filtros de calidad (FLAC / MP3 320)
 *  - 📥 Descargas — Lista de descargas activas y completadas
 */
class MillayFragment : AbsMainActivityFragment(R.layout.fragment_millay) {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var toolbar: Toolbar

    private val tabTitles = listOf("🏠 Inicio", "🔍 Buscar", "📥 Descargas")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar   = view.findViewById(R.id.toolbar)
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)

        setupToolbar()
        setupViewPager()
    }

    private fun setupToolbar() {
        toolbar.title = "Millay"
        mainActivity.setSupportActionBar(toolbar)
    }

    private fun setupViewPager() {
        viewPager.adapter = MillayPagerAdapter(this)
        viewPager.offscreenPageLimit = 2 // Mantener las 3 tabs cargadas

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // Sin menú adicional en esta pantalla
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean = false

    // ---------------------------------------------------------------------------
    // Adaptador interno del ViewPager2
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
