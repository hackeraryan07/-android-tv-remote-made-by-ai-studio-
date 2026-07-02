package com.example.tvremote.connection

import java.math.BigInteger
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey

object CryptoUtil {
    fun computePairingSecret(
        clientCert: X509Certificate,
        serverCert: X509Certificate,
        code: String
    ): ByteArray {
        val clientModulus = (clientCert.publicKey as RSAPublicKey).modulus
        val clientExponent = (clientCert.publicKey as RSAPublicKey).publicExponent
        val serverModulus = (serverCert.publicKey as RSAPublicKey).modulus
        val serverExponent = (serverCert.publicKey as RSAPublicKey).publicExponent

        val h = MessageDigest.getInstance("SHA-256")
        h.update(getModulusOrExponentBytes(clientModulus))
        h.update(getModulusOrExponentBytes(clientExponent, true))
        h.update(getModulusOrExponentBytes(serverModulus))
        h.update(getModulusOrExponentBytes(serverExponent, true))

        // Code is 6 hex characters. Last 4 characters are used.
        val codeBytes = code.substring(2).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        h.update(codeBytes)

        return h.digest()
    }

    private fun getModulusOrExponentBytes(bigInt: BigInteger, prependZero: Boolean = false): ByteArray {
        var hex = bigInt.toString(16).uppercase()
        if (prependZero) {
            hex = "0\$hex"
        } else if (hex.length % 2 != 0) {
            hex = "0\$hex"
        }
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
