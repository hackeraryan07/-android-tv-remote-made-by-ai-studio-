package com.example.tvremote.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tvremote.connection.CommandClient
import com.example.tvremote.connection.PairingClient
import com.example.tvremote.connection.TlsManager
import com.example.tvremote.discovery.DiscoveredTv
import com.example.tvremote.discovery.TvScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RemoteState {
    object Scanning : RemoteState()
    data class FoundTvs(val tvs: List<DiscoveredTv>) : RemoteState()
    data class Pairing(val tv: DiscoveredTv) : RemoteState()
    data class AwaitingCode(val tv: DiscoveredTv) : RemoteState()
    data class Connected(val tvName: String) : RemoteState()
    data class Error(val message: String) : RemoteState()
}

class TvRemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val tvScanner = TvScanner(application)
    private val tlsManager = TlsManager(application)
    
    private val _state = MutableStateFlow<RemoteState>(RemoteState.Scanning)
    val state: StateFlow<RemoteState> = _state.asStateFlow()

    private var pairingClient: PairingClient? = null
    private var commandClient: CommandClient? = null
    private var currentTv: DiscoveredTv? = null

    private val prefs = application.getSharedPreferences("tv_remote_prefs", Context.MODE_PRIVATE)

    init {
        viewModelScope.launch {
            tlsManager.initialize()
        }
        startScanning()
    }

    fun startScanning() {
        _state.value = RemoteState.Scanning
        tvScanner.startScanning()
        
        viewModelScope.launch {
            tvScanner.discoveredTvs.collect { tvs ->
                if (_state.value is RemoteState.Scanning || _state.value is RemoteState.FoundTvs) {
                    _state.value = RemoteState.FoundTvs(tvs)
                    
                    val pairedTvName = prefs.getString("paired_tv_name", null)
                    if (pairedTvName != null) {
                        val knownTv = tvs.find { it.name == pairedTvName }
                        if (knownTv != null) {
                            tvScanner.stopScanning()
                            connectToTv(knownTv.address.hostAddress ?: "", knownTv.name)
                        }
                    }
                }
            }
        }
    }

    fun connectManually(ip: String) {
        tvScanner.stopScanning()
        connectToTv(ip, "Manual TV")
    }

    fun initiatePairing(tv: DiscoveredTv) {
        tvScanner.stopScanning()
        currentTv = tv
        _state.value = RemoteState.Pairing(tv)
        
        viewModelScope.launch {
            try {
                pairingClient = PairingClient(tv.address.hostAddress ?: "", tlsManager)
                pairingClient?.initiatePairing()
                _state.value = RemoteState.AwaitingCode(tv)
            } catch (e: Exception) {
                android.util.Log.e("TvRemoteViewModel", "Pairing failed", e)
                _state.value = RemoteState.Error("Pairing failed: ${e.javaClass.simpleName} - ${e.message}")
            }
        }
    }

    fun submitPairingCode(code: String) {
        val tv = currentTv ?: return
        viewModelScope.launch {
            try {
                _state.value = RemoteState.Pairing(tv)
                pairingClient?.provideCode(code)
                
                // Save paired TV
                prefs.edit().putString("paired_tv_name", tv.name).apply()
                
                // Now connect to command port
                connectToTv(tv.address.hostAddress ?: "", tv.name)
            } catch (e: Exception) {
                _state.value = RemoteState.Error("Wrong code or pairing failed: ${e.message}")
            } finally {
                pairingClient?.close()
                pairingClient = null
            }
        }
    }

    private fun connectToTv(ip: String, tvName: String) {
        viewModelScope.launch {
            try {
                commandClient?.close()
                commandClient = CommandClient(ip, tlsManager) {
                    _state.value = RemoteState.Error("Connection lost")
                }
                commandClient?.connect()
                _state.value = RemoteState.Connected(tvName)
            } catch (e: Exception) {
                _state.value = RemoteState.Error("Could not connect to TV: ${e.message}")
            }
        }
    }

    fun sendCommand(keyCode: Int) {
        commandClient?.sendKey(keyCode)
    }

    override fun onCleared() {
        super.onCleared()
        tvScanner.stopScanning()
        pairingClient?.close()
        commandClient?.close()
    }
}
