package com.example.webrtcclient_1.acessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class PaytmAccessibilityService : AccessibilityService() {

    companion object {

        var isEnabled = false

        private lateinit var accessibilityService: PaytmAccessibilityService

        fun getAccessibilityInstance() = accessibilityService

    }


    override fun onServiceConnected() {
        super.onServiceConnected()

        isEnabled = true
        accessibilityService = this
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onDestroy() {
        super.onDestroy()
        isEnabled = false
    }

}