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

package plus.yumeyuka.yumebox.data.store

import com.tencent.mmkv.MMKV
import plus.yumeyuka.yumebox.data.store.MMKVPreference
import plus.yumeyuka.yumebox.data.model.AccessControlMode
import plus.yumeyuka.yumebox.data.model.ProxyMode
import plus.yumeyuka.yumebox.data.model.TunStack

class NetworkSettingsStorage(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {


    val proxyMode by enumFlow(ProxyMode.Tun)


    val bypassPrivateNetwork by boolFlow(true)
    val dnsHijack by boolFlow(true)
    val allowBypass by boolFlow(true)
    val enableIPv6 by boolFlow(false)
    val systemProxy by boolFlow(true )


    val tunStack by enumFlow(TunStack.System)
    val accessControlMode by enumFlow(AccessControlMode.allow_all)
    val accessControlPackages by stringSetFlow(emptySet())
}
