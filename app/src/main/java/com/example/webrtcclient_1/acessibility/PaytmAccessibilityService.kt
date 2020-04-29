package com.example.webrtcclient_1.acessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class PaytmAccessibilityService : AccessibilityService() {

    companion object {

        private lateinit var accessibilityService: PaytmAccessibilityService

        fun getAccessibilityInstance() = accessibilityService

    }


    override fun onServiceConnected() {
        super.onServiceConnected()

        accessibilityService = this
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

}