package com.example.tvremote.connection

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.net.Socket
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLEngine

class TlsManager(private val context: Context) {
    private val keyAlias = "AndroidTvRemoteKey"
    private val keyPassword = "password".toCharArray()
    private val keyStoreFile by lazy { File(context.filesDir, "remote_keystore.p12") }
    private val keyStore: KeyStore

    init {
        Security.addProvider(BouncyCastleProvider())
        keyStore = KeyStore.getInstance("PKCS12")
        
        if (keyStoreFile.exists()) {
            FileInputStream(keyStoreFile).use { fis ->
                keyStore.load(fis, keyPassword)
            }
        } else {
            keyStore.load(null, null)
            generateKey()
        }
    }

    private fun generateKey() {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
        keyPairGenerator.initialize(2048, SecureRandom())
        val keyPair = keyPairGenerator.generateKeyPair()

        val cert = generateSelfSignedCertificate(keyPair)

        keyStore.setKeyEntry(keyAlias, keyPair.private, keyPassword, arrayOf(cert))
        
        FileOutputStream(keyStoreFile).use { fos ->
            keyStore.store(fos, keyPassword)
        }
    }

    private fun generateSelfSignedCertificate(keyPair: KeyPair): X509Certificate {
        val owner = X500Name("CN=Android TV Remote")
        val serial = BigInteger(64, SecureRandom())
        val notBefore = Date()
        val notAfter = Date(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000) // 10 years

        val builder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
            owner, serial, notBefore, notAfter, owner, keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keyPair.private)

        val certHolder = builder.build(signer)
        return JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(certHolder)
    }

    fun getClientCertificate(): X509Certificate {
        return keyStore.getCertificate(keyAlias) as X509Certificate
    }

    fun getSslSocketFactory(): SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, keyPassword)
        
        val defaultKeyManager = keyManagerFactory.keyManagers.first { it is X509ExtendedKeyManager } as X509ExtendedKeyManager
        val customKeyManager = object : X509ExtendedKeyManager() {
            override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?): String {
                return keyAlias
            }

            override fun chooseEngineClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?): String {
                return keyAlias
            }

            override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? {
                return defaultKeyManager.chooseServerAlias(keyType, issuers, socket)
            }

            override fun chooseEngineServerAlias(keyType: String?, issuers: Array<out Principal>?, engine: SSLEngine?): String? {
                return defaultKeyManager.chooseEngineServerAlias(keyType, issuers, engine)
            }

            override fun getCertificateChain(alias: String?): Array<X509Certificate> {
                return defaultKeyManager.getCertificateChain(alias) ?: arrayOf(getClientCertificate())
            }

            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> {
                return arrayOf(keyAlias)
            }

            override fun getPrivateKey(alias: String?): PrivateKey {
                return defaultKeyManager.getPrivateKey(alias)
            }

            override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? {
                return defaultKeyManager.getServerAliases(keyType, issuers)
            }
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(arrayOf(customKeyManager), trustAllCerts, SecureRandom())
        return sslContext.socketFactory
    }
}
