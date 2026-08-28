package calendario.kevshupp.diariokevinali

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class NetworkStatusTracker(context: Context, private val onStatusChange: (Boolean) -> Unit) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun startListening() {
        val cm = connectivityManager ?: run {
            onStatusChange(false)
            return
        }

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onStatusChange(true)
            }

            override fun onLost(network: Network) {
                onStatusChange(isNetworkAvailable())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                onStatusChange(hasInternetCapability(networkCapabilities))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, networkCallback!!)
        onStatusChange(isNetworkAvailable())
    }

    fun stopListening() {
        try {
            networkCallback?.let {
                connectivityManager?.unregisterNetworkCallback(it)
            }
        } catch (e: Exception) {
            // Callback might not be registered
        } finally {
            networkCallback = null
        }
    }

    fun isNetworkAvailable(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork)
        return hasInternetCapability(caps)
    }

    private fun hasInternetCapability(caps: NetworkCapabilities?): Boolean {
        return (caps != null &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
    }
}
