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

package core

import org.gradle.api.Project
import java.util.Properties

class ConfigProvider(private val project: Project) {
    private val catalog by lazy { project.rootProject.extensions.findByName("libs") }
    private val externalProperties by lazy { loadExternalProperties() }

    private fun loadExternalProperties(): Properties {
        val file = project.rootProject.file("kernel.properties")
        val props = Properties()
        if (file.isFile) {
            runCatching { file.inputStream().use(props::load) }
        }
        return props
    }

    private fun fromExternalConfig(key: String): String? {
        return externalProperties.getProperty(key)?.takeIf { it.isNotBlank() }
    }

    private fun fromGropify(key: String): String? {
        return try {
            val ext = project.extensions.findByName("gropify") ?: return null
            val m = ext.javaClass.methods.firstOrNull { it.name == "getPropertyValue" } ?: return null
            (m.invoke(ext, key) as? String)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun fromGradleProperties(key: String): String? {
        return project.findProperty(key)?.toString()?.takeIf { it.isNotBlank() }
    }

    private fun fromCatalog(key: String): String? {
        return try {
            val libsClass = catalog?.javaClass ?: return null
            val findVersion = libsClass.methods.firstOrNull { it.name == "findVersion" } ?: return null
            val versionObj = findVersion.invoke(catalog, key) ?: return null
            val reqVersion =
                versionObj.javaClass.methods.firstOrNull { it.name == "get" }?.invoke(versionObj) ?: return null
            val requiredVersion = reqVersion.javaClass.methods.firstOrNull { it.name == "getRequiredVersion" }
                ?.invoke(reqVersion) as? String
            requiredVersion?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    fun getString(key: String, fallback: String): String {
        return fromExternalConfig(key)
            ?: fromGropify(key)
            ?: fromGradleProperties(key)
            ?: fromCatalog(key)
            ?: fallback
    }

    fun getInt(key: String, fallback: Int): Int {
        return getString(key, fallback.toString()).toIntOrNull() ?: fallback
    }

    fun getCsv(key: String, fallback: String): List<String> {
        return getString(key, fallback).split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
}

fun Project.gropifyString(path: String, fallback: String): String {
    val ext = extensions.findByName("gropify") ?: return fallback
    return try {
        val method = ext.javaClass.methods.firstOrNull { it.name == "getPropertyValue" }
        val value = method?.invoke(ext, path) as? String
        if (value.isNullOrBlank()) fallback else value
    } catch (_: Exception) {
        fallback
    }
}
