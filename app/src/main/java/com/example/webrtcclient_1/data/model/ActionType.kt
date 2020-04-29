package com.example.webrtcclient_1.data.model

sealed class ActionType

data class CLICK(val x: Float, val y: Float): ActionType()

data class LONGCLICK(val x: Float, val y: Float): ActionType()

data class SWIPE(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val duration: Long): ActionType()

data class PASTE(val x: Float, val y: Float, val text: String): ActionType()