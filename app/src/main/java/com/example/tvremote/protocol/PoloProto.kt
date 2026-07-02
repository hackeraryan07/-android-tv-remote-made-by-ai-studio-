package com.example.tvremote.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class OuterMessage(
    @ProtoNumber(1) val protocolVersion: Int? = null,
    @ProtoNumber(2) val status: Int? = null,
    @ProtoNumber(10) val pairingRequest: PairingRequest? = null,
    @ProtoNumber(11) val pairingRequestAck: PairingRequestAck? = null,
    @ProtoNumber(20) val options: Options? = null,
    @ProtoNumber(30) val configuration: Configuration? = null,
    @ProtoNumber(31) val configurationAck: ConfigurationAck? = null,
    @ProtoNumber(40) val secret: Secret? = null,
    @ProtoNumber(41) val secretAck: SecretAck? = null
) {
    companion object {
        const val STATUS_OK = 200
        const val STATUS_ERROR = 400
        const val STATUS_BAD_CONFIGURATION = 401
        const val STATUS_BAD_SECRET = 402
    }
}

@Serializable
data class PairingRequest(
    @ProtoNumber(1) val serviceName: String,
    @ProtoNumber(2) val clientName: String? = null
)

@Serializable
data class PairingRequestAck(
    @ProtoNumber(1) val serverName: String? = null
)

@Serializable
data class Options(
    @ProtoNumber(1) val inputEncodings: List<Encoding> = emptyList(),
    @ProtoNumber(2) val outputEncodings: List<Encoding> = emptyList(),
    @ProtoNumber(3) val preferredRole: Int? = null // RoleType
) {
    @Serializable
    data class Encoding(
        @ProtoNumber(1) val type: Int, // EncodingType
        @ProtoNumber(2) val symbolLength: Int
    )

    companion object {
        const val ENCODING_TYPE_UNKNOWN = 0
        const val ENCODING_TYPE_ALPHANUMERIC = 1
        const val ENCODING_TYPE_NUMERIC = 2
        const val ENCODING_TYPE_HEXADECIMAL = 3
        const val ENCODING_TYPE_QRCODE = 4

        const val ROLE_TYPE_UNKNOWN = 0
        const val ROLE_TYPE_INPUT = 1
        const val ROLE_TYPE_OUTPUT = 2
    }
}

@Serializable
data class Configuration(
    @ProtoNumber(1) val encoding: Options.Encoding,
    @ProtoNumber(2) val clientRole: Int
)

@Serializable
class ConfigurationAck

@Serializable
data class Secret(
    @ProtoNumber(1) val secret: ByteArray
)

@Serializable
data class SecretAck(
    @ProtoNumber(1) val secret: ByteArray
)
