package com.example.tvremote.connection

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class DelimitedProtobufSocket<T, R>(
    private val socketFactory: SSLSocketFactory,
    private val host: String,
    private val port: Int,
    private val encode: (T) -> ByteArray,
    private val decode: (ByteArray) -> R
) {
    private var socket: SSLSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    fun connect() {
        socket = socketFactory.createSocket() as SSLSocket
        socket?.connect(InetSocketAddress(host, port), 5000)
        socket?.startHandshake()
        input = socket?.inputStream
        output = socket?.outputStream
    }

    fun getServerCertificate(): X509Certificate? {
        return try {
            socket?.session?.peerCertificates?.firstOrNull() as? X509Certificate
        } catch (e: SSLPeerUnverifiedException) {
            null
        }
    }

    fun sendMessage(msg: T) {
        val bytes = encode(msg)
        val out = output ?: throw IllegalStateException("Not connected")
        writeVarint(out, bytes.size)
        out.write(bytes)
        out.flush()
    }

    fun readMessage(): R {
        val inp = input ?: throw IllegalStateException("Not connected")
        val size = readVarint(inp)
        val bytes = ByteArray(size)
        var readCount = 0
        while (readCount < size) {
            val count = inp.read(bytes, readCount, size - readCount)
            if (count == -1) throw EOFException()
            readCount += count
        }
        return decode(bytes)
    }

    fun close() {
        try { socket?.close() } catch (e: Exception) {}
        socket = null
        input = null
        output = null
    }

    private fun writeVarint(out: OutputStream, value: Int) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                out.write(v)
                return
            } else {
                out.write((v and 0x7F) or 0x80)
                v = v ushr 7
            }
        }
    }

    private fun readVarint(input: InputStream): Int {
        var result = 0
        var shift = 0
        while (shift < 32) {
            val b = input.read()
            if (b == -1) throw EOFException()
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        throw Exception("Malformed varint")
    }
}
