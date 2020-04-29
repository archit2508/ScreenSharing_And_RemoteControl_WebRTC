package com.example.webrtcclient_1.utils.extensions

import android.app.Activity
import android.graphics.Point
import android.view.Display
import android.widget.Toast
import androidx.fragment.app.Fragment

fun <T : Activity> T.getScreenWidth(): Int {
    val realSize = Point()
    Display::class.java.getMethod("getRealSize", Point::class.java).invoke(windowManager.defaultDisplay, realSize)
    return realSize.x
}

fun <T : Activity> T.getScreenHeight(): Int {
    val realSize = Point()
    Display::class.java.getMethod("getRealSize", Point::class.java).invoke(windowManager.defaultDisplay, realSize)
    return realSize.y
}

fun <T : Fragment> T.showToast(text: String) {
    Toast.makeText(this.activity, text, Toast.LENGTH_SHORT).show()
}