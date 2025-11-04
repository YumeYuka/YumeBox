package com.github.kr328.clash.design.util

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberDebouncedOnClick(
    debounceMillis: Long = 500L,
    onClick: () -> Unit
): () -> Unit {
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    return remember(onClick) {
        {
            if (!isProcessing) {
                isProcessing = true
                onClick()
                scope.launch {
                    delay(debounceMillis)
                    isProcessing = false
                }
            }
        }
    }
}

@Composable
fun rememberThrottledOnClick(
    throttleMillis: Long = 500L,
    onClick: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    return remember(onClick) {
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= throttleMillis) {
                lastClickTime = currentTime
                onClick()
            }
        }
    }
}

@Composable
fun rememberNavigationOnClick(
    onClick: () -> Unit
): () -> Unit {
    return rememberDebouncedOnClick(debounceMillis = 800L, onClick = onClick)
}

