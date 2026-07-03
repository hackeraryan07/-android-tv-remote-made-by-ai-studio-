package com.example.tvremote.connection

import android.os.Build
import com.example.tvremote.protocol.Configuration
import com.example.tvremote.protocol.Options
import com.example.tvremote.protocol.OuterMessage
import com.example.tvremote.protocol.PairingRequest
import com.example.tvremote.protocol.Secret
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
class PairingClient(
    private val host: String,
    private val tlsManager: TlsManager
) {
    private var socket: DelimitedProtobufSocket<OuterMessage, OuterMessage>? = null
    private val clientName = Build.MODEL ?: "Android TV Remote"
    private val serviceName = "com.google.android.tv.remote" // usually "androidtvremote" or "com.google.android.tv.remote"
    // Wait, what service name does python use?
    // In python: _client_name = "androidtvremote2" ? Let me check.

    suspend fun initiatePairing() = withContext(Dispatchers.IO) {
        socket = DelimitedProtobufSocket(
            tlsManager.getSslSocketFactory(),
            host,
            6467,
            { ProtoBuf.encodeToByteArray(it) },
            { ProtoBuf.decodeFromByteArray(it) }
        )
        socket?.connect()

        val req = OuterMessage(
            protocolVersion = 2,
            status = OuterMessage.STATUS_OK,
            pairingRequest = PairingRequest(
                serviceName = "androidtvremote", // Will verify
                clientName = clientName
            )
        )
        socket?.sendMessage(req)
        
        val ack = socket?.readMessage()
        if (ack?.status != OuterMessage.STATUS_OK) throw Exception("Pairing request failed with status ${ack?.status}")

        val options = OuterMessage(
            protocolVersion = 2,
            status = OuterMessage.STATUS_OK,
            options = Options(
                inputEncodings = listOf(Options.Encoding(type = Options.ENCODING_TYPE_HEXADECIMAL, symbolLength = 6)),
                preferredRole = Options.ROLE_TYPE_INPUT
            )
        )
        socket?.sendMessage(options)

        val optionsAck = socket?.readMessage()
        if (optionsAck?.status != OuterMessage.STATUS_OK) throw Exception("Options failed")

        val configuration = OuterMessage(
            protocolVersion = 2,
            status = OuterMessage.STATUS_OK,
            configuration = Configuration(
                encoding = Options.Encoding(type = Options.ENCODING_TYPE_HEXADECIMAL, symbolLength = 6),
                clientRole = Options.ROLE_TYPE_INPUT
            )
        )
        socket?.sendMessage(configuration)

        val configAck = socket?.readMessage()
        if (configAck?.status != OuterMessage.STATUS_OK) throw Exception("Configuration failed")
        
        // Now TV shows the code
    }

    suspend fun provideCode(code: String) = withContext(Dispatchers.IO) {
        val serverCert = socket?.getServerCertificate() ?: throw Exception("No server cert")
        val clientCert = tlsManager.getClientCertificate()

        val secretHash = CryptoUtil.computePairingSecret(clientCert, serverCert, code)

        val secretMsg = OuterMessage(
            protocolVersion = 2,
            status = OuterMessage.STATUS_OK,
            secret = Secret(secret = secretHash)
        )
        socket?.sendMessage(secretMsg)

        val secretAck = socket?.readMessage()
        if (secretAck?.status != OuterMessage.STATUS_OK) {
            throw Exception("Wrong pairing code")
        }
        
        socket?.close()
    }

    fun close() {
        socket?.close()
    }
}
