package com.longho.novelextractor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovelExtractorScreen()
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        val webView = findWebViewInstance() // 🔹 Get WebView from Compose
        if (webView?.canGoBack() == true) {
            webView.goBack() // 🔹 Navigate back in WebView
        } else {
            super.onBackPressed() // 🔹 Exit app if no history
        }
    }

    private fun findWebViewInstance(): WebView? {
        return try {
            (window.decorView.rootView as? ViewGroup)?.let { rootView ->
                findWebViewRecursively(rootView)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun findWebViewRecursively(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val webView = findWebViewRecursively(view.getChildAt(i))
                if (webView != null) return webView
            }
        }
        return null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelExtractorScreen() {
    val context = LocalContext.current
    var currentUrl by remember { mutableStateOf("https://ncode.syosetu.com/") }
    val webView = rememberWebView(onUrlChange = { newUrl ->
        currentUrl = newUrl  // 🔹 Ensure state updates correctly
        Log.d("-----------", currentUrl)
    })

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Novel Extractor") },
                    navigationIcon = {
                        IconButton(
                            onClick = { if (webView.canGoBack()) webView.goBack() } // 🔹 Back Button
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { webView.reload() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload"
                            )
                        }
                    }
                )
                AddressBar(
                    url = currentUrl,
                    onGoClick = { webView.loadUrl(it) }
                )
            }
        },
        bottomBar = {
            if (isNovelSite(currentUrl)) {
                Button(
                    onClick = { extractAndCopyNovel(context, webView) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Copy Novel Text")
                }
            }
        }
    ) {padding ->
        Column(
            Modifier.padding(padding)

        ) {
            WebViewScreen(webView)
        }
    }
}

@Composable
fun AddressBar(url: String, onGoClick: (String) -> Unit) {
    var text by remember { mutableStateOf(url) }

    // 🔹 Ensure `text` updates when `url` changes externally
    LaunchedEffect(url) {
        text = url
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onGoClick(text) }),
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surface)
        )
        Button(
            onClick = { onGoClick(text) },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("Go")
        }
    }
}


@Composable
fun WebViewScreen(webView: WebView) {
    AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun rememberWebView(onUrlChange: (String) -> Unit): WebView {
    val context = LocalContext.current
    return remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    request?.url?.toString()?.let { onUrlChange(it) }
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    url?.let {
                        onUrlChange(it)
                    }
                }
            }
            loadUrl("https://ncode.syosetu.com/")
        }
    }
}


fun isNovelSite(url: String): Boolean {
    return url.contains("ncode.syosetu.com") || url.contains("booktoki468.com")
}

fun extractAndCopyNovel(context: Context, webView: WebView) {
    val jsCode = loadJavaScriptFromAssets(context, "background.js")

    webView.evaluateJavascript(jsCode) { result ->
        try {
            // 🔹 Fix: Properly decode the JSON response
            val cleanedResult = result
                .trim()
                .removeSurrounding("\"") // Removes extra surrounding quotes
                .replace("\\\\n", "\n") // Converts escaped `\n` to actual newlines
                .replace("\\\"", "\"") // Fixes escaped quotes

            val jsonObject = JSONObject(cleanedResult) // Properly parse JSON
            val extractedText = jsonObject.optString("text", "")
            val nextUrl = jsonObject.optString("nextUrl", "")

            if (extractedText.isNotEmpty() && !extractedText.contains("Error")) {
                copyToClipboard(context, extractedText)
                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()

                // 🔹 Auto-navigate to next chapter if available
                if (nextUrl.isNotEmpty()) {
                    webView.loadUrl(nextUrl)
                }
            } else {
                Toast.makeText(context, "Failed to extract novel content", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error parsing result", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}


fun loadJavaScriptFromAssets(context: Context, fileName: String): String {
    return try {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.use { it.readText() }
    } catch (e: Exception) {
        e.printStackTrace()
        "console.log('Error loading JavaScript file');"
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Novel Text", text)
    clipboard.setPrimaryClip(clip)
}