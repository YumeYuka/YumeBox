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

package plus.yumeyuka.yumebox.presentation.screen


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import plus.yumeyuka.yumebox.presentation.component.Card
import plus.yumeyuka.yumebox.presentation.component.ScreenLazyColumn
import plus.yumeyuka.yumebox.presentation.component.SmallTitle
import plus.yumeyuka.yumebox.presentation.component.TopBar
import plus.yumeyuka.yumebox.common.util.openUrl
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme
import dev.oom_wg.purejoy.mlang.MLang

@Composable
@Destination<RootGraph>
fun ContributorScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopBar(title = MLang.Contributor.Title, scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
            topPadding = 20.dp
        ) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = MLang.Contributor.Description,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
                SmallTitle(MLang.Contributor.Section.SpecialThanks)
                Card {
                    SuperArrow(
                        title = "Chenzyadb",
                        summary = MLang.Contributor.Contributors.ChenzyadbRole,
                        onClick = {
                            openUrl(context, "https://github.com/chenzyadb")
                        }
                    )
                }
                SmallTitle(MLang.Contributor.Section.Community)
                Card {
                    SuperArrow(
                        title = "白彩恋",
                        summary = "Github@ShIroRRen",
                        onClick = {
                            openUrl(context, "https://github.com/ShIroRRen")
                        },
                        rightActions = {
                            Text(
                                "丰川祥子",
                                modifier = Modifier.padding(end = 16.dp),
                                style = MiuixTheme.textStyles.body2,
                            )
                        }
                    )
                    SuperArrow(
                        title = "枫莹",
                        summary = "Github@FengYing1314",
                        onClick = {
                            openUrl(context, "https://github.com/FengYing1314")
                        },
                        rightActions = {
                            Text(
                                "若叶睦",
                                modifier = Modifier.padding(end = 16.dp),
                                style = MiuixTheme.textStyles.body2,
                            )
                        }
                    )
                    SuperArrow(
                        title = "诺芳",
                        summary = "Github@NuoFang6",
                        onClick = {
                            openUrl(context, "https://github.com/NuoFang6")
                        },
                        rightActions = {
                            Text(
                                "祐天寺若麦",
                                modifier = Modifier.padding(end = 16.dp),
                                style = MiuixTheme.textStyles.body2,
                            )
                        }
                    )
                    SuperArrow(
                        title = "Linso",
                        summary = "Github@Linso05",
                        onClick = {
                            openUrl(context, "https://github.com/Linso05")
                        },
                        rightActions = {
                            Text(
                                "八幡海铃",
                                modifier = Modifier.padding(end = 16.dp),
                                style = MiuixTheme.textStyles.body2,
                            )
                        }
                    )
                }
            }
        }
    }
}
