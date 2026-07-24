package com.davebar219.mileslauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
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
    private lateinit var favoritesGrid: GridLayout
    private lateinit var favoritesTitle: TextView
    private lateinit var modeTitle: TextView
    private lateinit var workButton: Button
    private lateinit var homeButton: Button
    private lateinit var manageHiddenButton: Button
    private lateinit var searchBox: EditText
    private lateinit var prefs: SharedPreferences

    private var allApps: List<ResolveInfo> = emptyList()

    private val workFavorites = mutableSetOf<String>()
    private val homeFavorites = mutableSetOf<String>()
    private val workHidden = mutableSetOf<String>()
    private val homeHidden = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("miles_launcher", MODE_PRIVATE)

        workFavorites.addAll(
            prefs.getStringSet("work_favorites", emptySet()) ?: emptySet()
        )
        homeFavorites.addAll(
            prefs.getStringSet("home_favorites", emptySet()) ?: emptySet()
        )
        workHidden.addAll(
            prefs.getStringSet("work_hidden", emptySet()) ?: emptySet()
        )
        homeHidden.addAll(
            prefs.getStringSet("home_hidden", emptySet()) ?: emptySet()
        )

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
            hint = "Search visible apps"
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

        manageHiddenButton = Button(this).apply {
            text = "Manage hidden apps"
            setOnClickListener {
                showHiddenAppsDialog()
            }
        }

        favoritesTitle = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(6))
        }

        favoritesGrid = GridLayout(this).apply {
            columnCount = 4
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }

        val allAppsTitle = TextView(this).apply {
            text = "Visible apps"
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(12), 0, dp(6))
        }

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        appGrid = GridLayout(this).apply {
            columnCount = 4
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }

        scrollContent.addView(favoritesTitle)
        scrollContent.addView(favoritesGrid)
        scrollContent.addView(allAppsTitle)
        scrollContent.addView(appGrid)

        val scrollView = ScrollView(this).apply {
            addView(scrollContent)
        }

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
                bottomMargin = dp(8)
            }
        )

        root.addView(
            manageHiddenButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
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
    }

    private fun updateMode() {
        modeTitle.text = if (workMode) {
            "Work mode — focused and ready"
        } else {
            "Home mode — relaxed and personal"
        }

        workButton.isEnabled = !workMode
        homeButton.isEnabled = workMode

        renderFavorites()
        renderApps(searchBox.text?.toString().orEmpty())
    }

    private fun renderFavorites() {
        favoritesGrid.removeAllViews()

        val currentFavorites = getCurrentFavorites()
        val currentHidden = getCurrentHidden()

        val favoriteApps = allApps.filter { app ->
            val appId = getAppId(app)
            appId in currentFavorites && appId !in currentHidden
        }

        favoritesTitle.text = if (workMode) {
            "Work favorites"
        } else {
            "Home favorites"
        }

        if (favoriteApps.isEmpty()) {
            val emptyMessage = TextView(this).apply {
                text = "Long-press an app below to pin it here."
                textSize = 14f
                setTextColor(Color.GRAY)
                setPadding(0, dp(6), 0, dp(10))
            }

            favoritesGrid.addView(
                emptyMessage,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(0, 4, 1f)
                }
            )
            return
        }

        favoriteApps.forEach { app ->
            favoritesGrid.addView(createAppTile(app))
        }
    }

    private fun renderApps(query: String) {
        appGrid.removeAllViews()

        val currentHidden = getCurrentHidden()

        val filteredApps = allApps.filter { app ->
            val appId = getAppId(app)
            appId !in currentHidden &&
                app.loadLabel(packageManager)
                    .toString()
                    .contains(query, ignoreCase = true)
        }

        filteredApps.forEach { app ->
            appGrid.addView(createAppTile(app))
        }
    }

    private fun createAppTile(app: ResolveInfo): LinearLayout {
        val label = app.loadLabel(packageManager).toString()
        val appId = getAppId(app)
        val isFavorite = appId in getCurrentFavorites()

        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(8), dp(4), dp(8))
            isClickable = true
            isFocusable = true

            setOnClickListener {
                launchApp(app)
            }

            setOnLongClickListener {
                showAppActions(app)
                true
            }
        }

        val icon = ImageView(this).apply {
            setImageDrawable(app.loadIcon(packageManager))
            contentDescription = label
            adjustViewBounds = true
        }

        val name = TextView(this).apply {
            text = if (isFavorite) "★ $label" else label
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            maxLines = 2
            setPadding(0, dp(5), 0, 0)
        }

        tile.addView(icon, LinearLayout.LayoutParams(dp(54), dp(54)))
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

    private fun showAppActions(app: ResolveInfo) {
        val label = app.loadLabel(packageManager).toString()
        val appId = getAppId(app)
        val favorites = getCurrentFavorites()

        val favoriteAction = if (appId in favorites) {
            "Remove from favorites"
        } else {
            "Add to favorites"
        }

        val options = arrayOf(
            favoriteAction,
            "Hide in this mode",
            "Cancel"
        )

        AlertDialog.Builder(this)
            .setTitle(label)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> toggleFavorite(app)
                    1 -> hideApp(app)
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun toggleFavorite(app: ResolveInfo) {
        val appId = getAppId(app)
        val label = app.loadLabel(packageManager).toString()
        val favorites = getCurrentFavorites()

        val message = if (appId in favorites) {
            favorites.remove(appId)
            "$label removed from favorites"
        } else {
            favorites.add(appId)
            "$label added to favorites"
        }

        savePreferences()
        refreshCurrentMode()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun hideApp(app: ResolveInfo) {
        val appId = getAppId(app)
        val label = app.loadLabel(packageManager).toString()

        getCurrentHidden().add(appId)
        getCurrentFavorites().remove(appId)

        savePreferences()
        refreshCurrentMode()

        Toast.makeText(
            this,
            "$label hidden in ${currentModeName()} mode",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showHiddenAppsDialog() {
        val hidden = getCurrentHidden()

        val hiddenApps = allApps.filter { app ->
            getAppId(app) in hidden
        }

        if (hiddenApps.isEmpty()) {
            Toast.makeText(
                this,
                "No hidden apps in ${currentModeName()} mode.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val labels = hiddenApps
            .map { it.loadLabel(packageManager).toString() }
            .toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("${currentModeName()} hidden apps")
            .setItems(labels) { _, which ->
                val app = hiddenApps[which]
                hidden.remove(getAppId(app))
                savePreferences()
                refreshCurrentMode()

                Toast.makeText(
                    this,
                    "${labels[which]} is visible again",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun refreshCurrentMode() {
        renderFavorites()
        renderApps(searchBox.text?.toString().orEmpty())
    }

    private fun getCurrentFavorites(): MutableSet<String> {
        return if (workMode) workFavorites else homeFavorites
    }

    private fun getCurrentHidden(): MutableSet<String> {
        return if (workMode) workHidden else homeHidden
    }

    private fun currentModeName(): String {
        return if (workMode) "Work" else "Home"
    }

    private fun savePreferences() {
        prefs.edit()
            .putStringSet("work_favorites", HashSet(workFavorites))
            .putStringSet("home_favorites", HashSet(homeFavorites))
            .putStringSet("work_hidden", HashSet(workHidden))
            .putStringSet("home_hidden", HashSet(homeHidden))
            .apply()
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

    private fun getAppId(app: ResolveInfo): String {
        return "${app.activityInfo.packageName}/${app.activityInfo.name}"
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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
