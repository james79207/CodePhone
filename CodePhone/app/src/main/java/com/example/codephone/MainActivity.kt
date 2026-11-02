package com.example.codephone

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.*
import android.widget.*

class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("cfg", MODE_PRIVATE) }
    private var web: WebView? = null
    private var chooser: ValueCallback<Array<Uri>>? = null
    private val reqFile = 1
    private val desktopUA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val url = prefs.getString("url", null)
        if (url.isNullOrBlank()) showSetup() else showWeb(url)
    }

    private fun showSetup() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = "CodePhone\nMasukkan alamat VS Code web Anda"
            textSize = 18f
        })
        val input = EditText(this).apply {
            setText(prefs.getString("url", "https://vscode.dev"))
            hint = "https://..."
            setSingleLine()
        }
        root.addView(input, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(TextView(this).apply {
            text = "Contoh:\n• https://vscode.dev\n• https://github.dev/USER/REPO\n• https://alamat-code-server-anda"
            setPadding(0, 24, 0, 24)
        })
        root.addView(Button(this).apply {
            text = "Simpan & Buka"
            setOnClickListener {
                var u = input.text.toString().trim()
                if (u.isNotEmpty()) {
                    if (!u.startsWith("http")) u = "https://$u"
                    prefs.edit().putString("url", u).apply()
                    showWeb(u)
                }
            }
        })
        setContentView(root)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showWeb(url: String) {
        val frame = FrameLayout(this)
        val w = WebView(this)
        web = w
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true)
        w.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }
        applyDesktopMode(w)
        w.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean = false
        }
        w.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                v: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams
            ): Boolean {
                chooser?.onReceiveValue(null)
                chooser = cb
                return try {
                    startActivityForResult(p.createIntent(), reqFile); true
                } catch (e: Exception) { chooser = null; false }
            }
        }
        w.loadUrl(url)
        frame.addView(w, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val gear = TextView(this).apply {
            text = "⚙"
            textSize = 20f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(140, 0, 0, 0))
            gravity = Gravity.CENTER
            setOnClickListener { showMenu() }
        }
        frame.addView(gear, FrameLayout.LayoutParams(110, 110, Gravity.BOTTOM or Gravity.END).apply {
            setMargins(0, 0, 24, 160)
        })
        setContentView(frame)
    }

    private fun applyDesktopMode(w: WebView) {
        val desktop = prefs.getBoolean("desktop", false)
        w.settings.userAgentString = if (desktop) desktopUA else null
        w.settings.useWideViewPort = desktop
        w.settings.loadWithOverviewMode = desktop
    }

    private fun showMenu() {
        val desktop = prefs.getBoolean("desktop", false)
        val items = arrayOf(
            "Reload",
            "Ubah alamat",
            if (desktop) "Matikan mode desktop" else "Aktifkan mode desktop"
        )
        AlertDialog.Builder(this).setItems(items) { _, i ->
            when (i) {
                0 -> web?.reload()
                1 -> { web?.destroy(); web = null; showSetup() }
                2 -> {
                    prefs.edit().putBoolean("desktop", !desktop).apply()
                    web?.let { applyDesktopMode(it); it.reload() }
                }
            }
        }.show()
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        if (req == reqFile) {
            chooser?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data))
            chooser = null
        } else super.onActivityResult(req, res, data)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val w = web
        if (w != null && w.canGoBack()) w.goBack() else super.onBackPressed()
    }
}
