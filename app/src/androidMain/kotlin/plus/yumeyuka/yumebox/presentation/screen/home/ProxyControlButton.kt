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

package plus.yumeyuka.yumebox.presentation.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import plus.yumeyuka.yumebox.common.AppConstants
import plus.yumeyuka.yumebox.presentation.icon.Yume
import plus.yumeyuka.yumebox.presentation.icon.yume.Play
import plus.yumeyuka.yumebox.presentation.icon.yume.Square
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import dev.oom_wg.purejoy.mlang.MLang

@Composable
fun ProxyControlButton(
    isRunning: Boolean,
    isEnabled: Boolean,
    hasEnabledProfile: Boolean,
    hasProfiles: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!hasProfiles) {
            Text(
                text = MLang.Home.Control.HintAddProfile,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        } else if (!hasEnabledProfile) {
            Text(
                text = MLang.Home.Control.HintEnableProfile,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }

        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(0.32f),
            enabled = isEnabled,
            colors = ButtonDefaults.buttonColorsPrimary(),
            cornerRadius = AppConstants.UI.BUTTON_CORNER_RADIUS,
            minHeight = 36.dp
        ) {
            Icon(
                imageVector = if (isRunning) Yume.Square else Yume.Play,
                contentDescription = if (isRunning) MLang.Home.Control.Stop else MLang.Home.Control.Start,
                tint = MiuixTheme.colorScheme.onPrimary
            )
        }
    }
}
