package com.github.kr328.clash.design.ui.icon

import androidx.compose.ui.graphics.vector.ImageVector
import com.github.kr328.clash.design.ui.icon.icons.`Arrow-down-up`
import com.github.kr328.clash.design.ui.icon.icons.Bolt
import com.github.kr328.clash.design.ui.icon.icons.House
import com.github.kr328.clash.design.ui.icon.icons.`Package-check`
import kotlin.collections.List as ____KtList

object Icons

private var __AllIcons: ____KtList<ImageVector>? = null

val Icons.AllIcons: ____KtList<ImageVector>
    get() {
        if (__AllIcons != null) {
            return __AllIcons!!
        }
        __AllIcons = listOf(`Arrow-down-up`, Bolt, House, `Package-check`)
        return __AllIcons!!
    }
