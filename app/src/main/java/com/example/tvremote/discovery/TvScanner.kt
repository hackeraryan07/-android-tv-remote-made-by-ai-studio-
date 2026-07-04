package com.example.tvremote.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.util.concurrent.ConcurrentLinkedQueue

data class DiscoveredTv(
    val name: String,
    val address: InetAddress,
    val port: Int
)

class TvScanner(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    
    private val resolveQueue = ConcurrentLinkedQueue<NsdServiceInfo>()
    private var isResolving = false

    private fun resolveNext() {
        if (isResolving) return
        val nextService = resolveQueue.poll()
        if (nextService != null) {
            isResolving = true
            try {
                nsdManager.resolveService(nextService, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e("TvScanner", "Resolve failed: $errorCode")
                        isResolving = false
                        resolveNext()
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        Log.d("TvScanner", "Resolve Succeeded: ${serviceInfo.serviceName} ${serviceInfo.host}")
                        val tv = DiscoveredTv(
                            name = serviceInfo.serviceName,
                            address = serviceInfo.host,
                            port = serviceInfo.port
                        )
                        val currentList = _discoveredTvs.value.toMutableList()
                        if (currentList.none { it.address == tv.address }) {
                            currentList.add(tv)
                            _discoveredTvs.value = currentList
                        }
                        isResolving = false
                        resolveNext()
                    }
                })
            } catch (e: Exception) {
                Log.e("TvScanner", "Resolve exception", e)
                isResolving = false
                resolveNext()
            }
        }
    }

    fun startScanning() {
        if (discoveryListener != null) return
        
        _discoveredTvs.value = emptyList()
        resolveQueue.clear()
        isResolving = false

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d("TvScanner", "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d("TvScanner", "Service found: ${service.serviceName}")
                resolveQueue.add(service)
                resolveNext()
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e("TvScanner", "Service lost: $service")
                val currentList = _discoveredTvs.value.toMutableList()
                currentList.removeAll { it.name == service.serviceName }
                _discoveredTvs.value = currentList
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i("TvScanner", "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("TvScanner", "Discovery failed: Error code: $errorCode")
                stopScanning()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("TvScanner", "Discovery failed: Error code: $errorCode")
                stopScanning()
            }
        }

        try {
            nsdManager.discoverServices(
                "_androidtvremote2._tcp.",
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            Log.e("TvScanner", "Failed to start discovery", e)
        }
    }

    fun stopScanning() {
        try {
            discoveryListener?.let {
                nsdManager.stopServiceDiscovery(it)
            }
        } catch (e: Exception) {
            Log.e("TvScanner", "Failed to stop discovery", e)
        } finally {
            discoveryListener = null
            resolveQueue.clear()
            isResolving = false
        }
    }
}
