package plus.yumeyuka.yumebox.core.util

import java.net.NetworkInterface
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NetworkInterfaces {
    suspend fun getLocalIpAddress(): String? = withContext(Dispatchers.IO) {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                if (networkInterface.isUp && !networkInterface.isLoopback) {
                    val addresses = networkInterface.inetAddresses
                    for (address in addresses) {
                        if (!address.isLoopbackAddress && address is InetAddress) {
                            val hostAddress = address.hostAddress
                            if (hostAddress?.contains(':') == false) {
                                return@withContext hostAddress
                            }
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}