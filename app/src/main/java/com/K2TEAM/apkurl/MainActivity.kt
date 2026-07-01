package com.K2TEAM.apkurl

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.K2TEAM.apkurl.ui.theme.APKurlTheme

class MainActivity : ComponentActivity() {
    private var interceptedUrl by mutableStateOf<String?>(null)
    private var isDefaultBrowser by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateInterceptedUrl(intent)
        isDefaultBrowser = checkIsDefaultBrowser()
        enableEdgeToEdge()
        setContent {
            APKurlTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BrowserEntryScreen(
                        interceptedUrl = interceptedUrl,
                        isDefaultBrowser = isDefaultBrowser,
                        onSetDefaultBrowser = ::openDefaultBrowserSettings,
                        onCopyUrl = ::copyInterceptedUrl,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDefaultBrowserState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateInterceptedUrl(intent)
    }

    private fun extractUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        return intent.dataString
    }

    private fun updateInterceptedUrl(intent: Intent?) {
        interceptedUrl = null
        interceptedUrl = extractUrl(intent)
    }

    private fun openDefaultBrowserSettings() {
        if (isDefaultBrowser) {
            Toast.makeText(
                this,
                "APKurl \u5df2\u7ecf\u662f\u9ed8\u8ba4\u6d4f\u89c8\u5668",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val intent = createDefaultBrowserIntent()
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "\u65e0\u6cd5\u6253\u5f00\u9ed8\u8ba4\u5e94\u7528\u8bbe\u7f6e",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun createDefaultBrowserIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
            }
        }

        return Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
    }

    private fun refreshDefaultBrowserState() {
        isDefaultBrowser = checkIsDefaultBrowser()
    }

    private fun checkIsDefaultBrowser(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
            }
        }

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val resolvedActivity = packageManager.resolveActivity(
            browserIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolvedActivity?.activityInfo?.packageName == packageName
    }

    private fun copyInterceptedUrl(url: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Intercepted URL", url))
        Toast.makeText(
            this,
            "\u5df2\u590d\u5236\u94fe\u63a5",
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
fun BrowserEntryScreen(
    interceptedUrl: String?,
    isDefaultBrowser: Boolean,
    onSetDefaultBrowser: () -> Unit,
    onCopyUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF2F3033),
        contentColor = Color.White,
        disabledContainerColor = Color(0xFFD7D7D9),
        disabledContentColor = Color(0xFF66666A)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(text = "APKurl")
        Text(
            text = if (isDefaultBrowser) {
                "APKurl \u5f53\u524d\u5df2\u662f\u9ed8\u8ba4\u6d4f\u89c8\u5668"
            } else {
                "\u8bbe\u4e3a\u9ed8\u8ba4\u6d4f\u89c8\u5668\u540e\uff0c\u53ef\u62e6\u622a\u5916\u90e8 http \u548c https \u94fe\u63a5"
            }
        )
        OutlinedTextField(
            value = interceptedUrl.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("\u62e6\u622a\u5230\u7684\u94fe\u63a5") },
            placeholder = { Text("\u6682\u65e0\u94fe\u63a5") },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        )
        Button(
            onClick = { interceptedUrl?.let(onCopyUrl) },
            enabled = !interceptedUrl.isNullOrBlank(),
            colors = buttonColors,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("\u590d\u5236\u94fe\u63a5")
        }
        Button(
            onClick = onSetDefaultBrowser,
            enabled = !isDefaultBrowser,
            colors = buttonColors,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isDefaultBrowser) {
                    "\u5df2\u8bbe\u4e3a\u9ed8\u8ba4\u6d4f\u89c8\u5668"
                } else {
                    "\u4e00\u952e\u8bbe\u7f6e\u9ed8\u8ba4\u6d4f\u89c8\u5668"
                }
            )
        }
    }
}
