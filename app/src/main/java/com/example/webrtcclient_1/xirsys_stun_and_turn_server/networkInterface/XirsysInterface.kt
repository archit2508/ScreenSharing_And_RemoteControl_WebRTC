package com.example.webrtcclient_1.xirsys_stun_and_turn_server.networkInterface

import com.example.webrtcclient_1.xirsys_stun_and_turn_server.model.XirsysStunTurnResponse
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.PUT

/**
 * this api will bring list of stun and turn servers
 */
interface XirsysInterface {
    @PUT("/_turn/MyFirstApp")
    fun getIceCandidates(@Header("Authorization") authkey: String?): Call<XirsysStunTurnResponse>
}