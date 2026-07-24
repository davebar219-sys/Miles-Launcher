package com.davebar219.mileslauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private var workMode = true
    private lateinit var appGrid: GridLayout
    private lateinit var modeTitle: TextView
    private lateinit var workButton: Button
    private lateinit var homeButton: Button
    private lateinit var searchBox: EditText
    private var allApps: List<ResolveInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allApps = loadLaunchableApps()
        showLauncher()
    }

    private fun showLauncher() {
        val background = Color.rgb(15, 18, 28)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(38), dp(20), dp(18))
            setBackgroundColor(background)
        }

        val title = TextView(this).apply {
            text = "Miles Launcher"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
        }

        modeTitle = TextView(this).apply {
            text = "Work mode"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(4), 0, dp(14))
        }

        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        workButton = Button(this).apply {
            text = "Work"
            setOnClickListener {
                workMode = true
                updateMode()
            }
        }

        homeButton = Button(this).apply {
            text = "Home"
            setOnClickListener {
                workMode = false
                updateMode()
            }
        }

        modeRow.addView(
            workButton,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        modeRow.addView(
            homeButton,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        searchBox = EditText(this).apply {
            hint = "Search apps"
            textSize = 16f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.rgb(29, 34, 49))
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    text: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    text: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    renderApps(text?.toString().orEmpty())
                }

                override fun afterTextChanged(text: Editable?) = Unit
            })
        }

        val scrollView = ScrollView(this)

        appGrid = GridLayout(this).apply {
            columnCount = 4
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }

        scrollView.addView(
            appGrid,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(title)
        root.addView(modeTitle)
        root.addView(modeRow)
        root.addView(
            searchBox,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
        )
        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
        updateMode()
        renderApps("")
    }

    private fun updateMode() {
        modeTitle.text = if (workMode) {
            "Work mode — focused and ready"
        } else {
            "Home mode — relaxed and personal"
        }

        workButton.isEnabled = !workMode
        homeButton.isEnabled = workMode
    }

    private fun renderApps(query: String) {
        appGrid.removeAllViews()

        val filteredApps = allApps.filter { app ->
            app.loadLabel(packageManager)
                .toString()
                .contains(query, ignoreCase = true)
        }

        filteredApps.forEach { app ->
            appGrid.addView(createAppTile(app))
        }
    }

    private fun loadLaunchableApps(): List<ResolveInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager
            .queryIntentActivities(launcherIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy {
                it.loadLabel(packageManager)
                    .toString()
                    .lowercase()
            }
    }

    private fun createAppTile(app: ResolveInfo): LinearLayout {
        val label = app.loadLabel(packageManager).toString()

        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(8), dp(4), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                launchApp(app)
            }
        }

        val icon = ImageView(this).apply {
            setImageDrawable(app.loadIcon(packageManager))
            contentDescription = label
            adjustViewBounds = true
        }

        val name = TextView(this).apply {
            text = label
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            maxLines = 2
            setPadding(0, dp(5), 0, 0)
        }

        tile.addView(
            icon,
            LinearLayout.LayoutParams(dp(54), dp(54))
        )

        tile.addView(
            name,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        tile.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = dp(108)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }

        return tile
    }

    private fun launchApp(app: ResolveInfo) {
        val activityInfo = app.activityInfo

        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(activityInfo.packageName, activityInfo.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(launchIntent)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Unable to open this app.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
