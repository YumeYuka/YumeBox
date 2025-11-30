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

package plus.yumeyuka.yumebox.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import plus.yumeyuka.yumebox.data.model.Profile
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.AddSecret
import top.yukonga.miuix.kmp.icon.icons.useful.Delete
import top.yukonga.miuix.kmp.icon.icons.useful.Edit
import top.yukonga.miuix.kmp.icon.icons.useful.New
import top.yukonga.miuix.kmp.icon.icons.useful.Refresh
import top.yukonga.miuix.kmp.icon.icons.useful.Share
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import dev.oom_wg.purejoy.mlang.MLang

@Composable
fun ProfileCard(
    profile: Profile,
    isDownloading: Boolean = false,
    onExport: (Profile) -> Unit,
    onUpdate: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onEdit: (Profile) -> Unit,
    onToggleEnabled: (Profile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MiuixTheme.colorScheme


    val isDark = isSystemInDarkTheme()
    val secondaryContainer = colorScheme.secondaryContainer.copy(alpha = 0.8f)
    val actionIconTint = remember(isDark) { colorScheme.onSurface.copy(alpha = if (isDark) 0.7f else 0.9f) }


    val updateBg = remember(colorScheme) { colorScheme.tertiaryContainer.copy(alpha = 0.6f) }
    val updateTint = remember(colorScheme) { colorScheme.onTertiaryContainer.copy(alpha = 0.8f) }

    val isConfigSaved = remember(profile) {
        val importedDir = java.io.File(context.filesDir, "imported")
        val profileDir = java.io.File(importedDir, profile.id)
        val configFile = java.io.File(profileDir, "config.yaml")
        val hasValidFile = configFile.exists() && configFile.length() > 10

        val hasDownloaded = profile.lastUpdatedAt != null && profile.lastUpdatedAt!! >= profile.createdAt

        hasValidFile && hasDownloaded
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(16.dp)
    ) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {

                Text(
                    text = profile.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.getDisplayProvider(),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                        fontWeight = FontWeight(550),
                        color = colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isConfigSaved) {
                        Text(
                            text = MLang.Component.ProfileCard.Unsaved,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp),
                            fontWeight = FontWeight(550),
                            color = colorScheme.error,
                            maxLines = 1
                        )
                    }
                }
            }

            Switch(
                checked = profile.enabled,
                enabled = isConfigSaved && !isDownloading,
                onCheckedChange = { newValue -> onToggleEnabled(profile.copy(enabled = newValue)) }
            )
        }


        val infoText = remember(profile) {
            profile.getInfoText()
        }

        Column(modifier = Modifier.padding(top = 8.dp)) {

            val lines = infoText.split('\n')

            lines.forEachIndexed { index, line ->
                when {

                    line.contains('|') -> {
                        val parts = line.split('|')
                        val expireText = parts.getOrNull(0) ?: ""
                        val timeText = parts.getOrNull(1) ?: ""

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = expireText,
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                                lineHeight = 20.sp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )

                            if (timeText.isNotEmpty()) {
                                Text(
                                    text = timeText,
                                    fontSize = 12.sp,
                                    color = colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier.padding(end = 13.dp)
                                )
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = line,
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            lineHeight = 20.sp,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }
            }
        }


        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            thickness = 0.5.dp,
            color = colorScheme.outline.copy(alpha = 0.5f)
        )


        Row(verticalAlignment = Alignment.CenterVertically) {


            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                IconButton(
                    backgroundColor = secondaryContainer,
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    enabled = isConfigSaved && !isDownloading,
                    onClick = { if (isConfigSaved && !isDownloading) onExport(profile) }
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp).alpha(if (isConfigSaved) 1f else 0.4f),
                        imageVector = MiuixIcons.Useful.Share,
                        tint = actionIconTint.copy(alpha = if (isConfigSaved) 1f else 0.4f),
                        contentDescription = MLang.Component.ProfileCard.Export
                    )
                }


                IconButton(
                    backgroundColor = secondaryContainer,
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    enabled = !isDownloading,
                    onClick = { if (!isDownloading) onEdit(profile) }
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = MiuixIcons.Useful.Edit,
                        tint = actionIconTint,
                        contentDescription = MLang.Component.ProfileCard.Edit
                    )
                }
            }

            Spacer(Modifier.weight(1f))




            if (profile.shouldShowUpdateButton()) {
                IconButton(
                    modifier = Modifier.padding(end = 8.dp),
                    backgroundColor = updateBg,
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    enabled = !isDownloading,
                    onClick = { if (!isDownloading) onUpdate(profile) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = MiuixIcons.Useful.Refresh,
                            tint = updateTint,
                            contentDescription = MLang.Component.ProfileCard.Update,
                        )
                        Text(
                            modifier = Modifier.padding(end = 3.dp),
                            text = MLang.Component.ProfileCard.Update,
                            color = updateTint,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }
            }


            IconButton(
                minHeight = 35.dp,
                minWidth = 35.dp,
                enabled = !isDownloading,
                onClick = { if (!isDownloading) onDelete(profile) },
                backgroundColor = secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = MiuixIcons.Useful.Delete,
                        tint = actionIconTint,
                        contentDescription = MLang.Component.ProfileCard.Delete
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp, end = 3.dp),
                        text = MLang.Component.ProfileCard.Delete,
                        color = actionIconTint,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

