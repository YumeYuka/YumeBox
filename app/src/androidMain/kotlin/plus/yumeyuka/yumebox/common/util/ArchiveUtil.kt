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

package plus.yumeyuka.yumebox.common.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

object ArchiveUtil {

    fun unzipZip(
        zipFile: File,
        destination: File
    ): Boolean {
        if (!zipFile.exists() || !zipFile.isFile) return false

        return runCatching {
            val destinationPath = prepareDestination(destination)

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                generateSequence { zis.nextEntry }.forEach { entry ->
                    val outFile = resolveEntryTarget(destinationPath, entry.name)

                    if (entry.isDirectory) {
                        ensureDirectory(outFile)
                    } else {
                        writeEntry(zis, outFile)
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

    fun createZip(
        sourceFiles: List<File>,
        zipFile: File,
        basePath: File? = null,
        onProgress: ((String, Long, Long) -> Unit)? = null,
    ): Boolean {
        return runCatching {
            val totalSize = sourceFiles.sumOf { if (it.isFile) it.length() else 0L }
            var processedSize = 0L

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                sourceFiles.forEach { file ->
                    val relativePath = if (basePath != null) {
                        file.relativeTo(basePath).path
                    } else {
                        file.name
                    }

                    addToZip(file, relativePath, zos) { name, size ->
                        processedSize += size
                        onProgress?.invoke(name, processedSize, totalSize)
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun addToZip(
        file: File,
        path: String,
        zos: ZipOutputStream,
        onProgress: ((String, Long) -> Unit)? = null,
    ) {
        runCatching {
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    addToZip(child, "$path/${child.name}", zos, onProgress)
                }
            } else {
                val entry = ZipEntry(path)
                entry.time = file.lastModified()
                zos.putNextEntry(entry)

                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }

                zos.closeEntry()
                onProgress?.invoke(path, file.length())
            }
        }
    }

    fun getZipContents(zipFile: File): List<ZipEntryInfo> {
        if (!zipFile.exists() || !zipFile.isFile) return emptyList()

        return runCatching {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                generateSequence { zis.nextEntry }
                    .map { entry ->
                        ZipEntryInfo(
                            name = entry.name,
                            isDirectory = entry.isDirectory,
                            size = entry.size,
                            compressedSize = entry.compressedSize,
                            lastModified = entry.time,
                        )
                    }
                    .toList()
            }
        }.getOrDefault(emptyList())
    }

    fun isValidZip(zipFile: File): Boolean {
        return runCatching {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                generateSequence { zis.nextEntry }.any()
            }
        }.getOrDefault(false)
    }

    fun extractFileFromZip(
        zipFile: File,
        fileName: String,
        targetFile: File,
    ): Boolean {
        return runCatching {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                generateSequence { zis.nextEntry }
                    .find { it.name == fileName }
                    ?.let {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        true
                    } ?: false
            }
        }.getOrDefault(false)
    }

    fun containsFile(zipFile: File, fileName: String): Boolean {
        return runCatching {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                generateSequence { zis.nextEntry }
                    .any { it.name == fileName }
            }
        }.getOrDefault(false)
    }

    fun getFileSizeInZip(zipFile: File, fileName: String): Long {
        return runCatching {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                generateSequence { zis.nextEntry }
                    .find { it.name == fileName }
                    ?.size ?: -1L
            }
        }.getOrDefault(-1L)
    }

    fun unzipMultiple(
        zipFiles: List<File>,
        destination: File
    ): Boolean {
        var allSuccess = true

        zipFiles.forEachIndexed { index, zipFile ->
            if (!unzipZip(zipFile, destination)) {
                allSuccess = false
                return@forEachIndexed
            }
        }

        return allSuccess
    }

    fun untar(
        tarFile: File,
        destination: File
    ): Boolean {
        if (!tarFile.exists() || !tarFile.isFile) return false

        return runCatching {
            val destinationPath = prepareDestination(destination)

            TarArchiveInputStream(FileInputStream(tarFile)).use { tis ->
                var entry: TarArchiveEntry? = tis.nextTarEntry
                while (entry != null) {
                    val outFile = resolveEntryTarget(destinationPath, entry.name)

                    if (entry.isDirectory) {
                        ensureDirectory(outFile)
                    } else {
                        writeEntry(tis, outFile)
                    }

                    entry = tis.nextTarEntry
                }
            }
            true
        }.getOrDefault(false)
    }

    fun untarGz(
        tarGzFile: File,
        destination: File
    ): Boolean {
        if (!tarGzFile.exists() || !tarGzFile.isFile) return false

        return runCatching {
            val destinationPath = prepareDestination(destination)

            FileInputStream(tarGzFile).use { fis ->
                GzipCompressorInputStream(fis).use { gzis ->
                    TarArchiveInputStream(gzis).use { tis ->
                        var entry: TarArchiveEntry? = tis.nextTarEntry
                        while (entry != null) {
                            val outFile = resolveEntryTarget(destinationPath, entry.name)

                            if (entry.isDirectory) {
                                ensureDirectory(outFile)
                            } else {
                                writeEntry(tis, outFile)
                            }

                            entry = tis.nextTarEntry
                        }
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

    fun extractArchive(
        archiveFile: File,
        destination: File,
    ): Boolean {
        return when {
            archiveFile.name.endsWith(".zip", ignoreCase = true) -> {
                unzipZip(archiveFile, destination)
            }

            archiveFile.name.endsWith(".tar", ignoreCase = true) -> {
                untar(archiveFile, destination)
            }

            archiveFile.name.endsWith(".tar.gz", ignoreCase = true) ||
                    archiveFile.name.endsWith(".tgz", ignoreCase = true) -> {
                untarGz(archiveFile, destination)
            }

            else -> false
        }
    }

    private fun prepareDestination(destination: File): Path {
        if (!destination.exists()) {
            if (!destination.mkdirs()) throw IllegalStateException("无法创建目录: ${destination.absolutePath}")
        }
        if (!destination.isDirectory) throw IllegalStateException("目标不是目录: ${destination.absolutePath}")
        return destination.toPath().toAbsolutePath().normalize()
    }

    private fun resolveEntryTarget(destinationPath: Path, entryName: String): File {
        val normalizedEntry = destinationPath.resolve(entryName).normalize()
        if (!normalizedEntry.startsWith(destinationPath)) {
            throw SecurityException("检测到路径遍历: $entryName")
        }
        return normalizedEntry.toFile()
    }

    private fun ensureDirectory(directory: File) {
        if (directory.exists()) {
            if (!directory.isDirectory) throw IllegalStateException("路径已存在但不是目录: ${directory.absolutePath}")
        } else if (!directory.mkdirs()) {
            throw IllegalStateException("无法创建目录: ${directory.absolutePath}")
        }
    }

    private fun writeEntry(input: InputStream, targetFile: File) {
        targetFile.parentFile?.let { ensureDirectory(it) }
        FileOutputStream(targetFile).use { output ->
            input.copyTo(output)
        }
    }
}

data class ZipEntryInfo(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long,
    val lastModified: Long,
)

data class TarEntryInfo(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mode: Int,
)
