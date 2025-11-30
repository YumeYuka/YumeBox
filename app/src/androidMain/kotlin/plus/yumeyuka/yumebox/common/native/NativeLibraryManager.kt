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

package plus.yumeyuka.yumebox.common.native

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import timber.log.Timber

object NativeLibraryManager {
    private const val LIBS_DIR_NAME = "libs"
    private var libsBaseDir: File? = null
    private var context: Context? = null
    private var isInitialized = false

    enum class LibraryType {
        JNI_LOAD,
        PROCESS_EXEC
    }

    enum class LibrarySource {
        MAIN_APK,
        EXTENSION_APK
    }

    data class LibraryInfo(
        val name: String,
        val type: LibraryType,
        val source: LibrarySource,
        val packageName: String? = null,
        val version: String? = null
    )

    private val managedLibraries = mutableMapOf<String, LibraryInfo>()

    fun initialize(context: Context) {
        if (isInitialized) return

        this.context = context
        libsBaseDir = File(context.filesDir, LIBS_DIR_NAME)
        libsBaseDir?.mkdirs()
        registerDefaultLibraries()
        isInitialized = true
        extractAllLibraries()
    }

    @SuppressLint("StaticFieldLeak")
    private fun registerDefaultLibraries() {
        registerLibrary(
            LibraryInfo(
                name = "libjavet-node-android.v.5.0.1.so",
                type = LibraryType.JNI_LOAD,
                source = LibrarySource.EXTENSION_APK,
                packageName = "plus.yumeyuka.yumebox.extension"
            )
        )
    }

    fun registerLibrary(info: LibraryInfo) {
        managedLibraries[info.name] = info
    }

    fun extractAllLibraries(): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()
        managedLibraries.forEach { (name, info) ->
            results[name] = extractLibrary(info)
        }
        return results
    }

    fun extractLibrary(info: LibraryInfo): Boolean {
        val targetDir = libsBaseDir ?: throw RuntimeException("Library manager not initialized")
        targetDir.mkdirs()
        val targetFile = File(targetDir, info.name)

        if (targetFile.exists() && targetFile.canRead()) {
            if (info.type == LibraryType.PROCESS_EXEC && !targetFile.canExecute()) {
                targetFile.setExecutable(true, false)
            }
            return true
        }

        return try {
            val result = when (info.source) {
                LibrarySource.MAIN_APK -> extractFromMainApk(info, targetFile)
                LibrarySource.EXTENSION_APK -> extractFromExtensionApk(info, targetFile)
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "提取库失败: ${info.name}")
            false
        }
    }

    private fun extractFromMainApk(info: LibraryInfo, targetFile: File): Boolean {
        val apkPath = context?.applicationInfo?.sourceDir
            ?: throw RuntimeException("Context not initialized")

        val abi = getSupportedAbi()
        ZipFile(apkPath).use { zip ->
            var libEntry = zip.getEntry("lib/$abi/${info.name}")
            if (libEntry == null) {
                val supportedAbis = Build.SUPPORTED_ABIS
                for (tryAbi in supportedAbis) {
                    libEntry = zip.getEntry("lib/$tryAbi/${info.name}")
                    if (libEntry != null) break
                }
            }

            if (libEntry == null) {
                throw RuntimeException("Library not found in APK: ${info.name}")
            }

            zip.getInputStream(libEntry).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            targetFile.setReadable(true, false)
            if (info.type == LibraryType.PROCESS_EXEC) {
                targetFile.setExecutable(true, false)
            }

            return true
        }
    }

    private fun extractFromExtensionApk(info: LibraryInfo, targetFile: File): Boolean {
        if (info.packageName == null) {
            throw RuntimeException("Package name required for extension APK source")
        }

        val extensionApk = getExtensionApk(info.packageName)
        if (extensionApk == null) {
            throw RuntimeException("Extension APK not found: ${info.packageName}")
        }

        val abi = getSupportedAbi()
        ZipFile(extensionApk).use { zip ->
            var entry = zip.getEntry("lib/$abi/${info.name}")
            if (entry == null) {
                val supportedAbis = Build.SUPPORTED_ABIS
                for (tryAbi in supportedAbis) {
                    val path = "lib/$tryAbi/${info.name}"
                    entry = zip.getEntry(path)
                    if (entry != null) break
                }
            }

            if (entry == null) {
                entry = zip.entries().asSequence().firstOrNull { e ->
                    e.name.startsWith("lib/") && e.name.endsWith(info.name)
                }
            }

            if (entry == null) {
                throw RuntimeException("Library not found in extension APK: ${info.name}")
            }

            zip.getInputStream(entry).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            targetFile.setReadable(true, false)
            if (info.type == LibraryType.PROCESS_EXEC) {
                targetFile.setExecutable(true, false)
            }

            return true
        }
    }

    private fun getExtensionApk(packageName: String): File? {
        return try {
            val pm = context?.packageManager ?: return null
            val info = pm.getApplicationInfo(packageName, 0)
            File(info.sourceDir)
        } catch (e: Exception) {
            null
        }
    }

    fun getLibraryPath(name: String): String? {
        if (!isInitialized) return null
        val libraryFile = File(libsBaseDir, name)
        return if (libraryFile.exists()) libraryFile.absolutePath else null
    }

    fun isLibraryAvailable(name: String): Boolean {
        val path = getLibraryPath(name) ?: return false
        val file = File(path)
        val info = managedLibraries[name]
        return when (info?.type) {
            LibraryType.JNI_LOAD -> file.exists() && file.canRead()
            LibraryType.PROCESS_EXEC -> file.exists() && file.canRead() && file.canExecute()
            null -> false
        }
    }

    fun loadJniLibrary(name: String): Boolean {
        val info = managedLibraries[name]
        if (info?.type != LibraryType.JNI_LOAD) {
            return false
        }

        val path = getLibraryPath(name) ?: return false

        return try {
            System.load(path)
            true
        } catch (e: UnsatisfiedLinkError) {
            Timber.e(e, "加载JNI库失败: $name")
            false
        }
    }

    fun getLibraryStatus(name: String): String {
        if (!isInitialized) return "Library manager not initialized"
        val info = managedLibraries[name] ?: return "Library not registered: $name"
        val path = getLibraryPath(name)

        return when {
            path == null -> "Library not extracted: $name"
            !File(path).exists() -> "Library file not found: $path"
            info.type == LibraryType.PROCESS_EXEC && !File(path).canExecute() ->
                "Library exists but not executable: $path"

            info.type == LibraryType.JNI_LOAD && !File(path).canRead() ->
                "Library exists but not readable: $path"

            else -> "Library ready: $name (${info.type}) at $path"
        }
    }

    fun getAllLibraryStatus(): Map<String, String> {
        return managedLibraries.keys.associateWith { getLibraryStatus(it) }
    }

    private fun getSupportedAbi(): String {
        val supportedABIs = Build.SUPPORTED_ABIS
        return when {
            supportedABIs.contains("arm64-v8a") -> "arm64-v8a"
            supportedABIs.contains("x86_64") -> "x86_64"
            supportedABIs.contains("armeabi-v7a") -> "armeabi-v7a"
            supportedABIs.contains("x86") -> "x86"
            else -> supportedABIs.firstOrNull() ?: "arm64-v8a"
        }
    }

    fun clearCache() {
    }

    fun getLibsBaseDir(): File? = libsBaseDir
}
