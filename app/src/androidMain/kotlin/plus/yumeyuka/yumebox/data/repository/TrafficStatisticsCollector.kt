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

package plus.yumeyuka.yumebox.data.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import plus.yumeyuka.yumebox.clash.manager.ClashManager
import plus.yumeyuka.yumebox.data.store.TrafficStatisticsStore
import timber.log.Timber

class TrafficStatisticsCollector(
    private val clashManager: ClashManager,
    private val trafficStatisticsStore: TrafficStatisticsStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "TrafficStatisticsCollector"
        private const val COLLECTION_INTERVAL_MS = 5000L
    }

    private var collectionJob: Job? = null
    private var lastTotalUpload: Long = 0L
    private var lastTotalDownload: Long = 0L
    private var lastProfileId: String? = null

    init {
        startCollection()
    }

    private fun startCollection() {
        collectionJob?.cancel()
        collectionJob = scope.launch {
            clashManager.isRunning.collect { isRunning ->
                if (isRunning) {
                    startTrafficMonitoring()
                } else {
                    resetLastValues()
                }
            }
        }
    }

    private fun CoroutineScope.startTrafficMonitoring() {
        launch {
            lastTotalUpload = trafficStatisticsStore.getLastTrafficUpload()
            lastTotalDownload = trafficStatisticsStore.getLastTrafficDownload()
            lastProfileId = trafficStatisticsStore.getLastProfileId()

            while (isActive && clashManager.isRunning.value) {
                try {
                    collectTrafficData()
                    delay(COLLECTION_INTERVAL_MS)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "流量数据收集失败")
                    delay(COLLECTION_INTERVAL_MS)
                }
            }
        }
    }

    private fun collectTrafficData() {
        val trafficTotal = clashManager.trafficTotal.value
        val currentUpload = trafficTotal.upload
        val currentDownload = trafficTotal.download
        val currentProfile = clashManager.currentProfile.value
        val currentProfileId = currentProfile?.id
        val currentProfileName = currentProfile?.name

        if (lastTotalUpload == 0L && lastTotalDownload == 0L) {
            lastTotalUpload = currentUpload
            lastTotalDownload = currentDownload
            lastProfileId = currentProfileId
            trafficStatisticsStore.setLastTraffic(currentUpload, currentDownload, currentProfileId)
            return
        }

        if (currentProfileId != lastProfileId) {
            Timber.tag(TAG).d("检测到配置切换: $lastProfileId -> $currentProfileId")
            lastTotalUpload = currentUpload
            lastTotalDownload = currentDownload
            lastProfileId = currentProfileId
            trafficStatisticsStore.setLastTraffic(currentUpload, currentDownload, currentProfileId)
            return
        }

        if (currentUpload < lastTotalUpload || currentDownload < lastTotalDownload) {
            Timber.tag(TAG).d("检测到代理重启，重置计数器")
            lastTotalUpload = currentUpload
            lastTotalDownload = currentDownload
            trafficStatisticsStore.setLastTraffic(currentUpload, currentDownload, currentProfileId)
            return
        }

        val uploadDelta = currentUpload - lastTotalUpload
        val downloadDelta = currentDownload - lastTotalDownload

        if (uploadDelta > 0 || downloadDelta > 0) {
            trafficStatisticsStore.recordTraffic(
                uploadDelta, 
                downloadDelta,
                currentProfileId,
                currentProfileName
            )
            Timber.tag(TAG).v("记录流量: 上传=$uploadDelta, 下载=$downloadDelta, 配置=$currentProfileName")
        }

        lastTotalUpload = currentUpload
        lastTotalDownload = currentDownload
        trafficStatisticsStore.setLastTraffic(currentUpload, currentDownload, currentProfileId)
    }

    private fun resetLastValues() {
        lastTotalUpload = 0L
        lastTotalDownload = 0L
        lastProfileId = null
    }

    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
    }
}
