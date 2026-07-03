package com.example.tvremote.connection

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

class TlsManager(private val context: Context) {
    private val keyAlias = "AndroidTvRemoteKey"
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (!keyStore.containsAlias(keyAlias)) {
            generateKey()
        }
    }

    private fun generateKey() {
        if (keyStore.containsAlias(keyAlias)) {
            keyStore.deleteEntry(keyAlias)
        }

        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        val parameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setCertificateSubject(X500Principal("CN=Android TV Remote"))
            .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setKeySize(2048)
            .build()
            
        kpg.initialize(parameterSpec)
        kpg.generateKeyPair()
    }

    fun getClientCertificate(): X509Certificate {
        if (!keyStore.containsAlias(keyAlias)) {
            generateKey()
        }
        return keyStore.getCertificate(keyAlias) as X509Certificate
    }

    fun getSslSocketFactory(): SSLSocketFactory {
        if (!keyStore.containsAlias(keyAlias)) {
            generateKey()
        }

        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, null)

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
        sslContext.init(arrayOf(customKeyManager), trustAllCerts, java.security.SecureRandom())
        return sslContext.socketFactory
    }
}
