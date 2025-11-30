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

package plus.yumeyuka.yumebox.substore

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import plus.yumeyuka.yumebox.common.native.NativeLibraryManager

class SubStoreService : Service() {

    companion object {
        var caseEngine: CaseEngine? = null
        var isRunning: Boolean = false
            private set

        fun startService(frontendPort: Int = 8080, backendPort: Int = 8081, allowLan: Boolean = false) {
            val context = plus.yumeyuka.yumebox.App.instance
            val intent = Intent(context, SubStoreService::class.java).apply {
                putExtra("frontendPort", frontendPort)
                putExtra("backendPort", backendPort)
                putExtra("allowLan", allowLan)
            }
            context.startService(intent)
        }

        fun stopService() {
            val context = plus.yumeyuka.yumebox.App.instance
            val intent = Intent(context, SubStoreService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val frontendPort = intent?.getIntExtra("frontendPort", 8080) ?: 8080
            val backendPort = intent?.getIntExtra("backendPort", 8081) ?: 8081
            val allowLan = intent?.getBooleanExtra("allowLan", false) ?: false

            if (NetworkUtil.isPortInUse(frontendPort) || NetworkUtil.isPortInUse(backendPort)) {
                throw Exception("端口 $frontendPort 或 $backendPort 已被占用")
            }

            if (!ensureJavetLibraryLoaded()) {
                throw Exception("Javet native 库加载失败")
            }

            val engine = CaseEngine(
                backendPort = backendPort,
                frontendPort = frontendPort,
                allowLan = allowLan
            )
            caseEngine = engine

            if (!engine.isInitialized()) {
                throw Exception("CaseEngine 初始化失败")
            }

            engine.startServer()
            isRunning = true

            return START_STICKY
        } catch (e: Exception) {
            e.printStackTrace()
            isRunning = false
            return START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            caseEngine?.stopServer()
            caseEngine = null
            isRunning = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ensureJavetLibraryLoaded(): Boolean {
        return try {
            NativeLibraryManager.initialize(applicationContext)
            val javetLibName = "libjavet-node-android.v.5.0.1.so"

            if (NativeLibraryManager.isLibraryAvailable(javetLibName)) {
                Log.d("SubStoreService", "Javet 库已存在，准备加载")
            } else {
                Log.d("SubStoreService", "Javet 库不存在，开始提取")
                val results = NativeLibraryManager.extractAllLibraries()
                Log.d("SubStoreService", "库提取结果: $results")
                if (results[javetLibName] != true) {
                    Log.e("SubStoreService", "Javet 库提取失败")
                    return false
                }
            }

            val loaded = NativeLibraryManager.loadJniLibrary(javetLibName)
            if (loaded) {
                Log.d("SubStoreService", "Javet 库加载成功")
            } else {
                Log.e("SubStoreService", "Javet 库加载失败")
                Log.e("SubStoreService", "库状态: ${NativeLibraryManager.getLibraryStatus(javetLibName)}")
            }
            loaded
        } catch (e: Exception) {
            Log.e("SubStoreService", "Javet 库加载异常", e)
            e.printStackTrace()
            false
        }
    }
}
