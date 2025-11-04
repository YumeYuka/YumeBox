package plus.yumeyuka.yumeyuka

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import plus.yumeyuka.yumeyuka.ui.HomeScreen

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "ComposeTarget") {
        HomeScreen()
    }
}

