package com.example.businessdaycalc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MidnightUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "com.example.businessdaycalc.MIDNIGHT_UPDATE" || 
            action == Intent.ACTION_BOOT_COMPLETED) {
            
            val updateIntents = listOf(
                Intent(context, BusinessDayWidgetLarge::class.java),
                Intent(context, BusinessDayWidgetMedium::class.java),
                Intent(context, BusinessDayWidgetSmall::class.java)
            )
            for (updateIntent in updateIntents) {
                updateIntent.action = "com.example.businessdaycalc.MIDNIGHT_UPDATE"
                context.sendBroadcast(updateIntent)
            }
        }
    }
}
