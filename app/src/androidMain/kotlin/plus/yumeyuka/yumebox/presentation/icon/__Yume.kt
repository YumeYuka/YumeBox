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

package plus.yumeyuka.yumebox.presentation.icon

import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.String
import plus.yumeyuka.yumebox.presentation.icon.yume.Bolt
import plus.yumeyuka.yumebox.presentation.icon.yume.Chromium
import plus.yumeyuka.yumebox.presentation.icon.yume.Github
import plus.yumeyuka.yumebox.presentation.icon.yume.House
import plus.yumeyuka.yumebox.presentation.icon.yume.Link
import plus.yumeyuka.yumebox.presentation.icon.yume.Meta
import plus.yumeyuka.yumebox.presentation.icon.yume.Play
import plus.yumeyuka.yumebox.presentation.icon.yume.Rocket
import plus.yumeyuka.yumebox.presentation.icon.yume.Sparkles
import plus.yumeyuka.yumebox.presentation.icon.yume.Square
import plus.yumeyuka.yumebox.presentation.icon.yume.Substore
import plus.yumeyuka.yumebox.presentation.icon.yume.Zap
import plus.yumeyuka.yumebox.presentation.icon.yume.Zashboard
import plus.yumeyuka.yumebox.presentation.icon.yume.`Arrow-down-up`
import plus.yumeyuka.yumebox.presentation.icon.yume.`List-chevrons-up-down`
import plus.yumeyuka.yumebox.presentation.icon.yume.`Package-check`
import plus.yumeyuka.yumebox.presentation.icon.yume.`Redo-dot`
import plus.yumeyuka.yumebox.presentation.icon.yume.`Scan-eye`
import plus.yumeyuka.yumebox.presentation.icon.yume.`Scroll-text`
import plus.yumeyuka.yumebox.presentation.icon.yume.`Settings-2`
import plus.yumeyuka.yumebox.presentation.icon.yume.`Squares-exclude`
import plus.yumeyuka.yumebox.presentation.icon.yume.`Wifi-cog`
import kotlin.collections.List as ____KtList
import kotlin.collections.Map as ____KtMap

object Yume

private var __AllIcons: ____KtList<ImageVector>? = null

val Yume.AllIcons: ____KtList<ImageVector>
  get() {
    if (__AllIcons != null) {
      return __AllIcons!!
    }
    __AllIcons= listOf(`Arrow-down-up`, Bolt, Chromium, Github, House, Link,
        `List-chevrons-up-down`, Meta, `Package-check`, Play, `Redo-dot`, Rocket, `Scan-eye`,
        `Scroll-text`, `Settings-2`, Sparkles, Square, `Squares-exclude`, Substore, `Wifi-cog`, Zap,
        Zashboard)
    return __AllIcons!!
  }

private var __AllIconsNamed: ____KtMap<String, ImageVector>? = null

val Yume.AllIconsNamed: ____KtMap<String, ImageVector>
  get() {
    if (__AllIconsNamed != null) {
      return __AllIconsNamed!!
    }
    __AllIconsNamed= mapOf("arrow-down-up" to `Arrow-down-up`, "bolt" to Bolt, "chromium" to
        Chromium, "github" to Github, "house" to House, "link" to Link, "list-chevrons-up-down" to
        `List-chevrons-up-down`, "meta" to Meta, "package-check" to `Package-check`, "play" to Play,
        "redo-dot" to `Redo-dot`, "rocket" to Rocket, "scan-eye" to `Scan-eye`, "scroll-text" to
        `Scroll-text`, "settings-2" to `Settings-2`, "sparkles" to Sparkles, "square" to Square,
        "squares-exclude" to `Squares-exclude`, "substore" to Substore, "wifi-cog" to `Wifi-cog`,
        "zap" to Zap, "zashboard" to Zashboard)
    return __AllIconsNamed!!
  }
