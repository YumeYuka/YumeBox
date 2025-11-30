/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) YumeYuka & YumeLira 2025.
 *
 */

package plus.yumeyuka.yumebox.presentation.webview

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import org.koin.androidx.compose.koinViewModel
import plus.yumeyuka.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import plus.yumeyuka.yumebox.presentation.theme.YumeTheme
import plus.yumeyuka.yumebox.presentation.component.LocalWebView
import plus.yumeyuka.yumebox.presentation.viewmodel.AppSettingsViewModel
import top.yukonga.miuix.kmp.basic.Text
import dev.oom_wg.purejoy.mlang.MLang


@Composable
fun WebViewScreen(
    initialUrl: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val themeMode = appSettingsViewModel.themeMode.state.collectAsState().value
    val colorTheme = appSettingsViewModel.colorTheme.state.collectAsState().value

    var webViewError by remember { mutableStateOf<String?>(null) }

    BackHandler {
        onBack?.invoke() ?: activity?.finish()
    }

    ProvideAndroidPlatformTheme {
        YumeTheme(
            themeMode = themeMode,
            colorTheme = colorTheme,
        ) {
            Box(modifier = modifier) {
                if (webViewError != null) {
                    Text(
                        text = webViewError!!,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center,
                    )
                } else if (initialUrl.isNotEmpty()) {
                    LocalWebView(
                        initialUrl = initialUrl,
                        modifier = Modifier.fillMaxSize(),
                        enableDebug = true,
                        onPageFinished = { url: String ->
                        },
                        onPageError = { url: String, error: String ->
                            if (url.endsWith("index.html") && (error.contains("404") || error.contains("Not Found"))) {
                                webViewError = MLang.Component.WebView.LoadFailed
                            }
                        },
                    )
                } else {
                    Text(
                        text = MLang.Component.WebView.InvalidUrl,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
