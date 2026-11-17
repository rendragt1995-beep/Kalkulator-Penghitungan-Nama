package com.penyandingan.nama.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.penyandingan.nama.domain.HasilPenyandinganLengkap
import com.penyandingan.nama.domain.KalkulatorPenyandingan

object PrintHelper {
    private var activeWebView: WebView? = null

    fun cetakHasil(context: Context, hasil: HasilPenyandinganLengkap) {
        val html = KalkulatorPenyandingan.formatLaporanHtml(hasil)
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean = true

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (view == null) return
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = "Penyandingan - ${hasil.nama1.namaAsli} & ${hasil.nama2.namaAsli}"
                    val adapter = view.createPrintDocumentAdapter(jobName)
                    printManager.print(
                        jobName,
                        adapter,
                        PrintAttributes.Builder().build()
                    )
                    activeWebView = null
                }
            }
        }
        activeWebView = webView
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
