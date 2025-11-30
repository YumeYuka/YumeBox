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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

interface UiStateManager {
    val uiState: StateFlow<UiState>
    fun setLoading(loading: Boolean)
    fun showMessage(message: String)
    fun showError(error: String)
    fun clearMessage()
    fun clearError()
}

class UiStateDelegate : UiStateManager {
    private val _uiState = MutableStateFlow(UiState())
    override val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    override fun setLoading(loading: Boolean) = _uiState.update { it.copy(isLoading = loading) }
    override fun showMessage(message: String) = _uiState.update { it.copy(message = message) }
    override fun showError(error: String) = _uiState.update { it.copy(error = error) }
    override fun clearMessage() = _uiState.update { it.copy(message = null) }
    override fun clearError() = _uiState.update { it.copy(error = null) }
}

abstract class BaseViewModel(
    private val delegate: UiStateDelegate = UiStateDelegate()
) : ViewModel(), UiStateManager by delegate
