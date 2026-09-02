package com.example.businessdaycalc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ItemSettingsActivity : AppCompatActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var currentSetting: DeliverySetting
    private lateinit var type: DeliveryType

    private lateinit var tvDelSteps: TextView
    private lateinit var tvStoSteps: TextView
    private lateinit var swUseStorage: Switch
    private lateinit var storageContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_settings)

        settingsManager = SettingsManager(this)
        
        val typeId = intent.getStringExtra("TYPE_ID") ?: return finish()
        type = DeliveryType.values().firstOrNull { it.id == typeId } ?: return finish()
        
        currentSetting = settingsManager.getSetting(type)

        findViewById<TextView>(R.id.tvTitle).text = "${type.displayName} 설정"
        
        tvDelSteps = findViewById(R.id.tvDelSteps)
        tvStoSteps = findViewById(R.id.tvStoSteps)
        swUseStorage = findViewById(R.id.swUseStorage)
        storageContainer = findViewById(R.id.storageContainer)

        updateUI()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnDelMinus).setOnClickListener {
            if (currentSetting.deliverySteps > 1) {
                currentSetting.deliverySteps--
                updateUI()
            }
        }
        findViewById<Button>(R.id.btnDelPlus).setOnClickListener {
            if (currentSetting.deliverySteps < 10) {
                currentSetting.deliverySteps++
                updateUI()
            }
        }

        swUseStorage.setOnCheckedChangeListener { _, isChecked ->
            currentSetting.useStorage = isChecked
            updateUI()
        }

        findViewById<Button>(R.id.btnStoMinus).setOnClickListener {
            if (currentSetting.storageSteps > 1) {
                currentSetting.storageSteps--
                updateUI()
            }
        }
        findViewById<Button>(R.id.btnStoPlus).setOnClickListener {
            if (currentSetting.storageSteps < 10) {
                currentSetting.storageSteps++
                updateUI()
            }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            settingsManager.saveSetting(currentSetting)
            updateWidgets()
            finish()
        }
    }

    private fun updateUI() {
        tvDelSteps.text = currentSetting.deliverySteps.toString()
        tvStoSteps.text = currentSetting.storageSteps.toString()
        swUseStorage.isChecked = currentSetting.useStorage
        storageContainer.alpha = if (currentSetting.useStorage) 1.0f else 0.5f
    }

    private fun updateWidgets() {
        val updateIntents = listOf(
            Intent(this, BusinessDayWidgetLarge::class.java),
            Intent(this, BusinessDayWidgetMedium::class.java),
            Intent(this, BusinessDayWidgetSmall::class.java)
        )
        for (intent in updateIntents) {
            intent.action = "android.appwidget.action.APPWIDGET_UPDATE"
            sendBroadcast(intent)
        }
    }
}
