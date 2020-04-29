package com.example.webrtcclient_1.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.webrtcclient_1.R
import com.example.webrtcclient_1.acessibility.AccessibilityHelper.performClick
import com.example.webrtcclient_1.acessibility.AccessibilityHelper.performLongClick
import com.example.webrtcclient_1.acessibility.AccessibilityHelper.performPaste
import com.example.webrtcclient_1.acessibility.AccessibilityHelper.performSwipe
import com.example.webrtcclient_1.acessibility.PaytmAccessibilityService
import com.example.webrtcclient_1.data.model.*
import com.example.webrtcclient_1.utils.ActionUtil
import com.example.webrtcclient_1.utils.SignallingClient
import com.example.webrtcclient_1.utils.extensions.getScreenHeight
import com.example.webrtcclient_1.utils.extensions.getScreenWidth
import com.example.webrtcclient_1.webrtc_custom_observers.CustomPeerConnectionObserver
import com.example.webrtcclient_1.webrtc_custom_observers.CustomSdpObserver
import com.example.webrtcclient_1.xirsys_stun_and_turn_server.model.IceServer
import com.example.webrtcclient_1.xirsys_stun_and_turn_server.model.XirsysStunTurnResponse
import com.example.webrtcclient_1.xirsys_stun_and_turn_server.networkClient.NetworkClient
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnectionFactory.InitializationOptions
import org.webrtc.SurfaceTextureHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.UnsupportedEncodingException
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), SignallingClient.SignalingInterface {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var audioConstraints: MediaConstraints? = null
    private var videoConstraints: MediaConstraints? = null
    private var sdpConstraints: MediaConstraints? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var localPeer: PeerConnection? = null
    private var iceServers: List<IceServer>? = null
    private var rootEglBase: EglBase? = null
    private var gotUserMedia = false
    private var peerIceServers: MutableList<PeerConnection.IceServer> = ArrayList()
    private var localDataChannel: DataChannel? = null
    private var sDeviceWidth = 0
    private var sDeviceHeight = 0
    private var mMediaProjectionPermissionResultData: Intent? = null
    private var mMediaProjectionPermissionResultCode = 0
    private var frameHeight = 0
    private var frameWidth = 0

    companion object {
        private const val SCREEN_RESOLUTION_SCALE = 2
        private const val CAPTURE_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_main)

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        sDeviceWidth = metrics.widthPixels
        sDeviceHeight = metrics.heightPixels

        //Setting screen height and width
        Screen.screenWidth = getScreenWidth()
        Screen.screenHeight = getScreenHeight()

        receive_stream.setOnClickListener {
            start_sharing.isEnabled = false
            SignallingClient.init(this, "hello", this)
            initializePeerConnectionFactoryGlobals()
            createPeerConnectionFactoryInstance()
            initSurfaceView()
        }

        end_call.setOnClickListener { hangup() }
        getIceServers()
    }

    override fun onResume() {
        super.onResume()
        start_sharing.setOnClickListener {
            checkForAccessibility()
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        var accessibilityEnabled = 0
        val accessibilityFound = false
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                this.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
            Log.d("sahil", "ACCESSIBILITY: $accessibilityEnabled")
        } catch (e: Settings.SettingNotFoundException) {
            Log.d("sahil", "Error finding setting, default accessibility to not found: " + e.message)
        }
        val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            Log.d("sahil", "***ACCESSIBILIY IS ENABLED***: ")
            val settingValue = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            Log.d("sahil", "Setting: $settingValue")
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessabilityService = mStringColonSplitter.next()
                    Log.d("sahil", "Setting: $accessabilityService")
                    if (accessabilityService.equals(
                            "com.example.webrtcclient_1/com.example.webrtcclient_1.acessibility.PaytmAccessibilityService",
                            ignoreCase = true
                        )
                    ) {
                        Log.d(
                            "sahil",
                            "We've found the correct setting - accessibility is switched on!"
                        )
                        return true
                    }
                }
            }
            Log.d("sahil", "***END***")
        } else {
            Log.d("sahil", "***ACCESSIBILIY IS DISABLED***")
        }
        return accessibilityFound
    }

    private fun checkForAccessibility() {
        if(isAccessibilityEnabled()) {
            initiateScreenSharingProcess()
        } else {
            startSettingsActivity()
        }
    }

    private fun initiateScreenSharingProcess() {
        startScreenCapture()
        receive_stream.isEnabled = false
    }

    private fun startSettingsActivity() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = (Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        startActivity(intent)
    }

    private fun stringToByteBuffer(msg: String, charset: Charset): ByteBuffer {
        return ByteBuffer.wrap(msg.toByteArray(charset))
    }

    private fun initSurfaceView() {
        remote_gl_surface_view.init(rootEglBase?.eglBaseContext, null)
        remote_gl_surface_view.setZOrderMediaOverlay(true)
    }

    private fun startScreenCapture() {
        val mediaProjectionManager = application.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != Companion.CAPTURE_PERMISSION_REQUEST_CODE) return
        mMediaProjectionPermissionResultCode = resultCode
        mMediaProjectionPermissionResultData = data
        init()
    }

    private fun createScreenCapturer(): VideoCapturer? {
        if (mMediaProjectionPermissionResultCode != Activity.RESULT_OK) {
            report("User didn't give permission to capture the screen.")
            return null
        }
        return ScreenCapturerAndroid(mMediaProjectionPermissionResultData, object : MediaProjection.Callback() {
            override fun onStop() {
                report("User revoked permission to capture the screen.")
            }
        })
    }

    fun report(info: String) {
        Log.e(Companion.TAG, info)
    }

    private fun init() {
        SignallingClient.init(this, "hello", this)

        initializePeerConnectionFactoryGlobals()
        createPeerConnectionFactoryInstance()

        //Now create a VideoCapturer instance.
        val videoCapturerAndroid: VideoCapturer? = createScreenCapturer()
        createMediaConstraints()
        createVideoSource(videoCapturerAndroid)
        createAudioSource()

        videoCapturerAndroid?.startCapture(sDeviceWidth / SCREEN_RESOLUTION_SCALE, sDeviceHeight / SCREEN_RESOLUTION_SCALE, 0)

        gotUserMedia = true
        if (SignallingClient.isInitiator) {
            initiate()
        }
    }

    private fun createAudioSource() {
        //create an AudioSource instance
        audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("101", audioSource)
    }

    private fun createVideoSource(videoCapturerAndroid: VideoCapturer?) {
        //Create a VideoSource instance
        if (videoCapturerAndroid != null) {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase?.eglBaseContext)
            videoSource = peerConnectionFactory?.createVideoSource(videoCapturerAndroid.isScreencast)
            videoCapturerAndroid.initialize(surfaceTextureHelper, this, videoSource?.capturerObserver)
        }
        localVideoTrack = peerConnectionFactory?.createVideoTrack("100", videoSource)
    }

    private fun createMediaConstraints() {
        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = MediaConstraints()
        videoConstraints = MediaConstraints()
        videoConstraints!!.mandatory.add(MediaConstraints.KeyValuePair(
            "maxHeight", (sDeviceHeight / SCREEN_RESOLUTION_SCALE).toString()))
        videoConstraints!!.mandatory.add(MediaConstraints.KeyValuePair(
            "maxWidth", (sDeviceWidth / SCREEN_RESOLUTION_SCALE).toString()))
        videoConstraints!!.mandatory.add(MediaConstraints.KeyValuePair(
            "maxFrameRate", 0.toString()))
        videoConstraints!!.mandatory.add(MediaConstraints.KeyValuePair(
            "minFrameRate", 0.toString()))
    }

    private fun createPeerConnectionFactoryInstance() {
        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        val options = PeerConnectionFactory.Options()
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(rootEglBase?.eglBaseContext, true, true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(rootEglBase?.eglBaseContext)
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()
    }

    private fun initializePeerConnectionFactoryGlobals() {
        //Initialize PeerConnectionFactory globals.
        val initializationOptions = InitializationOptions.builder(this).createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)
        rootEglBase = EglBase.create()
    }


    /**
     * This method will be called directly by the app when it is the initiator and has got the local media
     * or when the remote peer sends a message through socket that it is ready to transmit AV data
     */
    override fun initiate() {
        Log.d(TAG, "initializing peer connection and creating offer")
        runOnUiThread {
            if (!SignallingClient.isStarted && localVideoTrack != null && SignallingClient.isChannelReady) {
                createPeerConnection()
                SignallingClient.isStarted = true
                if (SignallingClient.isInitiator) {
                    sendOffer()
                }
            }
        }
    }

    /**
     * Creating the local peerconnection instance
     */
    private fun createPeerConnection() {
        localPeer = peerConnectionFactory?.createPeerConnection(getRtcConfig(), object : CustomPeerConnectionObserver("localPeerCreation") {
            override fun onIceCandidate(iceCandidate: IceCandidate) {
                super.onIceCandidate(iceCandidate)
                onIceCandidateCreated(iceCandidate)
            }

            override fun onAddStream(mediaStream: MediaStream) {
                showToast("Received Remote stream")
                super.onAddStream(mediaStream)
                gotRemoteStream(mediaStream)
            }

            override fun onDataChannel(dataChannel: DataChannel) {
                super.onDataChannel(dataChannel)
                Log.d(TAG, "data channel state: " + dataChannel.state())
                dataChannel.registerObserver(object : DataChannel.Observer {
                    override fun onBufferedAmountChange(l: Long) {}
                    override fun onStateChange() {}
                    override fun onMessage(buffer: DataChannel.Buffer) {
                        if(SignallingClient.isInitiator){
                            Log.d(TAG, "onMessage: got message")
                            val message = byteBufferToString(buffer.data, Charset.defaultCharset())
                            showToast(message)
                            initiateAction(message)
                        }
                    }
                })
            }
        })
        createDataChannel()
        if(SignallingClient.isInitiator) addStreamToLocalPeer()
    }

    private fun initiateAction(message: String) {
        performAction(ActionUtil.getTypeFromAction(message))
    }

    private fun performAction(action: ActionType) {
        when(action) {

            is SWIPE -> performSwipe(action.x1, action.y1, action.x2, action.y2, action.duration)

            is CLICK -> performClick(action.x, action.y)

            is LONGCLICK -> performLongClick(action.x, action.y)

            is PASTE -> performPaste(action.x, action.y, action.text)
        }
    }

    private fun createDataChannel() {
        localDataChannel = localPeer!!.createDataChannel("sendDataChannel", DataChannel.Init())
        localDataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(l: Long) {}
            override fun onStateChange() {}
            override fun onMessage(buffer: DataChannel.Buffer) {}
        })
    }

    /**
     * Received remote peer's media stream. we will get the first video track and render it
     */
    private fun gotRemoteStream(stream: MediaStream) {
        //we have remote video stream. add to the renderer.
        Log.d(TAG, "gotRemoteStream: "+localPeer?.connectionState().toString())
        val videoTrack = stream.videoTracks[0]
        runOnUiThread {
            try {
                goFullScreen()
                setSurfaceViewTouchListener()
                remote_gl_surface_view.visibility = View.VISIBLE
                roomNameSlate.visibility = View.GONE
                remote_gl_surface_view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                videoTrack.addSink(remote_gl_surface_view)


                Log.v("$TAG-DeviceHeight",remote_gl_surface_view.measuredHeight.toString())
                Log.v("$TAG-DeviceWidth",remote_gl_surface_view.measuredWidth.toString())
                remote_gl_surface_view.addFrameListener({
                    frameHeight = remote_gl_surface_view.measuredHeight
                    frameWidth = remote_gl_surface_view.measuredWidth
                    Log.v("$TAG-FrameHeight",remote_gl_surface_view.measuredHeight.toString())
                    Log.v("$TAG-FrameWidth",remote_gl_surface_view.measuredWidth.toString())
                },1.0f)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setSurfaceViewTouchListener() {
        val gestureDetector = object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent?): Boolean {
//                Log.v("sahil", "CLICK(${e?.x}, ${e?.y})")
//                sendClickEvent(e?.x, e?.y, false)
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                Log.v("sahil", "DOUBLE_CLICK(${e?.x}, ${e?.y})")
                sendClickEvent(e?.x, e?.y, false)
                sendClickEvent(e?.x, e?.y, false)
                return false
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
//                Log.v("sahil", "CLICK(${e?.x}, ${e?.y})")
//                sendClickEvent(e?.x, e?.y, false)
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                Log.v("sahil", "CLICK(${e?.x}, ${e?.y})")
                sendClickEvent(e?.x,e?.y, false)
                return false
            }

            override fun onLongPress(e: MotionEvent?) {
                super.onLongPress(e)
                Log.v("sahil", "LONGCLICK(${e?.x}, ${e?.y})")
                sendClickEvent(e?.x, e?.y, true)
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                val time1 = e1?.eventTime ?: 0
                val time2 = e2?.eventTime ?: 0
                val t3 = time2 - time1

                Log.v("sahil", "SWIPE(${e1?.x}, ${e1?.y}, ${e2?.x}, ${e2?.y}, $t3)")
                sendSwipeEvent(e1?.x, e1?.y, e2?.x, e2?.y, t3)

                return false
            }
        }

        val motionDetector = GestureDetector(this, gestureDetector)

        remote_gl_surface_view.setOnTouchListener { v, event ->
            return@setOnTouchListener motionDetector.onTouchEvent(event)
        }
    }

    private fun sendClickEvent(x: Float?, y: Float?, isLongClick: Boolean) {
        var xPer = 0f
        var yPer = 0f
        var message = ""

        yPer = calculateYPercentage(y)

        xPer = calculateXPercentage(x)

        message = if(isLongClick) "LONGCLICK($xPer, $yPer)" else "CLICK($xPer, $yPer)"

        val data: ByteBuffer = stringToByteBuffer(
            msg = message,
            charset = Charset.defaultCharset()
        )
        localDataChannel?.send(DataChannel.Buffer(data, false))
    }

    private fun sendSwipeEvent(x1: Float?, y1: Float?, x2: Float?, y2: Float?, time: Long) {
        val x1Per = calculateXPercentage(x1)
        val y1Per = calculateYPercentage(y1)
        val x2Per = calculateXPercentage(x2)
        val y2Per = calculateYPercentage(y2)

        val data: ByteBuffer = stringToByteBuffer(
            msg = "SWIPE($x2Per, $y1Per, $x1Per, $y2Per, $time)",
            charset = Charset.defaultCharset()
        )
        localDataChannel?.send(DataChannel.Buffer(data, false))
    }

    private fun calculateXPercentage(value: Float?): Float {
        return (value?.div(frameWidth.toFloat()))?.times(100f) ?: 0f
    }

    private fun calculateYPercentage(value: Float?): Float {
        return (value?.div(frameHeight.toFloat()))?.times(100f) ?: 0f
    }

    private fun goFullScreen() {
        actionBar?.hide()
    }

    private fun getRtcConfig(): RTCConfiguration {
        val rtcConfig = RTCConfiguration(peerIceServers)
        // TCP candidates are only useful when connecting to a server that supports ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        rtcConfig.enableDtlsSrtp = true
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA
        return rtcConfig
    }

    private fun byteBufferToString(buffer: ByteBuffer, charset: Charset): String {
        val bytes: ByteArray
        if (buffer.hasArray()) {
            bytes = buffer.array()
        } else {
            bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
        }
        return String(bytes, charset)
    }

    /**
     * Adding the screen recording stream to the peer object
     */
    private fun addStreamToLocalPeer() {
        //creating local mediastream
        val stream = peerConnectionFactory?.createLocalMediaStream("102")
        stream?.addTrack(localVideoTrack)
        localPeer?.addStream(stream)
    }

    /**
     * We generate the offer and send it over through socket to remote peer
     */
    private fun sendOffer() {
        sdpConstraints = MediaConstraints()
        sdpConstraints!!.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        sdpConstraints!!.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        localPeer!!.createOffer(object : CustomSdpObserver("localCreateOffer") {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                super.onCreateSuccess(sessionDescription)
                localPeer?.setLocalDescription(CustomSdpObserver("localSetLocalDesc"), sessionDescription)
                Log.d(Companion.TAG, "offer created")
                SignallingClient.emitOffer(sessionDescription)
            }
        }, sdpConstraints)
    }

    /**
     * Received local ice candidate. Send it to remote peer through signalling for negotiation
     */
    fun onIceCandidateCreated(iceCandidate: IceCandidate) {
        Log.d(Companion.TAG, "candidate created: $iceCandidate")
        SignallingClient.emitIceCandidate(iceCandidate)
    }

    /**
     * SignallingCallback - called when the room is created - i.e. you are the initiator
     * @param roomName
     */
    override fun onCreatedRoom(roomName: String?) {
        showToast("You created the room $roomName\n Ready to transmit: $gotUserMedia")
        if (gotUserMedia) {
            SignallingClient.emitMessage("got user media")
        }
    }

    override fun onNewPeerJoined() {
        showToast("Remote Peer Joined")
        Log.d(Companion.TAG, "Remote Peer Joined")
        initiate()
    }

    /**
     * SignallingCallback - Called when remote peer sends answer to your offer
     * answer contains sdp data
     * we ll give that sdp data to our peer object
     */
    override fun onAnswerReceived(data: JSONObject?) {
        showToast("Received Answer")
        Log.d(Companion.TAG, "Received Answer: remote wale ki description apne paas set kr lenge")
        try {
            localPeer!!.setRemoteDescription(
                CustomSdpObserver("localSetRemote"),
                SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(data?.getString("type")?.toLowerCase()),
                    data?.getString("sdp")
                )
            )
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    /**
     * Remote IceCandidate received
     * we ll give this candidate to our peer
     */
    override fun onIceCandidateReceived(data: JSONObject?) {
        Log.d(Companion.TAG, "candidate received: $data")
        try {
            localPeer!!.addIceCandidate(
                IceCandidate(data?.getString("id"), data?.getInt("label")!!, data.getString("candidate"))
            )
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onJoinedRoom() {
        showToast("You joined the room - hello")
        runOnUiThread {
            createPeerConnection()
            SignallingClient.isStarted = true
        }
    }

    /**
     * SignallingCallback - Called when remote peer sends offer
     */
    override fun onOfferReceived(data: JSONObject?) {
        showToast("Received Offer")
        runOnUiThread{
            if (!SignallingClient.isInitiator && !SignallingClient.isStarted) {
                createPeerConnection()
            }
            try {
                localPeer?.setRemoteDescription(
                    CustomSdpObserver("localSetRemote"),
                    SessionDescription(SessionDescription.Type.OFFER, data?.getString("sdp"))
                )
                doAnswer()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private fun doAnswer() {
        sdpConstraints = MediaConstraints()
        sdpConstraints?.mandatory?.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        sdpConstraints?.mandatory?.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        sdpConstraints?.optional?.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        localPeer?.createAnswer(object: CustomSdpObserver("localCreateAns") {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                super.onCreateSuccess(sessionDescription)
                localPeer?.setLocalDescription(CustomSdpObserver("localSetLocal"), sessionDescription)
                SignallingClient.emitAnswer(sessionDescription)
            }
        }, sdpConstraints)
    }

    /**
     * called when remote peer disconnects
     */
    override fun onRemoteHangUp(msg: String?) {
        showToast("Remote Peer hungup")
        runOnUiThread(this::hangup)
    }

    private fun hangup() {
        try {
            if (localPeer != null) {
                localPeer?.close()
            }
            localPeer = null;
            SignallingClient.close();
        } catch (e: Exception) {
            e.printStackTrace();
        }
    }

    override fun onDestroy() {
        SignallingClient.close();
        super.onDestroy();
    }

    private fun showToast(msg: String) {
        runOnUiThread{ Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    /**
     * This method will fetch list of stun and turn servers from xirsys (third party api)
     * these servers will be added to peer connection object
     */
    private fun getIceServers() {
        NetworkClient.getRetrofitInstance().getIceCandidates(getAuthToken()).enqueue(object : Callback<XirsysStunTurnResponse?> {

            override fun onResponse(call: Call<XirsysStunTurnResponse?>, response: Response<XirsysStunTurnResponse?>) {
                val body: XirsysStunTurnResponse? = response.body()
                iceServers = body?.iceServerList?.iceServers
                for ((url, username, credential) in iceServers!!) {
                    if (credential == null) {
                        val peerIceServer = PeerConnection.IceServer.builder(url).createIceServer()
                        peerIceServers.add(peerIceServer)
                    } else {
                        val peerIceServer = PeerConnection.IceServer.builder(url)
                            .setUsername(username)
                            .setPassword(credential)
                            .createIceServer()
                        peerIceServers.add(peerIceServer)
                    }
                }
                Log.d("onApiResponse", "IceServers\n" + iceServers.toString())
            }

            override fun onFailure(call: Call<XirsysStunTurnResponse?>, t: Throwable) {
                Log.d("onApiFailResponse", "IceServers\n" + t.stackTrace.toString())
            }
        })
    }

    private fun getAuthToken(): String? {
        var data = ByteArray(0)
        try {
            data = "archit690:ab40918c-7ab7-11ea-bc0e-0242ac110004".toByteArray(charset("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return "Basic " + Base64.encodeToString(data, Base64.NO_WRAP)
    }

}
