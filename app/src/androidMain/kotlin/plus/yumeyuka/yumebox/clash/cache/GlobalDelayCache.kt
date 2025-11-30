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

package plus.yumeyuka.yumebox.clash.cache

import java.util.concurrent.ConcurrentHashMap

class GlobalDelayCache(
    private val validityDuration: Long = 30 * 60 * 1000L
) {
    data class DelayInfo(
        val delay: Int,
        val testUrl: String,
        val timestamp: Long
    ) {
        fun isValid(now: Long, duration: Long) = now - timestamp < duration
    }

    private val cache = ConcurrentHashMap<String, DelayInfo>()

    fun updateDelay(nodeName: String, delay: Int, testUrl: String = "default") {
        if (delay > 0) {
            cache[nodeName] = DelayInfo(delay, testUrl, System.currentTimeMillis())
        }
    }

    fun updateDelays(delays: Map<String, Int>, testUrl: String = "default") {
        val timestamp = System.currentTimeMillis()
        delays.forEach { (nodeName, delay) ->
            if (delay > 0) {
                cache[nodeName] = DelayInfo(delay, testUrl, timestamp)
            }
        }
    }

    fun getDelay(nodeName: String): Int? {
        val info = cache[nodeName] ?: return null
        val now = System.currentTimeMillis()
        return if (info.isValid(now, validityDuration)) {
            info.delay
        } else {
            cache.remove(nodeName)
            null
        }
    }

    fun getDelayInfo(nodeName: String): DelayInfo? {
        val info = cache[nodeName] ?: return null
        val now = System.currentTimeMillis()
        return if (info.isValid(now, validityDuration)) info else { cache.remove(nodeName); null }
    }

    fun getAllValidDelays(): Map<String, Int> {
        val now = System.currentTimeMillis()
        return cache.entries
            .filter { it.value.isValid(now, validityDuration) }
            .associate { it.key to it.value.delay }
    }

    fun clearDelay(nodeName: String) = cache.remove(nodeName)

    fun clearAll() = cache.clear()

    fun clearExpired(): Int {
        val now = System.currentTimeMillis()
        val expiredKeys = cache.entries
            .filter { !it.value.isValid(now, validityDuration) }
            .map { it.key }
        expiredKeys.forEach { cache.remove(it) }
        return expiredKeys.size
    }
}
