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

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import plus.yumeyuka.yumebox.data.model.Selection
import androidx.core.content.edit

class SelectionDao(context: Context) {
    companion object {
        private const val PREFS_NAME = "proxy_selections"
        private const val KEY_PREFIX = "selection_"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setSelected(selection: Selection) {
        try {
            val key = makeKey(selection.profileId, selection.proxyGroup)
            prefs.edit {
                putString(key, selection.selectedNode)
            }
        } catch (e: Exception) {
        }
    }

    fun getSelected(profileId: String, proxyGroup: String): String? {
        return try {
            val key = makeKey(profileId, proxyGroup)
            prefs.getString(key, null)
        } catch (e: Exception) {
            null
        }
    }

    fun removeSelected(profileId: String, proxyGroup: String) {
        try {
            val key = makeKey(profileId, proxyGroup)
            prefs.edit {
                remove(key)
            }
        } catch (e: Exception) {
        }
    }

    fun getAllSelections(profileId: String): Map<String, String> {
        return try {
            val prefix = makeKeyPrefix(profileId)
            val selections = mutableMapOf<String, String>()

            prefs.all.forEach { (key, value) ->
                if (key.startsWith(prefix) && value is String) {
                    val proxyGroup = key.substring(prefix.length)
                    selections[proxyGroup] = value
                }
            }

            selections
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun clearAllSelections(profileId: String) {
        try {
            val prefix = makeKeyPrefix(profileId)
            prefs.edit {

                prefs.all.keys
                    .filter { it.startsWith(prefix) }
                    .forEach { remove(it) }

            }
        } catch (e: Exception) {
        }
    }

    fun setSelections(profileId: String, selections: Map<String, String>) {
        try {
            prefs.edit {

                selections.forEach { (proxyGroup, selectedNode) ->
                    val key = makeKey(profileId, proxyGroup)
                    putString(key, selectedNode)
                }

            }
        } catch (e: Exception) {
        }
    }

    private fun makeKey(profileId: String, proxyGroup: String): String {
        return "${KEY_PREFIX}${profileId}_$proxyGroup"
    }

    private fun makeKeyPrefix(profileId: String): String {
        return "${KEY_PREFIX}${profileId}_"
    }
}
