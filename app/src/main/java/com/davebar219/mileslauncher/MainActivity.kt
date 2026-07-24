package com.davebar219.mileslauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var root: LinearLayout
    private lateinit var headerCard: LinearLayout
    private lateinit var contentCard: LinearLayout
    private lateinit var modeTitle: TextView
    private lateinit var modeSubtitle: TextView
    private lateinit var workButton: Button
    private lateinit var homeButton: Button
    private lateinit var searchBox: EditText
    private lateinit var clearSearchButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var hiddenButton: Button
    private lateinit var favoritesTitle: TextView
    private lateinit var allAppsTitle: TextView
    private lateinit var favoritesGrid: GridLayout
    private lateinit var appGrid: GridLayout
    private lateinit var emptyAppsText: TextView
    private lateinit var scrollView: ScrollView

    private var allApps: List<ResolveInfo> = emptyList()
    private var workMode = true
    private var darkTheme = true
    private var gridColumns = 4
    private var iconSizeDp = 54
    private var labelSizeSp = 11f
    private var sortMode = SORT_ALPHABETICAL
    private var compactMode = false

    private val workFavorites = mutableSetOf<String>()
    private val homeFavorites = mutableSetOf<String>()
    private val workHidden = mutableSetOf<String>()
    private val homeHidden = mutableSetOf<String>()

    private data class Palette(
        val background: Int,
        val surface: Int,
        val surfaceRaised: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val accent: Int,
        val accentSoft: Int,
        val border: Int,
        val danger: Int
    )

    private val palette: Palette
        get() = if (darkTheme) {
            Palette(
                background = Color.rgb(10, 13, 22),
                surface = Color.rgb(21, 25, 38),
                surfaceRaised = Color.rgb(29, 35, 52),
                textPrimary = Color.WHITE,
                textSecondary = Color.rgb(174, 183, 204),
                accent = if (workMode) Color.rgb(77, 142, 255) else Color.rgb(164, 103, 255),
                accentSoft = if (workMode) Color.rgb(32, 57, 94) else Color.rgb(62, 41, 91),
                border = Color.rgb(48, 56, 76),
                danger = Color.rgb(244, 103, 119)
            )
        } else {
            Palette(
                background = Color.rgb(239, 243, 250),
                surface = Color.WHITE,
                surfaceRaised = Color.rgb(247, 249, 253),
                textPrimary = Color.rgb(23, 28, 39),
                textSecondary = Color.rgb(91, 101, 122),
                accent = if (workMode) Color.rgb(39, 105, 230) else Color.rgb(124, 67, 211),
                accentSoft = if (workMode) Color.rgb(220, 233, 255) else Color.rgb(237, 226, 255),
                border = Color.rgb(214, 220, 232),
                danger = Color.rgb(196, 48, 72)
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        loadPreferences()
        allApps = loadLaunchableApps()
        buildUi()
        refreshEverything(animate = false)
    }

    override fun onResume() {
        super.onResume()
        val latestApps = loadLaunchableApps()
        if (latestApps.map(::getAppId) != allApps.map(::getAppId)) {
            allApps = latestApps
            refreshEverything(animate = false)
        }
    }

    private fun loadPreferences() {
        workMode = prefs.getBoolean(KEY_WORK_MODE, true)
        darkTheme = prefs.getBoolean(KEY_DARK_THEME, true)
        gridColumns = prefs.getInt(KEY_GRID_COLUMNS, 4).coerceIn(3, 5)
        iconSizeDp = prefs.getInt(KEY_ICON_SIZE, 54).coerceIn(42, 72)
        labelSizeSp = prefs.getFloat(KEY_LABEL_SIZE, 11f).coerceIn(9f, 14f)
        sortMode = prefs.getInt(KEY_SORT_MODE, SORT_ALPHABETICAL)
        compactMode = prefs.getBoolean(KEY_COMPACT_MODE, false)

        workFavorites.addAll(prefs.getStringSet(KEY_WORK_FAVORITES, emptySet()) ?: emptySet())
        homeFavorites.addAll(prefs.getStringSet(KEY_HOME_FAVORITES, emptySet()) ?: emptySet())
        workHidden.addAll(prefs.getStringSet(KEY_WORK_HIDDEN, emptySet()) ?: emptySet())
        homeHidden.addAll(prefs.getStringSet(KEY_HOME_HIDDEN, emptySet()) ?: emptySet())
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(30), dp(16), dp(14))
        }

        headerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        modeTitle = TextView(this).apply {
            textSize = 27f
            setTypeface(typeface, Typeface.BOLD)
        }

        modeSubtitle = TextView(this).apply {
            textSize = 13f
            setPadding(0, dp(2), 0, 0)
        }

        titleColumn.addView(modeTitle)
        titleColumn.addView(modeSubtitle)

        settingsButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_preferences)
            contentDescription = "Launcher settings"
            setPadding(dp(11), dp(11), dp(11), dp(11))
            setOnClickListener { showSettingsDialog() }
        }

        topRow.addView(
            titleColumn,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        topRow.addView(settingsButton, squareParams(46))

        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        workButton = Button(this).apply {
            text = "Work"
            isAllCaps = false
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setOnClickListener { switchMode(true) }
        }

        homeButton = Button(this).apply {
            text = "Home"
            isAllCaps = false
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setOnClickListener { switchMode(false) }
        }

        modeRow.addView(workButton, weightedParams())
        modeRow.addView(homeButton, weightedParams())

        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(4), 0)
        }

        val searchIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            contentDescription = null
            setPadding(dp(4), dp(4), dp(8), dp(4))
        }

        searchBox = EditText(this).apply {
            hint = "Search apps"
            textSize = 16f
            setSingleLine(true)
            background = null
            setPadding(0, dp(12), 0, dp(12))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun afterTextChanged(s: Editable?) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    clearSearchButton.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                    renderApps(s?.toString().orEmpty(), animate = true)
                }
            })
        }

        clearSearchButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            contentDescription = "Clear search"
            visibility = View.GONE
            setPadding(dp(9), dp(9), dp(9), dp(9))
            setOnClickListener {
                searchBox.setText("")
                searchBox.clearFocus()
                hideKeyboard()
            }
        }

        searchRow.addView(searchIcon, squareParams(38))
        searchRow.addView(searchBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        searchRow.addView(clearSearchButton, squareParams(42))

        headerCard.addView(topRow)
        headerCard.addView(
            modeRow,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply {
                topMargin = dp(16)
            }
        )
        headerCard.addView(
            searchRow,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
        )

        contentCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(18))
        }

        val quickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        hiddenButton = Button(this).apply {
            isAllCaps = false
            textSize = 13f
            setOnClickListener { showHiddenAppsDialog() }
        }

        val sortButton = Button(this).apply {
            text = "Sort"
            isAllCaps = false
            textSize = 13f
            setOnClickListener { showSortDialog() }
        }

        quickRow.addView(hiddenButton, weightedParams())
        quickRow.addView(sortButton, weightedParams(leftMargin = 8))

        favoritesTitle = sectionTitle()
        favoritesGrid = newGrid()
        allAppsTitle = sectionTitle()
        emptyAppsText = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(28), dp(12), dp(28))
            visibility = View.GONE
        }
        appGrid = newGrid()

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(quickRow)
            addView(favoritesTitle, topMarginParams(18))
            addView(favoritesGrid)
            addView(allAppsTitle, topMarginParams(18))
            addView(emptyAppsText)
            addView(appGrid)
        }

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            addView(scrollContent)
        }

        contentCard.addView(
            scrollView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        root.addView(headerCard)
        root.addView(
            contentCard,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = dp(12)
            }
        )

        setContentView(root)
    }

    private fun switchMode(toWorkMode: Boolean) {
        if (workMode == toWorkMode) return
        workMode = toWorkMode
        prefs.edit().putBoolean(KEY_WORK_MODE, workMode).apply()
        searchBox.setText("")
        scrollView.scrollTo(0, 0)
        refreshEverything(animate = true)
    }

    private fun refreshEverything(animate: Boolean) {
        applyTheme()
        updateModeHeader()
        renderFavorites(animate)
        renderApps(searchBox.text?.toString().orEmpty(), animate)
    }

    private fun applyTheme() {
        val p = palette
        root.setBackgroundColor(p.background)
        headerCard.background = roundedDrawable(p.surface, 26, p.border, 1)
        contentCard.background = roundedDrawable(p.surface, 26, p.border, 1)

        modeTitle.setTextColor(p.textPrimary)
        modeSubtitle.setTextColor(p.textSecondary)
        favoritesTitle.setTextColor(p.textPrimary)
        allAppsTitle.setTextColor(p.textPrimary)
        emptyAppsText.setTextColor(p.textSecondary)

        settingsButton.background = roundedDrawable(p.surfaceRaised, 15, p.border, 1)
        settingsButton.setColorFilter(p.textPrimary)
        clearSearchButton.background = roundedDrawable(Color.TRANSPARENT, 14)
        clearSearchButton.setColorFilter(p.textSecondary)

        val searchParent = searchBox.parent as? View
        searchParent?.background = roundedDrawable(p.surfaceRaised, 18, p.border, 1)
        searchBox.setTextColor(p.textPrimary)
        searchBox.setHintTextColor(p.textSecondary)

        hiddenButton.setTextColor(p.textPrimary)
        hiddenButton.background = roundedDrawable(p.surfaceRaised, 16, p.border, 1)
        (hiddenButton.parent as? LinearLayout)?.let { row ->
            val sortButton = row.getChildAt(1) as? Button
            sortButton?.setTextColor(p.textPrimary)
            sortButton?.background = roundedDrawable(p.surfaceRaised, 16, p.border, 1)
        }

        updateModeButtons()
    }

    private fun updateModeHeader() {
        modeTitle.text = if (workMode) "Miles · Work" else "Miles · Home"
        modeSubtitle.text = if (workMode) {
            "Focused apps, fewer distractions"
        } else {
            "Personal space, your way"
        }

        hiddenButton.text = "Hidden (${getCurrentHidden().size})"
        favoritesTitle.text = if (workMode) "Work favorites" else "Home favorites"
        allAppsTitle.text = when (sortMode) {
            SORT_FAVORITES_FIRST -> "All apps · favorites first"
            SORT_REVERSE -> "All apps · Z–A"
            else -> "All apps · A–Z"
        }
        updateModeButtons()
    }

    private fun updateModeButtons() {
        val p = palette
        workButton.background = roundedDrawable(if (workMode) p.accent else Color.TRANSPARENT, 15)
        homeButton.background = roundedDrawable(if (!workMode) p.accent else Color.TRANSPARENT, 15)
        workButton.setTextColor(if (workMode) Color.WHITE else p.textSecondary)
        homeButton.setTextColor(if (!workMode) Color.WHITE else p.textSecondary)
        (workButton.parent as? View)?.background = roundedDrawable(p.surfaceRaised, 19, p.border, 1)
    }

    private fun renderFavorites(animate: Boolean) {
        favoritesGrid.removeAllViews()
        favoritesGrid.columnCount = gridColumns

        val favorites = getCurrentFavorites()
        val hidden = getCurrentHidden()
        val apps = sortedApps(
            allApps.filter { getAppId(it) in favorites && getAppId(it) !in hidden }
        )

        if (apps.isEmpty()) {
            favoritesGrid.addView(
                TextView(this).apply {
                    text = "Long-press an app to pin it here."
                    textSize = 13f
                    setTextColor(palette.textSecondary)
                    setPadding(dp(4), dp(8), dp(4), dp(8))
                },
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(0, gridColumns, 1f)
                }
            )
        } else {
            apps.forEachIndexed { index, app ->
                favoritesGrid.addView(createAppTile(app, highlighted = true, index = index, animate = animate))
            }
        }
    }

    private fun renderApps(query: String, animate: Boolean) {
        appGrid.removeAllViews()
        appGrid.columnCount = gridColumns

        val normalized = query.trim()
        val hidden = getCurrentHidden()
        val filtered = allApps.filter { app ->
            getAppId(app) !in hidden &&
                app.loadLabel(packageManager).toString().contains(normalized, ignoreCase = true)
        }
        val apps = sortedApps(filtered)

        emptyAppsText.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
        emptyAppsText.text = if (normalized.isBlank()) {
            "No visible apps in this profile."
        } else {
            "No apps match “$normalized”."
        }

        apps.forEachIndexed { index, app ->
            appGrid.addView(createAppTile(app, highlighted = false, index = index, animate = animate))
        }
    }

    private fun sortedApps(apps: List<ResolveInfo>): List<ResolveInfo> {
        val label: (ResolveInfo) -> String = {
            it.loadLabel(packageManager).toString().lowercase()
        }
        return when (sortMode) {
            SORT_REVERSE -> apps.sortedByDescending(label)
            SORT_FAVORITES_FIRST -> apps.sortedWith(
                compareByDescending<ResolveInfo> { getAppId(it) in getCurrentFavorites() }
                    .thenBy(label)
            )
            else -> apps.sortedBy(label)
        }
    }

    private fun createAppTile(
        app: ResolveInfo,
        highlighted: Boolean,
        index: Int,
        animate: Boolean
    ): LinearLayout {
        val p = palette
        val label = app.loadLabel(packageManager).toString()
        val appId = getAppId(app)
        val isFavorite = appId in getCurrentFavorites()
        val tileHeight = if (compactMode) iconSizeDp + 47 else iconSizeDp + 64

        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(9), dp(5), dp(7))
            isClickable = true
            isFocusable = true
            background = roundedDrawable(
                if (highlighted) p.accentSoft else p.surfaceRaised,
                18,
                if (highlighted) p.accent else p.border,
                if (highlighted) 2 else 1
            )
            setOnClickListener { launchApp(app) }
            setOnLongClickListener {
                showAppActions(app)
                true
            }
        }

        val iconFrame = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = roundedDrawable(if (highlighted) p.surface else Color.TRANSPARENT, 16)
        }

        val icon = ImageView(this).apply {
            setImageDrawable(app.loadIcon(packageManager))
            contentDescription = label
            adjustViewBounds = true
        }
        iconFrame.addView(icon, squareParams(iconSizeDp))

        val name = TextView(this).apply {
            text = label
            textSize = labelSizeSp
            gravity = Gravity.CENTER
            setTextColor(p.textPrimary)
            maxLines = if (compactMode) 1 else 2
            setPadding(dp(2), dp(6), dp(2), 0)
        }

        tile.addView(iconFrame, squareParams(iconSizeDp + 8))
        tile.addView(name, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        if (isFavorite && !highlighted) {
            val badge = TextView(this).apply {
                text = "★"
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(p.accent)
            }
            tile.addView(badge, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(15)))
        }

        tile.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = dp(tileHeight)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(dp(4), dp(4), dp(4), dp(4))
        }

        if (animate && index < 16) {
            tile.alpha = 0f
            tile.translationY = dp(10).toFloat()
            tile.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 18L).coerceAtMost(220L))
                .setDuration(180L)
                .start()
        }

        return tile
    }

    private fun showAppActions(app: ResolveInfo) {
        val label = app.loadLabel(packageManager).toString()
        val appId = getAppId(app)
        val favorites = getCurrentFavorites()
        val favoriteAction = if (appId in favorites) "Remove from favorites" else "Add to favorites"

        AlertDialog.Builder(this)
            .setTitle(label)
            .setItems(arrayOf(favoriteAction, "Hide in ${currentModeName()}", "App details")) { _, which ->
                when (which) {
                    0 -> toggleFavorite(app)
                    1 -> hideApp(app)
                    2 -> openAppDetails(app)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleFavorite(app: ResolveInfo) {
        val appId = getAppId(app)
        val label = app.loadLabel(packageManager).toString()
        val favorites = getCurrentFavorites()
        val added = if (appId in favorites) {
            favorites.remove(appId)
            false
        } else {
            favorites.add(appId)
            true
        }
        saveCollections()
        renderFavorites(animate = true)
        renderApps(searchBox.text?.toString().orEmpty(), animate = false)
        Toast.makeText(this, if (added) "$label pinned" else "$label unpinned", Toast.LENGTH_SHORT).show()
    }

    private fun hideApp(app: ResolveInfo) {
        val appId = getAppId(app)
        val label = app.loadLabel(packageManager).toString()
        getCurrentHidden().add(appId)
        getCurrentFavorites().remove(appId)
        saveCollections()
        refreshEverything(animate = true)
        Toast.makeText(this, "$label hidden in ${currentModeName()}", Toast.LENGTH_SHORT).show()
    }

    private fun showHiddenAppsDialog() {
        val hidden = getCurrentHidden()
        val hiddenApps = sortedApps(allApps.filter { getAppId(it) in hidden })

        if (hiddenApps.isEmpty()) {
            Toast.makeText(this, "No hidden apps in ${currentModeName()}.", Toast.LENGTH_SHORT).show()
            return
        }

        val labels = hiddenApps.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("${currentModeName()} hidden apps")
            .setMessage("Tap an app to make it visible again.")
            .setItems(labels) { _, which ->
                hidden.remove(getAppId(hiddenApps[which]))
                saveCollections()
                refreshEverything(animate = true)
                Toast.makeText(this, "${labels[which]} restored", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Restore all") { _, _ ->
                hidden.clear()
                saveCollections()
                refreshEverything(animate = true)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showSortDialog() {
        val labels = arrayOf("A–Z", "Z–A", "Favorites first")
        AlertDialog.Builder(this)
            .setTitle("Sort apps")
            .setSingleChoiceItems(labels, sortMode) { dialog, which ->
                sortMode = which
                prefs.edit().putInt(KEY_SORT_MODE, sortMode).apply()
                updateModeHeader()
                renderFavorites(animate = false)
                renderApps(searchBox.text?.toString().orEmpty(), animate = true)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSettingsDialog() {
        val items = arrayOf(
            "Theme: ${if (darkTheme) "Dark" else "Light"}",
            "Grid columns: $gridColumns",
            "Icon size: $iconSizeDp dp",
            "Label size: ${labelSizeSp.toInt()} sp",
            "Compact tiles: ${if (compactMode) "On" else "Off"}",
            "Reset appearance"
        )

        AlertDialog.Builder(this)
            .setTitle("Miles settings")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        darkTheme = !darkTheme
                        saveAppearance()
                        refreshEverything(animate = true)
                    }
                    1 -> chooseGridColumns()
                    2 -> chooseIconSize()
                    3 -> chooseLabelSize()
                    4 -> {
                        compactMode = !compactMode
                        saveAppearance()
                        refreshEverything(animate = true)
                    }
                    5 -> confirmResetAppearance()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun chooseGridColumns() {
        val values = intArrayOf(3, 4, 5)
        val labels = values.map { "$it columns" }.toTypedArray()
        val checked = values.indexOf(gridColumns).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Grid size")
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                gridColumns = values[which]
                saveAppearance()
                refreshEverything(animate = true)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun chooseIconSize() {
        val values = intArrayOf(44, 54, 64, 72)
        val labels = arrayOf("Small", "Medium", "Large", "Extra large")
        val checked = values.indexOf(iconSizeDp).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Icon size")
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                iconSizeDp = values[which]
                saveAppearance()
                refreshEverything(animate = true)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun chooseLabelSize() {
        val values = floatArrayOf(9f, 11f, 12f, 14f)
        val labels = arrayOf("Small", "Medium", "Large", "Extra large")
        val checked = values.indexOf(labelSizeSp).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Label size")
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                labelSizeSp = values[which]
                saveAppearance()
                refreshEverything(animate = true)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmResetAppearance() {
        AlertDialog.Builder(this)
            .setTitle("Reset appearance?")
            .setMessage("Favorites and hidden apps will not be changed.")
            .setPositiveButton("Reset") { _, _ ->
                darkTheme = true
                gridColumns = 4
                iconSizeDp = 54
                labelSizeSp = 11f
                sortMode = SORT_ALPHABETICAL
                compactMode = false
                saveAppearance()
                refreshEverything(animate = true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveAppearance() {
        prefs.edit()
            .putBoolean(KEY_DARK_THEME, darkTheme)
            .putInt(KEY_GRID_COLUMNS, gridColumns)
            .putInt(KEY_ICON_SIZE, iconSizeDp)
            .putFloat(KEY_LABEL_SIZE, labelSizeSp)
            .putInt(KEY_SORT_MODE, sortMode)
            .putBoolean(KEY_COMPACT_MODE, compactMode)
            .apply()
    }

    private fun saveCollections() {
        prefs.edit()
            .putStringSet(KEY_WORK_FAVORITES, HashSet(workFavorites))
            .putStringSet(KEY_HOME_FAVORITES, HashSet(homeFavorites))
            .putStringSet(KEY_WORK_HIDDEN, HashSet(workHidden))
            .putStringSet(KEY_HOME_HIDDEN, HashSet(homeHidden))
            .apply()
    }

    private fun openAppDetails(app: ResolveInfo) {
        try {
            startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${app.activityInfo.packageName}")
                }
            )
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open app details.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchApp(app: ResolveInfo) {
        val info = app.activityInfo
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(info.packageName, info.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(launchIntent)
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open this app.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadLaunchableApps(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager
            .queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy(::getAppId)
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
    }

    private fun getCurrentFavorites(): MutableSet<String> =
        if (workMode) workFavorites else homeFavorites

    private fun getCurrentHidden(): MutableSet<String> =
        if (workMode) workHidden else homeHidden

    private fun currentModeName(): String = if (workMode) "Work" else "Home"

    private fun getAppId(app: ResolveInfo): String =
        "${app.activityInfo.packageName}/${app.activityInfo.name}"

    private fun newGrid(): GridLayout = GridLayout(this).apply {
        columnCount = gridColumns
        alignmentMode = GridLayout.ALIGN_BOUNDS
        useDefaultMargins = false
    }

    private fun sectionTitle(): TextView = TextView(this).apply {
        textSize = 18f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(2), 0, dp(2), dp(6))
    }

    private fun roundedDrawable(
        fillColor: Int,
        radiusDp: Int,
        strokeColor: Int? = null,
        strokeWidthDp: Int = 0
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radiusDp).toFloat()
        setColor(fillColor)
        if (strokeColor != null && strokeWidthDp > 0) {
            setStroke(dp(strokeWidthDp), strokeColor)
        }
    }

    private fun weightedParams(leftMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
            if (leftMargin > 0) this.leftMargin = dp(leftMargin)
        }

    private fun squareParams(sizeDp: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))

    private fun topMarginParams(topMarginDp: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(topMarginDp)
        }

    private fun hideKeyboard() {
        val manager = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        manager?.hideSoftInputFromWindow(searchBox.windowToken, 0)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val PREFS_NAME = "miles_launcher"
        private const val KEY_WORK_MODE = "work_mode"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_ICON_SIZE = "icon_size"
        private const val KEY_LABEL_SIZE = "label_size"
        private const val KEY_SORT_MODE = "sort_mode"
        private const val KEY_COMPACT_MODE = "compact_mode"
        private const val KEY_WORK_FAVORITES = "work_favorites"
        private const val KEY_HOME_FAVORITES = "home_favorites"
        private const val KEY_WORK_HIDDEN = "work_hidden"
        private const val KEY_HOME_HIDDEN = "home_hidden"

        private const val SORT_ALPHABETICAL = 0
        private const val SORT_REVERSE = 1
        private const val SORT_FAVORITES_FIRST = 2
    }
}
