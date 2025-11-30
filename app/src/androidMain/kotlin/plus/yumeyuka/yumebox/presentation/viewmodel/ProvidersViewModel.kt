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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import plus.yumeyuka.yumebox.clash.manager.ClashManager
import plus.yumeyuka.yumebox.core.Clash
import plus.yumeyuka.yumebox.core.model.Provider
import dev.oom_wg.purejoy.mlang.MLang

class ProvidersViewModel(
    private val clashManager: ClashManager
) : ViewModel() {

    private val _providers = MutableStateFlow<List<Provider>>(emptyList())
    val providers: StateFlow<List<Provider>> = _providers.asStateFlow()

    private val _uiState = MutableStateFlow(ProvidersUiState())
    val uiState: StateFlow<ProvidersUiState> = _uiState.asStateFlow()

    val isRunning: StateFlow<Boolean> = clashManager.isRunning

    fun refreshProviders() {
        viewModelScope.launch {
            if (!clashManager.isRunning.value) {
                _providers.value = emptyList()
                return@launch
            }

            try {
                _uiState.update { it.copy(isLoading = true) }
                val providerList = Clash.queryProviders()
                _providers.value = providerList.sorted()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = MLang.Providers.Message.FetchFailed.format(e.message)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateProvider(provider: Provider) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(updatingProviders = it.updatingProviders + provider.name) }
                Clash.updateProvider(provider.type, provider.name).await()
                refreshProviders()
                _uiState.update { it.copy(message = MLang.Providers.Message.UpdateSuccess.format(provider.name)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = MLang.Providers.Message.UpdateFailed.format(e.message)) }
            } finally {
                _uiState.update { it.copy(updatingProviders = it.updatingProviders - provider.name) }
            }
        }
    }

    fun updateAllProviders() {
        viewModelScope.launch {
            val httpProviders = _providers.value.filter { it.vehicleType == Provider.VehicleType.HTTP }
            if (httpProviders.isEmpty()) return@launch

            try {
                _uiState.update { it.copy(isUpdatingAll = true) }
                val providerNames = httpProviders.map { it.name }.toSet()
                _uiState.update { it.copy(updatingProviders = providerNames) }

                httpProviders.forEach { provider ->
                    try {
                        Clash.updateProvider(provider.type, provider.name).await()
                    } catch (e: Exception) {
                    }
                }

                refreshProviders()
                _uiState.update { it.copy(message = MLang.Providers.Message.AllUpdated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = MLang.Providers.Message.UpdateFailed.format(e.message)) }
            } finally {
                _uiState.update { it.copy(isUpdatingAll = false, updatingProviders = emptySet()) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    data class ProvidersUiState(
        val isLoading: Boolean = false,
        val isUpdatingAll: Boolean = false,
        val updatingProviders: Set<String> = emptySet(),
        val message: String? = null,
        val error: String? = null
    )
}
