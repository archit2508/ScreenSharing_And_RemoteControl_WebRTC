package com.example.webrtcclient_1.acessibility

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.example.webrtcclient_1.data.model.Screen
import com.example.webrtcclient_1.utils.constants.TAG
import com.example.webrtcclient_1.utils.extensions.getValueFromPercentage

/**
 * Helper class for Performing actions using AccessibilityService
 */

object AccessibilityHelper {

    private val accessibilityService: PaytmAccessibilityService by lazy {
        PaytmAccessibilityService.getAccessibilityInstance()
    }

    private var lastNode: AccessibilityNodeInfo? = null

    /**
     * Function to perform swipe gesture
     */
    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long) {
        val swipePath = Path().apply {
            moveTo(getValueFromPercentage(x2, Screen.screenWidth).toFloat(), getValueFromPercentage(y1, Screen.screenHeight).toFloat())
            lineTo(getValueFromPercentage(x1, Screen.screenWidth).toFloat(), getValueFromPercentage(y2, Screen.screenHeight).toFloat())
        }

        val gestureDescription = GestureDescription.Builder().apply {
            addStroke(GestureDescription.StrokeDescription(swipePath, 0, duration))
        }

        val bool = accessibilityService.dispatchGesture(gestureDescription.build(), null, null)
        Log.v(TAG, "$bool")
    }

    /**
     * Function to perform Click gesture
     */
    fun performClick(x: Float, y: Float) {
        val bool = accessibilityService.dispatchGesture(getClickPath(x, y, 1), null, null)
        Log.v(TAG, "$bool")
    }

    /**
     * Function to perform long click gesture
     */
    fun performLongClick(x: Float, y: Float) {
        val bool = accessibilityService.dispatchGesture(getClickPath(x, y, 1000), null, null)
        Log.v(TAG, "$bool")
    }

    /**
     * Function to find node at (x,y)
     */
    private fun findNode(root: AccessibilityNodeInfo?, x: Int, y: Int): AccessibilityNodeInfo? {
        root?.run {
            val bound = Rect()
            getBoundsInScreen(bound)

            if(bound.contains(x, y)) {
                if(root.childCount == 0) return root
                lastNode = root

                for(i in 0 until root.childCount) {
                    val temp = findNode(root.getChild(i), x, y)
                    if(temp != null) lastNode = temp
                }
            }
        }

        return lastNode
    }

    /**
     * Function to perform text paste on EditText
     */
    fun performPaste(x: Float, y: Float, text: String) {
        val bundle = Bundle().apply {
            putString(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }

        val node = findNode(
            accessibilityService.rootInActiveWindow,
            getValueFromPercentage(x, Screen.screenWidth), getValueFromPercentage(y, Screen.screenHeight)
        )
        val bool = node?.performAction(AccessibilityNodeInfoCompat.ACTION_SET_TEXT, bundle)
        Log.v(TAG, "$bool")
    }

    private fun getClickPath(x: Float, y: Float, duration: Long): GestureDescription {
        val clickPath = Path().apply {
            moveTo(getValueFromPercentage(x, Screen.screenWidth).toFloat(), getValueFromPercentage(y, Screen.screenHeight).toFloat())
        }

        return GestureDescription.Builder().apply {
            addStroke(GestureDescription.StrokeDescription(clickPath, 0, duration))
        }.build()
    }
    
}
