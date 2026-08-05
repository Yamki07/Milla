/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.fragments.millay

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.automix.MillayWebBridge
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment

/**
 * Fragmento principal de Millay que hospeda la interfaz gráfica Monochrome Web UI (HTML/CSS/JS)
 * conectada con el motor DJ nativo en Kotlin a través de WebView.
 */
class MillayFragment : AbsMainActivityFragment(R.layout.fragment_millay) {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.millayWebView)
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Inyectar la interfaz de comunicación bidireccional JS-Kotlin
        webView.addJavascriptInterface(MillayWebBridge(requireContext()), "MillaNative")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }

        // Cargar los activos empaquetados de Monochrome UI
        webView.loadUrl("file:///android_asset/millay/index.html")
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

    override fun onMenuItemSelected(item: MenuItem): Boolean = false
}
