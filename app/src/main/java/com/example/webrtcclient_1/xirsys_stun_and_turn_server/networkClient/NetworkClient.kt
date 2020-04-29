package com.example.webrtcclient_1.xirsys_stun_and_turn_server.networkClient

import com.example.webrtcclient_1.xirsys_stun_and_turn_server.networkInterface.XirsysInterface
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient {

    private const val API_ENDPOINT = "https://global.xirsys.net"
    private var retrofitInstance: Retrofit? = null

    fun getRetrofitInstance(): XirsysInterface {
        if (retrofitInstance == null) {
            retrofitInstance = Retrofit.Builder()
                .baseUrl(API_ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitInstance!!.create(XirsysInterface::class.java)
    }
}