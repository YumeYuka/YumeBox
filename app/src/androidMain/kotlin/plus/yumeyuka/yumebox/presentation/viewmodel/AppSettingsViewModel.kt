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

package plus.yumeyuka.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import plus.yumeyuka.yumebox.data.store.Preference
import plus.yumeyuka.yumebox.data.store.AppSettingsStorage
import plus.yumeyuka.yumebox.data.model.ThemeMode
import plus.yumeyuka.yumebox.data.model.AppLanguage
import plus.yumeyuka.yumebox.presentation.theme.AppColorTheme


class AppSettingsViewModel(
    private val storage: AppSettingsStorage,
) : ViewModel() {


    val themeMode: Preference<ThemeMode> = storage.themeMode
    val colorTheme: Preference<AppColorTheme> = storage.colorTheme
    val appLanguage: Preference<AppLanguage> = storage.appLanguage
    val automaticRestart: Preference<Boolean> = storage.automaticRestart
    val hideAppIcon: Preference<Boolean> = storage.hideAppIcon
    val showTrafficNotification: Preference<Boolean> = storage.showTrafficNotification
    val bottomBarFloating: Preference<Boolean> = storage.bottomBarFloating
    val showDivider: Preference<Boolean> = storage.showDivider

    val oneWord: Preference<String> = storage.oneWord
    val oneWordAuthor: Preference<String> = storage.oneWordAuthor


    fun onThemeModeChange(mode: ThemeMode) = themeMode.set(mode)
    fun onColorThemeChange(theme: AppColorTheme) = colorTheme.set(theme)
    fun onAppLanguageChange(language: AppLanguage) = appLanguage.set(language)
    fun onAutomaticRestartChange(enabled: Boolean) = automaticRestart.set(enabled)
    fun onHideAppIconChange(hide: Boolean) = hideAppIcon.set(hide)
    fun onShowTrafficNotificationChange(show: Boolean) = showTrafficNotification.set(show)
    fun onBottomBarFloatingChange(floating: Boolean) = bottomBarFloating.set(floating)
    fun onShowDividerChange(show: Boolean) = showDivider.set(show)

    fun onOneWordChange(text: String) = oneWord.set(text)
    fun onOneWordAuthorChange(author: String) = oneWordAuthor.set(author)
}
