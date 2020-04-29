package com.example.webrtcclient_1.xirsys_stun_and_turn_server.model

import com.google.gson.annotations.SerializedName

data class XirsysStunTurnResponse (
    var s: String?,
    var p: String?,
    var e: Any?,
    @SerializedName("v") var iceServerList: IceServerList?
)