package com.example.webrtcclient_1.utils

import com.example.webrtcclient_1.data.model.*
import com.example.webrtcclient_1.data.model.ActionType

class ActionUtil {

    companion object {

        fun getTypeFromAction(action: String): ActionType {
            val numeric = action.replace(Regex("[a-zA-Z\\p{Ps}\\p{Pe}]"), "")

            val values = numeric
                .split(Regex(","))
                .filter {
                    it.isNotEmpty()
                }.map {
                    it.trim()
                }
                .toList()

            return when(action.substring(0, action.indexOf('('))) {

                "SWIPE" -> SWIPE(values[0].toFloat(), values[1].toFloat(), values[2].toFloat(), values[3].toFloat(), values[4].toLong())

                "CLICK" -> CLICK(values[0].toFloat(), values[1].toFloat())

                "LONGCLICK" -> LONGCLICK(values[0].toFloat(), values[1].toFloat())

                "PASTE" -> PASTE(values[0].toFloat(), values[1].toFloat(), "Taking Back to Main Screen Wait!!")

                else -> throw IllegalArgumentException("Wrong Action Type")
            }
        }
    }
}