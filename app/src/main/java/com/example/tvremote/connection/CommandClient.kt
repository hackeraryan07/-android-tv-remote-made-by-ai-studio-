package com.example.tvremote.connection

import android.os.Build
import com.example.tvremote.protocol.RemoteConfigure
import com.example.tvremote.protocol.RemoteDeviceInfo
import com.example.tvremote.protocol.RemoteKeyInject
import com.example.tvremote.protocol.RemoteMessage
import com.example.tvremote.protocol.RemotePingResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
class CommandClient(
    private val host: String,
    private val tlsManager: TlsManager,
    private val onConnectionLost: () -> Unit
) {
    private var socket: DelimitedProtobufSocket<RemoteMessage, RemoteMessage>? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var readJob: Job? = null

    suspend fun connect() = kotlinx.coroutines.withContext(Dispatchers.IO) {
        socket = DelimitedProtobufSocket(
            tlsManager.getSslSocketFactory(),
            host,
            6466,
            { ProtoBuf.encodeToByteArray(it) },
            { ProtoBuf.decodeFromByteArray(it) }
        )
        socket?.connect()

        val deviceInfo = RemoteDeviceInfo(
            model = Build.MODEL ?: "Android",
            vendor = Build.MANUFACTURER ?: "Google",
            unknown1 = 1,
            unknown2 = "1",
            packageName = "com.google.android.tv.remote",
            appVersion = "1.0.0"
        )
        
        val configure = RemoteMessage(
            remoteConfigure = RemoteConfigure(
                code1 = 622,
                deviceInfo = deviceInfo
            )
        )
        socket?.sendMessage(configure)

        startReading()
    }

    private fun startReading() {
        readJob = scope.launch {
            try {
                while (isActive) {
                    val msg = socket?.readMessage() ?: break
                    if (msg.remotePingRequest != null) {
                        // Respond to ping
                        val pingResp = RemoteMessage(
                            remotePingResponse = RemotePingResponse(val1 = msg.remotePingRequest.val1)
                        )
                        socket?.sendMessage(pingResp)
                    }
                }
            } catch (e: Exception) {
                onConnectionLost()
            }
        }
    }

    fun sendKey(keyCode: Int, direction: Int = RemoteKeyInject.SHORT) {
        scope.launch {
            try {
                val msg = RemoteMessage(
                    remoteKeyInject = RemoteKeyInject(
                        keyCode = keyCode,
                        direction = direction
                    )
                )
                socket?.sendMessage(msg)
            } catch (e: Exception) {
                onConnectionLost()
            }
        }
    }

    fun close() {
        readJob?.cancel()
        socket?.close()
    }
}
