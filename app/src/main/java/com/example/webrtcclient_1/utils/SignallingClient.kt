package com.example.webrtcclient_1.utils

import android.annotation.SuppressLint
import android.util.Log
import com.example.webrtcclient_1.ui.MainActivity
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.net.URISyntaxException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object SignallingClient {

    private const val TAG = "SignallingClient"
    private const val EVENT_LISTEN = "Event"

    var roomName: String = "hello"
    private lateinit var socket: Socket
    var isChannelReady = false
    var isInitiator = false
    var isStarted = false
    private var callback: SignalingInterface? = null
    var context: MainActivity? = null

    //This piece of code should not go into production!!
    //This will help in cases where the node server is running in non-https server and you want to ignore the warnings
    @SuppressLint("TrustAllX509TrustManager")
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

        @SuppressLint("TrustAllX509TrustManager")
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}

        @SuppressLint("TrustAllX509TrustManager")
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
    })

    fun init(signalingInterface: SignalingInterface?, roomName: String, context: MainActivity?) {
        this.roomName = roomName
        this.callback = signalingInterface
        this.context = context
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, null)
            IO.setDefaultHostnameVerifier { hostname: String?, session: SSLSession? -> true }
            IO.setDefaultSSLContext(sslContext)

            //connecting to signalling server using socket
            connectToSignallingServer()
            SocketListener.initSocketListener(socket, callback)
            setSocketListeners()

        } catch (e: URISyntaxException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun setSocketListeners() {
        SocketListener.listenRoomCreatedEvent()
        SocketListener.listenRoomIsFullEvent()
        SocketListener.listenRoomJoinedEvent()
        SocketListener.listenPeerJoinedEvent()
        SocketListener.listenLogReceivedEvent()
        SocketListener.listenPeerDisconnectEvent()
        SocketListener.listenMessageReceivedEvent()
        SocketListener.listenRoomJoined()
    }
    
    /**
     * Connecting to signalling server using socket
     */
    private fun connectToSignallingServer() {
        //set the signalling server url here
        socket = IO.socket("http://frozen-wildwood-51036.herokuapp.com/")
        socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        socket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        socket.on(Socket.EVENT_CONNECT, onConnect);
        socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        socket.connect()
        Log.d(TAG, "init() called")
        if (roomName.isNotEmpty()) {
            emitInitStatement(roomName)
        }
    }

    private var onConnect = Emitter.Listener { Log.d(TAG, "Socket Connected!") }

    private val onConnectError = Emitter.Listener {
        context?.runOnUiThread {
            Log.d(TAG, "Socket Connection Error: $it")
        }
    }
    private val onDisconnect = Emitter.Listener {
        context?.runOnUiThread {
            Log.d(TAG, "Socket Disconnected!")
        }
    }

    /**
     * Emitting create or join room event to signalling server
     */
    private fun emitInitStatement(message: String) {
        Log.d(TAG, "emitting event = [create or join], message = [$message]")
        socket.emit("create or join", message)
    }

    fun emitMessage(message: String) {
        Log.d(TAG, "emitting message = [$message]")
        socket.emit("message", message)
    }

    /**
     * emitting offer to signalling server
     * signalling server will send offer to remote peer
     */
    fun emitOffer(message: SessionDescription) {
        try {
            Log.d(TAG, "emitting message = [$message]")
            val obj = JSONObject()
            obj.put("type", message.type.canonicalForm())
            obj.put("sdp", message.description)
            Log.d(TAG, "emitting offer: $obj")
            socket.emit("message", obj)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun emitAnswer(message: SessionDescription) {
        try {
            Log.d(TAG, "emitting message = [$message]")
            val obj = JSONObject()
            obj.put("type", message.type.canonicalForm())
            obj.put("sdp", message.description)
            Log.d(TAG, "emitting answer: $obj")
            socket.emit("message", obj)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    /**
     * emitting iceCandidate to signalling server
     * signalling server will send this candidate info to remote peer
     */
    fun emitIceCandidate(iceCandidate: IceCandidate) {
        try {
            val obj = JSONObject()
            obj.put("type", "candidate")
            obj.put("label", iceCandidate.sdpMLineIndex)
            obj.put("id", iceCandidate.sdpMid)
            obj.put("candidate", iceCandidate.sdp)
            Log.d(TAG, "emitting iceCandidate: $obj")
            socket.emit("message", obj)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        Log.d(TAG, "emitting disconnect event")
        socket.emit("bye", roomName)
        socket.disconnect()
        socket.close()
    }

    interface SignalingInterface {
        fun onRemoteHangUp(msg: String?)
        fun onAnswerReceived(data: JSONObject?)
        fun onIceCandidateReceived(data: JSONObject?)
        fun initiate()
        fun onCreatedRoom(roomName: String?)
        fun onNewPeerJoined()
        fun onJoinedRoom()
        fun onOfferReceived(data: JSONObject?)
    }
}