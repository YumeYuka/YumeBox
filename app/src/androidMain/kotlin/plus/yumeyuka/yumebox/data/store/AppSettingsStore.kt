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

package plus.yumeyuka.yumebox.data.store

import com.tencent.mmkv.MMKV
import plus.yumeyuka.yumebox.data.store.MMKVPreference
import plus.yumeyuka.yumebox.presentation.theme.AppColorTheme
import plus.yumeyuka.yumebox.data.model.ThemeMode
import plus.yumeyuka.yumebox.data.model.AppLanguage

class AppSettingsStorage(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {

    val themeMode by enumFlow(ThemeMode.Auto)
    val colorTheme by enumFlow(AppColorTheme.ClassicMonochrome)
    val appLanguage by enumFlow(AppLanguage.System)
    val automaticRestart by boolFlow(false)
    val hideAppIcon by boolFlow(false)
    val showTrafficNotification by boolFlow(true)
    val bottomBarFloating by boolFlow(true)
    val showDivider by boolFlow(true)

    val oneWord by strFlow("So 愛のために泣けるのは，君がそこにいるから")

    val oneWordAuthor by strFlow("THERE IS A REASON")
}
