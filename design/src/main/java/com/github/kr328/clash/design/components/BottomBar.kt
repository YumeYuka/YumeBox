package com.github.kr328.clash.design.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.github.kr328.clash.design.store.UiStore
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem

object AppPreferences {
    private var uiStoreInstance: UiStore? = null

    fun init(uiStore: UiStore) {
        uiStoreInstance = uiStore
    }

    val showDivider: State<Boolean>
        get() = mutableStateOf(uiStoreInstance?.bottomBarShowDivider ?: false)

    val bottomBarFloating: State<Boolean>
        get() = mutableStateOf(uiStoreInstance?.bottomBarFloating ?: true)
}

@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    useFloating: Boolean = true,
    items: List<NavigationItem>,
    selected: Int,
    onPageChange: (Int) -> Unit
) {

    AnimatedContent(
        targetState = useFloating,
        transitionSpec = {
            (fadeIn(animationSpec = tween(300)) +
                    slideInVertically(
                        animationSpec = tween(300),
                        initialOffsetY = { it / 2 },
                    )).togetherWith(
                fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            animationSpec = tween(300),
                            targetOffsetY = { it / 2 },
                        ),
            )
        },
        label = "BottomBarStyleTransition",
    ) { isFloating ->
        if (isFloating) {
            FloatingNavigationBar(
                modifier = modifier,
                items = items,
                selected = selected,
                onClick = onPageChange,
                showDivider = showDivider,
            )
        } else {
            NavigationBar(
                modifier = modifier,
                items = items,
                selected = selected,
                onClick = onPageChange,
                showDivider = showDivider,
            )
        }
    }
}

