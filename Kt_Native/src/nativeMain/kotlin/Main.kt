import kotlinx.cinterop.*
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen

@OptIn(ExperimentalForeignApi::class)
fun executeCommand(command: String): String {
    memScoped {
        val fp = popen(command, "r") ?: error("Failed to run command: $command")

        val buffer = allocArray<ByteVar>(4096)
        val output = StringBuilder()

        while (fgets(buffer, 4096, fp) != null) {
            output.append(buffer.toKString())
        }

        pclose(fp)
        return output.toString()
    }
}

@OptIn(ExperimentalForeignApi::class)
fun executeSuCommand(command: String): String {
    return executeCommand("su -c \"$command\"")
}

@OptIn(ExperimentalForeignApi::class)
fun executeCommandWithSELinux(command: String): String {
    val selinuxCommands = """
        setenforce 0
        $command
        setenforce 1
    """.trimIndent().replace("\n", " && ")

    return executeSuCommand(selinuxCommands)
}

enum class VersionCompareResult {
    LOWER, EQUAL, HIGHER
}

interface GameManager {
    fun isWifi(): Boolean
    fun isGameInstalled(): Boolean
    fun installGame(installPath: String): Boolean
    fun getGameVersion(): String?
    fun getApkVersion(apkName: String): String?
    fun compareVersion(localVersion: String, apkVersion: String): VersionCompareResult
}
class YuanShen : GameManager {

    override fun isWifi(): Boolean {
        return executeCommandWithSELinux("ip route | grep default | grep wlan0") != null
    }

    override fun isGameInstalled(): Boolean {
        val packageName: String = executeCommandWithSELinux("pm list packages com.miHoYo.Yuanshen")
        return packageName.contains("YuanShen", ignoreCase = true)
    }

    override fun installGame(installPath: String): Boolean {
        val installationStatus = executeCommandWithSELinux("pm install -r $installPath")
        return installationStatus.contains("Success", ignoreCase = true)
    }

    override fun getGameVersion(): String? {
        return executeCommandWithSELinux("dumpsys package com.miHoYo.Yuanshen | grep versionName")
            ?.substringBefore("_")
    }

    override fun getApkVersion(apkName: String): String? {
        return apkName
            .substringAfter("_")
            .substringBefore(".apk")
            .takeIf { it.isNotEmpty() }
    }

    override fun compareVersion(localVersion: String, apkVersion: String): VersionCompareResult {
        val local = localVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val apk = apkVersion.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(local.size, apk.size)

        for (i in 0 until maxLength) {
            val localPart = local.getOrNull(i) ?: 0
            val apkPart = apk.getOrNull(i) ?: 0

            when {
                apkPart > localPart -> return VersionCompareResult.HIGHER
                apkPart < localPart -> return VersionCompareResult.LOWER
            }
        }

        return VersionCompareResult.EQUAL
    }
}

@OptIn(ExperimentalForeignApi::class)
fun getGameVersionStatic(): String? {
    return executeCommandWithSELinux("dumpsys package com.miHoYo.Yuanshen | grep versionName")
        ?.substringBefore("_")
}

@OptIn(ExperimentalForeignApi::class)
fun getApkVersionStatic(apkName: String): String? {
    return apkName
        .substringAfter("_")
        .substringBefore(".apk")
        .takeIf { it.isNotEmpty() }
}

@OptIn(ExperimentalForeignApi::class)
fun isGameInstalledStatic(): Boolean {
    val packageName: String = executeCommandWithSELinux("pm list packages com.miHoYo.Yuanshen")
    return packageName.contains("YuanShen", ignoreCase = true)
}

@OptIn(ExperimentalForeignApi::class)
fun createYuanShen(): YuanShen {
    return YuanShen()
}

@OptIn(ExperimentalForeignApi::class)
fun checkGameInstalled(manager: YuanShen): Boolean {
    return manager.isGameInstalled()
}

@OptIn(ExperimentalForeignApi::class)
fun installGame(manager: YuanShen, installPath: String): Boolean {
    return manager.installGame(installPath)
}

@OptIn(ExperimentalForeignApi::class)
fun getGameVersion(manager: YuanShen): String? {
    return manager.getGameVersion()
}

@OptIn(ExperimentalForeignApi::class)
fun getApkVersion(manager: YuanShen, apkName: String): String? {
    return manager.getApkVersion(apkName)
}

@OptIn(ExperimentalForeignApi::class)
fun compareVersion(manager: YuanShen, localVersion: String, apkVersion: String): Int {
    return when (manager.compareVersion(localVersion, apkVersion)) {
        VersionCompareResult.LOWER -> 0
        VersionCompareResult.EQUAL -> 1
        VersionCompareResult.HIGHER -> 2
    }
}