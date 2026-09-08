package com.example.businessdaycalc

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

enum class DeliveryType(val id: String, val displayName: String, val defaultDelivery: Int, val defaultStorageUse: Boolean, val defaultStorage: Int) {
    NORMAL("normal", "일반등기", 1, true, 4),
    CERTIFIED("certified", "내용증명", 2, true, 2),
    COURT("court", "법원등기", 3, false, 0),
    CONTRACT("contract", "계약등기", 3, true, 2)
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
                var storageSteps = obj.getInt("storageSteps")
                var deliverySteps = obj.getInt("deliverySteps")

                // Update to new defaults if user has not explicitly edited settings
                if (!obj.has("user_edited")) {
                    useStorage = type.defaultStorageUse
                    storageSteps = type.defaultStorage
                    deliverySteps = type.defaultDelivery
                }

                return DeliverySetting(
                    type = type,
                    deliverySteps = deliverySteps,
                    useStorage = useStorage,
                    storageSteps = storageSteps
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
