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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

object VpnUtils {

    private const val VPN_PERMISSION_REQUEST_CODE = 1001

    fun checkVpnPermission(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    fun getVpnPermissionIntent(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    fun registerVpnPermissionLauncher(
        activity: ComponentActivity,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }

    fun requestVpnPermissionLegacy(activity: Activity) {
        val vpnIntent = VpnService.prepare(activity)
        if (vpnIntent != null) {
            activity.startActivityForResult(vpnIntent, VPN_PERMISSION_REQUEST_CODE)
        }
    }

    fun handleVpnPermissionResult(
        requestCode: Int,
        resultCode: Int,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        if (requestCode == VPN_PERMISSION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }
}