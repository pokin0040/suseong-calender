package com.example.businessdaycalc

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WidgetConfigureActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private lateinit var previewContainer: FrameLayout
    private lateinit var sbOpacity: SeekBar
    private lateinit var tvOpacityValue: TextView
    private lateinit var rgTheme: RadioGroup
    private lateinit var rbLight: RadioButton
    private lateinit var rbDark: RadioButton
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button

    private var currentOpacity = 100
    private var isDarkTheme = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        setContentView(R.layout.activity_widget_configure)

        // Find AppWidgetId from Intent
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        previewContainer = findViewById(R.id.previewContainer)
        sbOpacity = findViewById(R.id.sbOpacity)
        tvOpacityValue = findViewById(R.id.tvOpacityValue)
        rgTheme = findViewById(R.id.rgTheme)
        rbLight = findViewById(R.id.rbLight)
        rbDark = findViewById(R.id.rbDark)
        btnCancel = findViewById(R.id.btnCancel)
        btnSave = findViewById(R.id.btnSave)

        // Load existing preferences if any
        currentOpacity = WidgetPreferences.getOpacity(this, appWidgetId)
        isDarkTheme = WidgetPreferences.isDark(this, appWidgetId)

        sbOpacity.progress = currentOpacity
        tvOpacityValue.text = "$currentOpacity%"
        if (isDarkTheme) rbDark.isChecked = true else rbLight.isChecked = true

        setupListeners()
        updatePreview()
    }

    private fun setupListeners() {
        sbOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentOpacity = progress
                tvOpacityValue.text = "$currentOpacity%"
                updatePreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            isDarkTheme = (checkedId == R.id.rbDark)
            updatePreview()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            // Save configuration
            WidgetPreferences.saveOpacity(this, appWidgetId, currentOpacity)
            WidgetPreferences.saveTheme(this, appWidgetId, isDarkTheme)

            // Push update to widget
            val appWidgetManager = AppWidgetManager.getInstance(this)
            
            // Try updating large, medium, small widgets safely
            WidgetHelper.updateWidgets(this, appWidgetManager, appWidgetId, WidgetType.LARGE)
            WidgetHelper.updateWidgets(this, appWidgetManager, appWidgetId, WidgetType.MEDIUM)
            WidgetHelper.updateWidgets(this, appWidgetManager, appWidgetId, WidgetType.SMALL)

            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }

    private fun updatePreview() {
        previewContainer.removeAllViews()

        // Inflate Large Widget preview as representation
        val previewView = layoutInflater.inflate(R.layout.widget_large, previewContainer, false)

        val alpha255 = (currentOpacity * 255 / 100)
        val bgColor = if (isDarkTheme) {
            Color.argb(alpha255, 33, 33, 33)
        } else {
            Color.argb(alpha255, 255, 255, 255)
        }

        val widgetRoot = previewView.findViewById<View>(R.id.widget_root)
        widgetRoot?.setBackgroundColor(bgColor)

        previewContainer.addView(previewView)
    }
}
