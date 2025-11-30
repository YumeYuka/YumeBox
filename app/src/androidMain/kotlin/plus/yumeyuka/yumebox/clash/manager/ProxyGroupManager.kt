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

package plus.yumeyuka.yumebox.clash.manager

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import plus.yumeyuka.yumebox.clash.cache.GlobalDelayCache
import plus.yumeyuka.yumebox.domain.model.ProxyGroupInfo
import plus.yumeyuka.yumebox.core.model.*
import plus.yumeyuka.yumebox.core.Clash
import plus.yumeyuka.yumebox.data.model.Profile
import plus.yumeyuka.yumebox.data.repository.SelectionDao
import android.content.Context
import timber.log.Timber
import plus.yumeyuka.yumebox.data.model.Selection
import java.util.concurrent.ConcurrentHashMap

class ProxyGroupManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val delayCache: GlobalDelayCache
) {
    private val selectionDao = SelectionDao(context)
    private val proxyGroupStates = ConcurrentHashMap<String, ProxyGroupState>()

    private val _proxyGroupsShared = MutableSharedFlow<List<ProxyGroupInfo>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = _proxyGroupsShared
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    data class ProxyGroupState(var now: String = "", var lastUpdate: Long = 0)

    suspend fun refreshProxyGroups(skipCacheClear: Boolean = false, currentProfile: Profile? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val groupNames = Clash.queryGroupNames(excludeNotSelectable = false)

            val rawGroups = groupNames.associateWith { name ->
                val group = Clash.queryGroup(name, ProxySort.Default)
                val state = proxyGroupStates.getOrPut(name) { ProxyGroupState() }
                val previousNow = state.now
                val currentNow = group.now

                if (currentProfile != null && currentNow.isNotBlank() && currentNow != previousNow) {
                    selectionDao.setSelected(
                        Selection(currentProfile.id, name, currentNow)
                    )
                }

                state.now = currentNow
                state.lastUpdate = System.currentTimeMillis()
                group
            }

            rawGroups.values.flatMap { it.proxies }.forEach { proxy ->
                if (proxy.delay > 0) {
                    delayCache.updateDelay(proxy.name, proxy.delay)
                }
            }

            val cachedDelays = delayCache.getAllValidDelays()
            val mergedDelayMap = cachedDelays.toMutableMap()

            val finalGroups = groupNames.map { name ->
                val group = rawGroups[name]!!
                val enrichedProxies = group.proxies.map { proxy ->
                    val cachedDelay = mergedDelayMap[proxy.name]
                    var updatedProxy = if (cachedDelay != null && cachedDelay > 0) {
                        proxy.copy(delay = cachedDelay)
                    } else {
                        proxy
                    }

                    if (proxy.type.group) {
                        proxyGroupStates[proxy.name]?.takeIf { it.now.isNotEmpty() }?.let {
                            updatedProxy = updatedProxy.copy(subtitle = "${proxy.type}(${it.now})")
                        }
                    }
                    updatedProxy
                }

                val chainPath = if (group.type.group && group.now.isNotBlank()) {
                    buildProxyChain(name, group.now, rawGroups)
                } else {
                    emptyList()
                }

                ProxyGroupInfo(
                    name = name,
                    type = group.type,
                    proxies = enrichedProxies,
                    now = group.now,
                    chainPath = chainPath
                )
            }

            _proxyGroupsShared.emit(finalGroups)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "刷新代理组失败")
            Result.failure(e)
        }
    }

    suspend fun refreshSingleGroupSelection(groupName: String, proxyName: String) {
        runCatching {
            val currentGroups = proxyGroups.value.toMutableList()
            val groupIndex = currentGroups.indexOfFirst { it.name == groupName }

            if (groupIndex != -1) {
                val preservedProxies = currentGroups[groupIndex].proxies.map { proxy ->
                    val cachedDelay = delayCache.getDelay(proxy.name)
                    if (cachedDelay != null && cachedDelay > 0) {
                        proxy.copy(delay = cachedDelay)
                    } else {
                        proxy
                    }
                }

                val updatedGroup = currentGroups[groupIndex].copy(
                    now = proxyName,
                    proxies = preservedProxies,
                    chainPath = if (currentGroups[groupIndex].type.group) {
                        val groupsMap = currentGroups.associate { it.name to
                            ProxyGroup(
                                type = it.type,
                                now = if (it.name == groupName) proxyName else it.now,
                                proxies = it.proxies
                            )
                        }
                        buildProxyChain(groupName, proxyName, groupsMap)
                    } else {
                        currentGroups[groupIndex].chainPath
                    }
                )

                currentGroups[groupIndex] = updatedGroup
                _proxyGroupsShared.emit(currentGroups)
            }
        }
    }

    private fun buildProxyChain(
        groupName: String,
        currentNode: String,
        groups: Map<String, ProxyGroup>,
        visited: MutableSet<String> = mutableSetOf()
    ): List<String> {
        if (groupName in visited) return listOf(groupName)
        visited.add(groupName)

        val nextGroup = groups[currentNode] ?: return listOf(groupName, currentNode)
        val groupNow = nextGroup.now.ifBlank { proxyGroupStates[currentNode]?.now }

        return if (!groupNow.isNullOrBlank()) {
            listOf(groupName) + buildProxyChain(currentNode, groupNow, groups, visited)
        } else {
            listOf(groupName, currentNode)
        }
    }

    suspend fun selectProxy(groupName: String, proxyName: String, currentProfile: Profile?): Boolean {
        return try {
            val result = Clash.patchSelector(groupName, proxyName)
            if (result) {
                if (currentProfile != null) {
                    selectionDao.setSelected(
                        Selection(currentProfile.id, groupName, proxyName)
                    )
                }

                proxyGroupStates[groupName]?.now = proxyName

                delay(50)
                refreshSingleGroupSelection(groupName, proxyName)
            }
            result
        } catch (e: Exception) {
            false
        }
    }

    suspend fun refreshDelaysOnly() {
        runCatching {
            val currentGroups = _proxyGroupsShared.replayCache.firstOrNull() ?: return
            val cachedDelays = delayCache.getAllValidDelays()
            val updatedGroups = currentGroups.map { group ->
                val enrichedProxies = group.proxies.map { proxy ->
                    cachedDelays[proxy.name]?.takeIf { it > 0 }?.let { proxy.copy(delay = it) } ?: proxy
                }
                group.copy(proxies = enrichedProxies)
            }
            _proxyGroupsShared.emit(updatedGroups)
        }.onFailure { e ->
            Timber.e(e, "刷新延迟失败")
        }
    }

    suspend fun restoreSelections(profileId: String) {
        runCatching {
            val selections = selectionDao.getAllSelections(profileId)
            if (selections.isEmpty()) return
            selections.forEach { (groupName, proxyName) ->
                runCatching {
                    if (Clash.patchSelector(groupName, proxyName)) {
                        proxyGroupStates[groupName]?.now = proxyName
                    }
                }
            }
            delay(300)
        }
    }

    fun getGroupState(groupName: String): ProxyGroupState? = proxyGroupStates[groupName]

    fun clearGroupStates() = proxyGroupStates.clear()
}