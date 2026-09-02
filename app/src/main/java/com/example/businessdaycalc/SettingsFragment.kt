package com.example.businessdaycalc

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = view.findViewById<LinearLayout>(R.id.settingsContainer)

        DeliveryType.values().forEach { type ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16))
                setBackgroundResource(android.R.drawable.list_selector_background)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val intent = Intent(requireContext(), ItemSettingsActivity::class.java).apply {
                        putExtra("TYPE_ID", type.id)
                    }
                    startActivity(intent)
                }
            }

            val tvName = TextView(requireContext()).apply {
                text = type.displayName
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(tvName)

            val tvArrow = TextView(requireContext()).apply {
                text = ">"
                textSize = 18f
                setTextColor(resources.getColor(R.color.text_secondary, null))
            }
            row.addView(tvArrow)

            container.addView(row)

            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(resources.getColor(R.color.divider, null))
            }
            container.addView(divider)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
