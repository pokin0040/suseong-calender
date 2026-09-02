package com.example.businessdaycalc

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

enum class DeliveryType(val id: String, val displayName: String, val defaultDelivery: Int, val defaultStorageUse: Boolean, val defaultStorage: Int) {
    NORMAL("normal", "일반등기", 1, true, 5),
    COURT("court", "법원등기", 3, false, 0),
    CERTIFIED("certified", "내용증명", 2, true, 4),
    CONTRACT("contract", "계약등기", 3, true, 5)
}

data class DeliverySetting(
    val type: DeliveryType,
    var deliverySteps: Int,
    var useStorage: Boolean,
    var storageSteps: Int
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    fun getSetting(type: DeliveryType): DeliverySetting {
        val jsonStr = prefs.getString(type.id, null)
        if (jsonStr != null) {
            try {
                val obj = JSONObject(jsonStr)
                var useStorage = obj.getBoolean("useStorage")
                // Fix for Court mail: default useStorage should be false unless user explicitly edited it
                if (type == DeliveryType.COURT && !obj.has("user_edited")) {
                    useStorage = false
                }
                return DeliverySetting(
                    type = type,
                    deliverySteps = obj.getInt("deliverySteps"),
                    useStorage = useStorage,
                    storageSteps = obj.getInt("storageSteps")
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Return default
        return DeliverySetting(type, type.defaultDelivery, type.defaultStorageUse, type.defaultStorage)
    }

    fun getAllSettings(): List<DeliverySetting> {
        return DeliveryType.values().map { getSetting(it) }
    }

    fun saveSetting(setting: DeliverySetting) {
        val obj = JSONObject()
        obj.put("deliverySteps", setting.deliverySteps)
        obj.put("useStorage", setting.useStorage)
        obj.put("storageSteps", setting.storageSteps)
        obj.put("user_edited", true)
        prefs.edit().putString(setting.type.id, obj.toString()).apply()
    }
}
