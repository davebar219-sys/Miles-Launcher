package com.davebar219.mileslauncher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private var workMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(48), dp(24), dp(24))
            setBackgroundColor(Color.rgb(15, 18, 28))
        }

        val title = TextView(this).apply {
            text = if (workMode) "Good morning, Dave." else "Welcome home, Dave."
            textSize = 29f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
        }

        val subtitle = TextView(this).apply {
            text = if (workMode) {
                "Work mode is active. What should we focus on?"
            } else {
                "Home mode is active. Time to breathe a little."
            }
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(8), 0, dp(24))
        }

        val modeButton = Button(this).apply {
            text = if (workMode) "Switch to Home" else "Switch to Work"
            setOnClickListener {
                workMode = !workMode
                render()
            }
        }

        val askMiles = Button(this).apply {
            text = "Ask Miles"
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Miles conversation panel is coming next.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val grid = GridLayout(this).apply {
            columnCount = 2
            rowCount = 2
            setPadding(0, dp(28), 0, 0)
        }

        val actions = if (workMode) {
            listOf(
                "Calendar" to Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR),
                "Email" to Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL),
                "Tasks" to null,
                "Settings" to Intent(Settings.ACTION_SETTINGS)
            )
        } else {
            listOf(
                "Camera" to Intent("android.media.action.IMAGE_CAPTURE"),
                "Music" to Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC),
                "Family" to null,
                "Settings" to Intent(Settings.ACTION_SETTINGS)
            )
        }

        actions.forEach { (label, intent) ->
            grid.addView(actionButton(label, intent))
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(modeButton, fullWidth())
        root.addView(askMiles, fullWidth())
        root.addView(grid, fullWidth())

        setContentView(root)
    }

    private fun actionButton(label: String, intent: Intent?): Button =
        Button(this).apply {
            text = label
            textSize = 16f
            setOnClickListener {
                if (intent == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "$label will be connected in the next version.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                try {
                    startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "No compatible app was found.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val margin = dp(8)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(92)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(margin, margin, margin, margin)
            }
        }

    private fun fullWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply {
        topMargin = dp(10)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
