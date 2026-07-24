package com.davebar219.mileslauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLauncher()
    }

    private fun showLauncher() {
        val background = Color.rgb(15, 18, 28)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(42), dp(20), dp(20))
            setBackgroundColor(background)
        }

        val title = TextView(this).apply {
            text = "Miles Launcher"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
        }

        val subtitle = TextView(this).apply {
            text = "Your apps"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(6), 0, dp(18))
        }

        val scrollView = ScrollView(this)

        val appGrid = GridLayout(this).apply {
            columnCount = 4
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }

        loadLaunchableApps().forEach { app ->
            appGrid.addView(createAppTile(app))
        }

        scrollView.addView(
            appGrid,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(title)
        root.addView(subtitle)
        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
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
            setPadding(dp(6), dp(10), dp(6), dp(10))
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
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            maxLines = 2
            setPadding(0, dp(6), 0, 0)
        }

        tile.addView(
            icon,
            LinearLayout.LayoutParams(dp(52), dp(52))
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
            height = dp(106)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(dp(3), dp(3), dp(3), dp(3))
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
