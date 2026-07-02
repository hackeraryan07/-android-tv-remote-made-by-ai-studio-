package com.example.tvremote.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class RemoteMessage(
    @ProtoNumber(1) val remoteConfigure: RemoteConfigure? = null,
    @ProtoNumber(2) val remoteSetActive: RemoteSetActive? = null,
    @ProtoNumber(8) val remotePingRequest: RemotePingRequest? = null,
    @ProtoNumber(9) val remotePingResponse: RemotePingResponse? = null,
    @ProtoNumber(10) val remoteKeyInject: RemoteKeyInject? = null,
    // Add other fields as needed
)

@Serializable
data class RemoteConfigure(
    @ProtoNumber(1) val code1: Int = 622, // Often 622 in clients
    @ProtoNumber(2) val deviceInfo: RemoteDeviceInfo? = null
)

@Serializable
data class RemoteDeviceInfo(
    @ProtoNumber(1) val model: String,
    @ProtoNumber(2) val vendor: String,
    @ProtoNumber(3) val unknown1: Int = 1,
    @ProtoNumber(4) val unknown2: String = "1",
    @ProtoNumber(5) val packageName: String,
    @ProtoNumber(6) val appVersion: String
)

@Serializable
data class RemotePingRequest(
    @ProtoNumber(1) val val1: Int,
    @ProtoNumber(2) val val2: Int? = null
)

@Serializable
data class RemotePingResponse(
    @ProtoNumber(1) val val1: Int
)

@Serializable
data class RemoteSetActive(
    @ProtoNumber(1) val active: Int
)

@Serializable
data class RemoteKeyInject(
    @ProtoNumber(1) val keyCode: Int,
    @ProtoNumber(2) val direction: Int // RemoteDirection
) {
    companion object {
        const val UNKNOWN_DIRECTION = 0
        const val START_LONG = 1
        const val END_LONG = 2
        const val SHORT = 3
    }
}

object RemoteKeyCode {
    const val KEYCODE_HOME = 3
    const val KEYCODE_BACK = 4
    const val KEYCODE_DPAD_UP = 19
    const val KEYCODE_DPAD_DOWN = 20
    const val KEYCODE_DPAD_LEFT = 21
    const val KEYCODE_DPAD_RIGHT = 22
    const val KEYCODE_DPAD_CENTER = 23
    const val KEYCODE_VOLUME_UP = 24
    const val KEYCODE_VOLUME_DOWN = 25
    const val KEYCODE_POWER = 26
    const val KEYCODE_MUTE = 91
    const val KEYCODE_WAKEUP = 224
    const val KEYCODE_SOFT_SLEEP = 276
}
