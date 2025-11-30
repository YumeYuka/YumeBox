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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContributorScreenDestination
import com.ramcosta.composedestinations.generated.destinations.OpenSourceLicensesScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import plus.yumeyuka.yumebox.common.util.openUrl
import plus.yumeyuka.yumebox.core.bridge.Bridge
import plus.yumeyuka.yumebox.presentation.component.Card
import plus.yumeyuka.yumebox.presentation.component.LinkItem
import plus.yumeyuka.yumebox.presentation.component.ScreenLazyColumn
import plus.yumeyuka.yumebox.presentation.component.SmallTitle
import plus.yumeyuka.yumebox.presentation.component.TopBar
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun AboutScreen(navigator: DestinationsNavigator) {

    val scrollBehavior = MiuixScrollBehavior()
    var coreVersion by remember { mutableStateOf(MLang.About.App.VersionLoading) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            coreVersion = Bridge.nativeCoreVersion()
        } catch (e: Exception) {
            coreVersion = MLang.About.App.VersionFailed
        }
    }

    Scaffold(
        topBar = {
            TopBar(title = MLang.About.Title, scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Icon(
                        painter = painterResource(id = plus.yumeyuka.yumebox.R.drawable.yume),
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        tint = Color.Unspecified
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "YumeBox",
                        style = MiuixTheme.textStyles.title1
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${plus.yumeyuka.yumebox.BuildConfig.VERSION_NAME} ($coreVersion)",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "YumeBox",
                            style = MiuixTheme.textStyles.title3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = MLang.About.App.Description,
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
                SmallTitle(MLang.About.Section.Developer)
                Card {
                    SuperArrow(
                        title = MLang.About.Developer.Name,
                        summary = "Github@YumeYuka",
                        onClick = {
                            openUrl(context, "https://github.com/YumeYuka")
                        },
                        leftAction = {
                            Image(
                                painter = painterResource(id = plus.yumeyuka.yumebox.R.drawable.avatar),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                        },
                        rightActions = {
                            Text(
                                MLang.About.Developer.Nick,
                                modifier = Modifier.padding(end = 16.dp),
                                style = MiuixTheme.textStyles.body2,
                            )
                        }
                    )
                    SuperArrow(
                        title = MLang.About.Developer.ContributorTitle,
                        onClick = { navigator.navigate(ContributorScreenDestination) }
                    )
                }
                SmallTitle(MLang.About.Section.ProjectLinks)

                Card {
                    LinkItem(
                        title = "YumeBox",
                        url = "https://github.com/YumeYuka/YumeBox"
                    )
                    LinkItem(
                        title = "Mihomo",
                        url = "https://github.com/MetaCubeX/mihomo"
                    )
                }
                SmallTitle(MLang.About.Section.More)

                Card {
                    LinkItem(
                        title = MLang.About.Link.Sponsor,
                        url = "https://www.yumeyuka.plus/about/",
                        showArrow = true
                    )
                    LinkItem(
                        title = MLang.About.Link.TelegramGroup,
                        url = "https://t.me/OOM_Group",
                        showArrow = true
                    )
                    LinkItem(
                        title = MLang.About.Link.TelegramChannel,
                        url = "https://t.me/YumeYuka_official",
                        showArrow = true
                    )
                }
                SmallTitle(MLang.About.Section.License)

                Card {
                    SuperArrow(
                        title = MLang.About.License.Libraries,
                        summary = MLang.About.License.LibrariesSummary,
                        onClick = { navigator.navigate(OpenSourceLicensesScreenDestination) }
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = MLang.About.License.AgplName,
                            style = MiuixTheme.textStyles.body1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = MLang.About.License.AgplDescription,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = MLang.About.Copyright,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CircleAvatar(
    image: Painter,
    size: Dp = 40.dp
) {
    Image(
        painter = image,
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
    )
}
