package com.example.webrtcclient_1.utils

import android.util.Log
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject
import java.util.*

object SocketListener {

    private const val TAG = "SignallingClient"
    private const val EVENT_LISTEN = "Event"

    private lateinit var socket: Socket
    private var callback: SignallingClient.SignalingInterface? = null
    
    fun initSocketListener(socket: Socket, callback: SignallingClient.SignalingInterface?){
        this.socket = socket
        this.callback = callback
    }

    /**
     * server sends SDP and ICE messages to this listener
     * SDP and ICE candidates are transferred through this
     */
    fun listenMessageReceivedEvent() {
        socket.on("message") { args: Array<Any> ->
            Log.d(TAG + EVENT_LISTEN, "message call() called with: args = [" + Arrays.toString(args) + "]")
            if (args[0] is String) {
                processStringMessageReceived(args)
            } else if (args[0] is JSONObject) {
                try {
                    processJSONMessageReceived(args)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun processJSONMessageReceived(args: Array<Any>) {
        val data = args[0] as JSONObject
        Log.d(TAG + EVENT_LISTEN, "Json Received :: $data")
        val type = data.getString("type")
        if (type.equals("offer", ignoreCase = true)) {
            callback?.onOfferReceived(data)
        } else if (type.equals("answer", ignoreCase = true) && SignallingClient.isStarted) {
            callback?.onAnswerReceived(data)
        } else if (type.equals("candidate", ignoreCase = true) && SignallingClient.isStarted) {
            callback!!.onIceCandidateReceived(data)
        }
    }

    private fun processStringMessageReceived(args: Array<Any>) {
        Log.d(TAG + EVENT_LISTEN, "String received :: " + args[0])
        val data = args[0] as String
        //when remote peer disconnects
        if (data.equals("bye", ignoreCase = true)) {
            callback?.onRemoteHangUp(data)
        }
    }

    /**
     * Remote peer disconnected
     */
    fun listenPeerDisconnectEvent() {
        socket.on("bye") { args: Array<Any?> ->
            callback?.onRemoteHangUp(args[0] as String?)
        }
    }

    /**
     * server sending log
     */
    fun listenLogReceivedEvent() {
        socket.on("log") { args: Array<Any?>? ->
            Log.d(TAG + EVENT_LISTEN, "log call() called with: args = [" + Arrays.toString(args) + "]")
        }
    }

    /**
     * server says we have joined room
     */
    fun listenPeerJoinedEvent() {
        socket.on("joined", Emitter.Listener { args: Array<Any?>? ->
            Log.d(TAG + EVENT_LISTEN, "joined call() called with: args = [" + Arrays.toString(args) + "]")
            SignallingClient.isChannelReady = true
        })
    }

    /**
     * Remote peer joined
     */
    fun listenRoomJoinedEvent() {
        socket.on("join") { args: Array<Any?>? ->
            Log.d(TAG + EVENT_LISTEN, "join call() called with: args = [" + Arrays.toString(args) + "]")
            SignallingClient.isChannelReady = true
            callback?.onNewPeerJoined()
        }
    }

    /**
     * when you joined a chat room successfully
     * required for receiving stream
     */
    fun listenRoomJoined() {
        socket.on("joined") { args: Array<Any?>? ->
            Log.d("SignallingClient", "joined call() called with: args = [" + Arrays.toString(args).toString() + "]")
//            SignallingClient.isChannelReady = true
            callback?.onJoinedRoom()
        }
    }

    /**
     * server says room is full
     */
    fun listenRoomIsFullEvent() {
        socket.on("full") { args: Array<Any?>? ->
            Log.d(TAG + EVENT_LISTEN, "full call() called with: args = [" + Arrays.toString(args) + "]")
        }
    }

    /**
     * server says room has been created
     */
    fun listenRoomCreatedEvent() {
        socket.on("created", Emitter.Listener { args: Array<Any?>? ->
            Log.d(TAG + EVENT_LISTEN, "room creation call() called with: args = [" + Arrays.toString(args) + "]")
            SignallingClient.isInitiator = true
            callback?.onCreatedRoom(SignallingClient.roomName)
        })
    }
}