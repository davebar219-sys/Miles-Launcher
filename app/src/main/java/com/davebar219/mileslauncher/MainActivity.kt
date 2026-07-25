package com.davebar219.mileslauncher

// Miles Launcher V3.6 — Step 2: corrected Fold 6 floating app bar and fast profile switching.

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.res.Configuration
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.text.TextUtils
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Suppress("DEPRECATION")
class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var rootFrame: FrameLayout
    private lateinit var launcherRoot: LinearLayout
    private lateinit var headerCard: LinearLayout
    private lateinit var contentCard: LinearLayout
    private lateinit var logoView: MilesLogoView
    private lateinit var modeTitle: TextView
    private lateinit var modeSubtitle: TextView
    private lateinit var dateText: TextView
    private lateinit var timeText: TextView
    private lateinit var workButton: Button
    private lateinit var homeButton: Button
    private lateinit var searchBox: EditText
    private lateinit var clearSearchButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var hiddenButton: Button
    private lateinit var sortButton: Button
    private lateinit var favoritesTitle: TextView
    private lateinit var allAppsTitle: TextView
    private lateinit var favoritesGrid: GridLayout
    private lateinit var appGrid: GridLayout
    private lateinit var emptyAppsText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var statusPill: TextView
    private lateinit var assistantCard: LinearLayout
    private lateinit var assistantGreeting: TextView
    private lateinit var assistantStatus: TextView
    private lateinit var voiceButton: TextView
    private lateinit var assistantLogo: MilesRobotView
    private lateinit var bottomDock: LinearLayout
    private lateinit var navHome: TextView
    private lateinit var navFavorites: TextView
    private lateinit var navMiles: TextView
    private lateinit var navAi: TextView
    private lateinit var navSettings: TextView

    private var profileTransitionRunning = false

    private var systemInsetLeft = 0
    private var systemInsetTop = 0
    private var systemInsetRight = 0
    private var systemInsetBottom = 0

    private var allApps: List<ResolveInfo> = emptyList()
    private var workMode = true
    private var darkTheme = true
    private var showLabels = true
    private var compactMode = false
    private var startupEnabled = true
    private var hapticsEnabled = true
    private var gridColumns = 4
    private var iconSizeDp = 54
    private var labelSizeSp = 11f
    private var sortMode = SORT_ALPHABETICAL

    private val workFavorites = mutableSetOf<String>()
    private val homeFavorites = mutableSetOf<String>()
    private val workHidden = mutableSetOf<String>()
    private val homeHidden = mutableSetOf<String>()

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 1_000L)
        }
    }

    private data class Palette(
        val background: Int,
        val surface: Int,
        val surfaceRaised: Int,
        val surfaceStrong: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val accent: Int,
        val accentStrong: Int,
        val accentSoft: Int,
        val border: Int,
        val danger: Int,
        val shadow: Int
    )

    private val palette: Palette
        get() = if (darkTheme) {
            Palette(
                background = Color.rgb(8, 11, 18),
                surface = Color.rgb(18, 22, 33),
                surfaceRaised = Color.rgb(26, 31, 45),
                surfaceStrong = Color.rgb(34, 41, 59),
                textPrimary = Color.rgb(247, 249, 255),
                textSecondary = Color.rgb(167, 178, 201),
                accent = if (workMode) Color.rgb(34, 132, 255) else Color.rgb(0, 180, 255),
                accentStrong = if (workMode) Color.rgb(0, 91, 224) else Color.rgb(0, 120, 224),
                accentSoft = if (workMode) Color.rgb(18, 49, 88) else Color.rgb(10, 55, 78),
                border = Color.rgb(48, 57, 78),
                danger = Color.rgb(244, 100, 120),
                shadow = Color.argb(90, 0, 0, 0)
            )
        } else {
            Palette(
                background = Color.rgb(239, 244, 252),
                surface = Color.WHITE,
                surfaceRaised = Color.rgb(247, 249, 253),
                surfaceStrong = Color.rgb(232, 237, 247),
                textPrimary = Color.rgb(23, 28, 40),
                textSecondary = Color.rgb(88, 99, 122),
                accent = if (workMode) Color.rgb(28, 105, 230) else Color.rgb(0, 145, 214),
                accentStrong = if (workMode) Color.rgb(16, 75, 184) else Color.rgb(0, 103, 174),
                accentSoft = if (workMode) Color.rgb(217, 233, 255) else Color.rgb(214, 243, 255),
                border = Color.rgb(213, 220, 234),
                danger = Color.rgb(197, 49, 72),
                shadow = Color.argb(25, 20, 28, 45)
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        loadPreferences()
        allApps = loadLaunchableApps()
        buildUi()
        applyWindowInsets()
        refreshEverything(animate = false)
        if (startupEnabled) showStartupExperience() else revealLauncherImmediately()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!::rootFrame.isInitialized) return
        rootFrame.post {
            applyResponsiveLayout()
            renderFavorites(animate = false)
            renderApps(searchBox.text?.toString().orEmpty(), animate = false)
        }
    }

    override fun onResume() {
        super.onResume()
        clockHandler.removeCallbacks(clockRunnable)
        clockHandler.post(clockRunnable)
        val latestApps = loadLaunchableApps()
        val oldIds = allApps.map(::getAppId)
        val newIds = latestApps.map(::getAppId)
        if (oldIds != newIds) {
            allApps = latestApps
            cleanMissingPreferences()
            refreshEverything(animate = false)
        }
    }

    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockRunnable)
    }

    override fun onBackPressed() {
        when {
            searchBox.text?.isNotEmpty() == true -> {
                searchBox.setText("")
                searchBox.clearFocus()
                hideKeyboard()
                scrollView.smoothScrollTo(0, 0)
            }
            else -> super.onBackPressed()
        }
    }

    private fun applyWindowInsets() {
        if (!::rootFrame.isInitialized) return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        rootFrame.setOnApplyWindowInsetsListener { _, insets ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val statusAndCutout = insets.getInsets(
                    WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()
                )
                val navigation = insets.getInsets(WindowInsets.Type.navigationBars())
                systemInsetLeft = max(statusAndCutout.left, navigation.left)
                systemInsetTop = statusAndCutout.top
                systemInsetRight = max(statusAndCutout.right, navigation.right)
                systemInsetBottom = navigation.bottom
            } else {
                @Suppress("DEPRECATION")
                systemInsetLeft = insets.systemWindowInsetLeft
                @Suppress("DEPRECATION")
                systemInsetTop = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                systemInsetRight = insets.systemWindowInsetRight
                @Suppress("DEPRECATION")
                systemInsetBottom = insets.systemWindowInsetBottom
            }

            applyResponsiveLayout()
            insets
        }
        rootFrame.requestApplyInsets()
    }

    private fun applyResponsiveLayout() {
        if (!::launcherRoot.isInitialized) return

        val innerDisplay = isInnerDisplay()
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val dockHeight = dp(
            when {
                innerDisplay && landscape -> 64
                innerDisplay -> 68
                landscape -> 60
                else -> 64
            }
        )
        val dockSideMargin = dp(if (innerDisplay) 24 else 12)
        val dockBottomGap = dp(if (systemInsetBottom > 0) 6 else 10)

        // Only system top/side insets belong on the launcher root. Reserving the dock and
        // navigation inset here compressed the whole page and created the oversized bottom gap.
        launcherRoot.setPadding(
            systemInsetLeft + dp(14),
            systemInsetTop + dp(10),
            systemInsetRight + dp(14),
            dp(8)
        )

        if (::bottomDock.isInitialized) {
            val frameWidth = rootFrame.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
            val availableWidth = (frameWidth - systemInsetLeft - systemInsetRight - dockSideMargin * 2)
                .coerceAtLeast(dp(280))
            val maximumDockWidth = when {
                innerDisplay && landscape -> dp(660)
                innerDisplay -> dp(620)
                else -> availableWidth
            }
            val targetWidth = min(availableWidth, maximumDockWidth)

            val params = (bottomDock.layoutParams as? FrameLayout.LayoutParams)
                ?: FrameLayout.LayoutParams(targetWidth, dockHeight)
            params.width = targetWidth
            params.height = dockHeight
            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            params.leftMargin = systemInsetLeft
            params.rightMargin = systemInsetRight
            params.bottomMargin = systemInsetBottom + dockBottomGap
            bottomDock.layoutParams = params
            bottomDock.bringToFront()
        }

        if (::scrollView.isInitialized) {
            // The bar overlays the page. Reserve exactly its visual footprint inside the scroller,
            // including Samsung's navigation/gesture inset, so the final app row clears the bar.
            val bottomClearance = systemInsetBottom + dockBottomGap + dockHeight + dp(14)
            scrollView.setPadding(0, 0, 0, bottomClearance)
            scrollView.clipToPadding = false
            scrollView.isScrollbarFadingEnabled = true
        }

        if (::favoritesGrid.isInitialized) favoritesGrid.columnCount = resolvedGridColumns()
        if (::appGrid.isInitialized) appGrid.columnCount = resolvedGridColumns()
    }

    private fun isInnerDisplay(): Boolean = resources.configuration.smallestScreenWidthDp >= 600

    private fun resolvedGridColumns(): Int {
        val configuration = resources.configuration
        val widthDp = configuration.screenWidthDp.takeIf { it > 0 }
            ?: (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt()
        val base = when {
            widthDp >= 840 -> 9
            widthDp >= 600 -> 7
            else -> 5
        }
        // The existing 3–5 column preference now acts as a density adjustment.
        return (base + (gridColumns - 4)).coerceIn(4, 10)
    }

    private fun scheduleSystemBarUpdate() {
        if (!::rootFrame.isInitialized) return
        rootFrame.post { configureSystemBars() }
    }

    private fun configureSystemBars() {
        val navColor = if (darkTheme) Color.rgb(8, 11, 18) else Color.rgb(239, 244, 252)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = navColor

        val decorView = window.decorView
        if (!decorView.isAttachedToWindow) {
            decorView.post { configureSystemBars() }
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val controller = decorView.windowInsetsController ?: return
            val lightAppearance = if (darkTheme) {
                0
            } else {
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            }
            val appearanceMask =
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            controller.setSystemBarsAppearance(lightAppearance, appearanceMask)
        } else {
            decorView.systemUiVisibility = if (darkTheme) {
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            } else {
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    private fun loadPreferences() {
        workMode = prefs.getBoolean(KEY_WORK_MODE, true)
        darkTheme = prefs.getBoolean(KEY_DARK_THEME, true)
        showLabels = prefs.getBoolean(KEY_SHOW_LABELS, true)
        compactMode = prefs.getBoolean(KEY_COMPACT_MODE, false)
        startupEnabled = prefs.getBoolean(KEY_STARTUP_ENABLED, true)
        hapticsEnabled = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        gridColumns = prefs.getInt(KEY_GRID_COLUMNS, 4).coerceIn(3, 5)
        iconSizeDp = prefs.getInt(KEY_ICON_SIZE, 54).coerceIn(42, 72)
        labelSizeSp = prefs.getFloat(KEY_LABEL_SIZE, 11f).coerceIn(9f, 14f)
        sortMode = prefs.getInt(KEY_SORT_MODE, SORT_ALPHABETICAL).coerceIn(0, 2)

        workFavorites.addAll(prefs.getStringSet(KEY_WORK_FAVORITES, emptySet()) ?: emptySet())
        homeFavorites.addAll(prefs.getStringSet(KEY_HOME_FAVORITES, emptySet()) ?: emptySet())
        workHidden.addAll(prefs.getStringSet(KEY_WORK_HIDDEN, emptySet()) ?: emptySet())
        homeHidden.addAll(prefs.getStringSet(KEY_HOME_HIDDEN, emptySet()) ?: emptySet())
    }

    private fun buildUi() {
        rootFrame = ProfileSwipeFrameLayout(this).apply {
            setBackgroundColor(palette.background)
            onSwipeLeft = { switchMode(true, swipeDirection = -1) }
            onSwipeRight = { switchMode(false, swipeDirection = 1) }
        }

        launcherRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(22), dp(14), dp(8)) // updated by applyWindowInsets()
            alpha = 0f
            scaleX = 0.985f
            scaleY = 0.985f
        }

        headerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(16), dp(17), dp(16))
            elevation = dp(4).toFloat()
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        logoView = MilesLogoView(this).apply {
            contentDescription = "Animated futuristic Miles monogram"
        }

        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), 0, 0, 0)
        }

        modeTitle = TextView(this).apply {
            textSize = 25f
            setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
        }

        modeSubtitle = TextView(this).apply {
            textSize = 12.5f
            setPadding(0, dp(3), 0, 0)
            includeFontPadding = false
        }

        titleColumn.addView(modeTitle)
        titleColumn.addView(modeSubtitle)

        settingsButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_preferences)
            contentDescription = "Launcher settings"
            setPadding(dp(11), dp(11), dp(11), dp(11))
            setOnClickListener {
                haptic(this)
                showSettingsDialog()
            }
        }

        topRow.addView(logoView, squareParams(52))
        topRow.addView(titleColumn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        topRow.addView(settingsButton, squareParams(46))

        assistantCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(14), dp(12), dp(14))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                haptic(this)
                startVoiceInput()
            }
            setOnLongClickListener {
                haptic(this, strong = true)
                openChatGpt()
                true
            }
        }

        assistantLogo = MilesRobotView(this).apply {
            contentDescription = "Animated Miles AI assistant"
        }

        val assistantText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), 0, dp(8), 0)
        }

        assistantGreeting = TextView(this).apply {
            text = "Hey, I'm Miles."
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
        }

        assistantStatus = TextView(this).apply {
            text = "Tap to talk • Hold to open ChatGPT"
            textSize = 12.5f
            setPadding(0, dp(4), 0, 0)
            includeFontPadding = false
        }

        assistantText.addView(assistantGreeting)
        assistantText.addView(assistantStatus)

        voiceButton = TextView(this).apply {
            text = "●"
            textSize = 26f
            gravity = Gravity.CENTER
            contentDescription = "Talk to Miles"
            setOnClickListener {
                haptic(this)
                startVoiceInput()
            }
        }

        assistantCard.addView(assistantLogo, squareParams(92))
        assistantCard.addView(assistantText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        assistantCard.addView(voiceButton, squareParams(50))

        val clockRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(15), dp(2), 0)
        }

        dateText = TextView(this).apply {
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
        }

        timeText = TextView(this).apply {
            textSize = 13f
            gravity = Gravity.END
            setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD))
        }

        clockRow.addView(dateText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        clockRow.addView(timeText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

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
            stateListAnimator = null
            setOnClickListener {
                haptic(this)
                switchMode(true)
            }
        }

        homeButton = Button(this).apply {
            text = "Home"
            isAllCaps = false
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            stateListAnimator = null
            setOnClickListener {
                haptic(this)
                switchMode(false)
            }
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
                    if (::clearSearchButton.isInitialized) {
                        clearSearchButton.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                    }
                    if (::appGrid.isInitialized) {
                        renderApps(s?.toString().orEmpty(), animate = true)
                    }
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
            assistantCard,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(14)
            }
        )
        headerCard.addView(clockRow)
        headerCard.addView(
            modeRow,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply {
                topMargin = dp(14)
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
            setPadding(dp(13), dp(11), dp(13), dp(16))
            elevation = dp(3).toFloat()
        }

        val quickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        statusPill = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(12), dp(9), dp(12), dp(9))
        }

        hiddenButton = Button(this).apply {
            isAllCaps = false
            textSize = 12.5f
            stateListAnimator = null
            setOnClickListener {
                haptic(this)
                showHiddenAppsDialog()
            }
        }

        sortButton = Button(this).apply {
            text = "Sort"
            isAllCaps = false
            textSize = 12.5f
            stateListAnimator = null
            setOnClickListener {
                haptic(this)
                showSortDialog()
            }
        }

        quickRow.addView(statusPill, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        quickRow.addView(hiddenButton, LinearLayout.LayoutParams(dp(96), dp(42)).apply { leftMargin = dp(8) })
        quickRow.addView(sortButton, LinearLayout.LayoutParams(dp(76), dp(42)).apply { leftMargin = dp(8) })

        favoritesTitle = sectionTitle()
        favoritesGrid = newGrid()
        allAppsTitle = sectionTitle()
        emptyAppsText = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(34), dp(12), dp(34))
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
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(scrollContent)
        }

        scrollView.clipToPadding = false
        scrollView.setPadding(0, 0, 0, dp(12))
        contentCard.addView(scrollView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        fun dockItem(symbol: String, label: String, action: () -> Unit): TextView = TextView(this).apply {
            text = "$symbol\n$label"
            gravity = Gravity.CENTER
            textSize = if (resources.configuration.smallestScreenWidthDp >= 600) 11f else 9.5f
            maxLines = 2
            includeFontPadding = false
            setLineSpacing(0f, 0.92f)
            setPadding(dp(2), dp(4), dp(2), dp(4))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                haptic(this)
                action()
            }
        }

        bottomDock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(3), dp(8), dp(3))
            elevation = dp(12).toFloat()
            clipChildren = false
            clipToPadding = false
            isClickable = true
            isFocusable = true
        }
        navHome = dockItem("⌂", "Home") { switchMode(false) }
        navFavorites = dockItem("☆", "Favorites") {
            searchBox.setText("")
            scrollView.smoothScrollTo(0, 0)
        }
        navMiles = dockItem("◉", "Miles") { startVoiceInput() }.apply {
            textSize = if (resources.configuration.smallestScreenWidthDp >= 600) 12f else 10.5f
        }
        navAi = dockItem("✦", "AI Tools") { openChatGpt() }
        navSettings = dockItem("⚙", "Settings") { showSettingsDialog() }

        val dockItemHeight = if (resources.configuration.smallestScreenWidthDp >= 600) dp(58) else dp(54)
        bottomDock.addView(navHome, LinearLayout.LayoutParams(0, dockItemHeight, 1f))
        bottomDock.addView(navFavorites, LinearLayout.LayoutParams(0, dockItemHeight, 1f))
        bottomDock.addView(navMiles, LinearLayout.LayoutParams(0, dockItemHeight, 1.12f))
        bottomDock.addView(navAi, LinearLayout.LayoutParams(0, dockItemHeight, 1f))
        bottomDock.addView(navSettings, LinearLayout.LayoutParams(0, dockItemHeight, 1f))

        launcherRoot.addView(headerCard)
        launcherRoot.addView(
            contentCard,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = dp(11)
            }
        )
        rootFrame.addView(
            CyberBackdropView(this),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        rootFrame.addView(
            launcherRoot,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        rootFrame.addView(
            bottomDock,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                if (resources.configuration.smallestScreenWidthDp >= 600) dp(68) else dp(64),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                bottomMargin = dp(10)
            }
        )
        setContentView(rootFrame)
    }

    private fun showStartupExperience() {
        val p = palette
        val splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
            setBackgroundColor(p.background)
            alpha = 1f
        }

        val splashLogo = MilesLogoView(this).apply {
            setAccentColors(p.accent, p.accentStrong)
            scaleX = 0.55f
            scaleY = 0.55f
            alpha = 0f
        }

        val brand = TextView(this).apply {
            text = "MILES"
            textSize = 30f
            letterSpacing = 0.16f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(p.textPrimary)
            alpha = 0f
            translationY = dp(10).toFloat()
        }

        val tagline = TextView(this).apply {
            text = "Neural interface initializing…"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(p.textSecondary)
            setPadding(0, dp(9), 0, 0)
            alpha = 0f
        }

        splash.addView(splashLogo, LinearLayout.LayoutParams(dp(126), dp(126)))
        splash.addView(brand, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(16)
        })
        splash.addView(tagline)
        rootFrame.addView(splash, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val logoIn = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(splashLogo, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(splashLogo, View.SCALE_X, 0.55f, 1f),
                ObjectAnimator.ofFloat(splashLogo, View.SCALE_Y, 0.55f, 1f),
                ObjectAnimator.ofFloat(splashLogo, View.ROTATION, -18f, 0f)
            )
            duration = 520L
        }

        val textIn = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(brand, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(brand, View.TRANSLATION_Y, dp(10).toFloat(), 0f),
                ObjectAnimator.ofFloat(tagline, View.ALPHA, 0f, 1f)
            )
            duration = 420L
            startDelay = 260L
        }

        logoIn.start()
        textIn.start()

        splash.postDelayed({
            launcherRoot.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(420L)
                .start()
            splash.animate()
                .alpha(0f)
                .scaleX(1.04f)
                .scaleY(1.04f)
                .setDuration(380L)
                .withEndAction {
                    rootFrame.removeView(splash)
                    animateAssistantEntrance()
                }
                .start()
        }, 1_250L)
    }

    private fun revealLauncherImmediately() {
        launcherRoot.alpha = 1f
        launcherRoot.scaleX = 1f
        launcherRoot.scaleY = 1f
        animateAssistantEntrance()
    }

    private fun animateAssistantEntrance() {
        if (!::assistantCard.isInitialized) return
        assistantCard.alpha = 0f
        assistantCard.translationY = dp(12).toFloat()
        assistantCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(420L)
            .start()
        assistantLogo.playGreeting()
        pulseVoiceButton()
    }

    private fun pulseVoiceButton() {
        if (!::voiceButton.isInitialized) return
        ValueAnimator.ofFloat(1f, 1.12f, 1f).apply {
            duration = 1500L
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                voiceButton.scaleX = value
                voiceButton.scaleY = value
                voiceButton.alpha = 0.72f + (value - 1f) * 1.8f
            }
            start()
        }
    }

    private fun switchMode(toWorkMode: Boolean, swipeDirection: Int = if (toWorkMode) -1 else 1) {
        if (workMode == toWorkMode || profileTransitionRunning) return
        profileTransitionRunning = true
        hideKeyboard()

        val direction = if (swipeDirection < 0) -1 else 1
        val travel = direction * dp(14).toFloat()
        val interpolator = DecelerateInterpolator(1.6f)

        contentCard.animate().cancel()
        modeTitle.animate().cancel()
        modeSubtitle.animate().cancel()
        assistantCard.animate().cancel()

        // A short single-stage fade makes the synchronous grid refresh feel immediate,
        // without the old long slide-out / slide-in sequence.
        contentCard.animate()
            .translationX(travel)
            .alpha(0.58f)
            .setDuration(PROFILE_OUT_MS)
            .setInterpolator(interpolator)
            .withEndAction {
                workMode = toWorkMode
                prefs.edit().putBoolean(KEY_WORK_MODE, workMode).apply()
                if (searchBox.text?.isNotEmpty() == true) searchBox.setText("")
                scrollView.scrollTo(0, 0)
                refreshEverything(animate = false)

                contentCard.translationX = -travel * 0.55f
                contentCard.alpha = 0.72f
                modeTitle.translationX = -travel * 0.18f
                modeSubtitle.translationX = -travel * 0.12f

                assistantGreeting.text = if (workMode) "Work mode activated." else "Welcome home."
                assistantStatus.text = if (workMode) {
                    "Miles is focused and ready."
                } else {
                    "Miles is ready for your personal apps."
                }
                assistantLogo.playProfileSwitch(direction)

                contentCard.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(PROFILE_IN_MS)
                    .setInterpolator(interpolator)
                    .withEndAction {
                        profileTransitionRunning = false
                        restoreAssistantPromptSoon()
                    }
                    .start()
                modeTitle.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(PROFILE_IN_MS)
                    .setInterpolator(interpolator)
                    .start()
                modeSubtitle.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(PROFILE_IN_MS)
                    .setInterpolator(interpolator)
                    .start()
            }
            .start()
    }

    private fun restoreAssistantPromptSoon() {
        assistantCard.removeCallbacks(restoreAssistantPromptRunnable)
        assistantCard.postDelayed(restoreAssistantPromptRunnable, 1_250L)
    }

    private val restoreAssistantPromptRunnable = Runnable {
        if (!::assistantGreeting.isInitialized) return@Runnable
        assistantGreeting.text = greetingForCurrentTime()
        assistantStatus.text = "Tap to talk • Hold to open ChatGPT"
    }

    private fun greetingForCurrentTime(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning. I'm Miles."
            in 12..16 -> "Good afternoon. I'm Miles."
            else -> "Good evening. I'm Miles."
        }
    }

    private fun refreshEverything(animate: Boolean) {
        applyTheme()
        scheduleSystemBarUpdate()
        updateModeHeader()
        renderFavorites(animate)
        renderApps(searchBox.text?.toString().orEmpty(), animate)
    }

    private fun applyTheme() {
        val p = palette
        rootFrame.setBackgroundColor(p.background)
        launcherRoot.setBackgroundColor(Color.TRANSPARENT)
        headerCard.background = neonPanelDrawable(p.surface, p.surfaceRaised, 26, p.accent)
        contentCard.background = neonPanelDrawable(p.surface, p.background, 26, p.accentStrong)
        bottomDock.background = neonPanelDrawable(p.surfaceRaised, p.background, 24, p.accent)

        logoView.setAccentColors(p.accent, p.accentStrong)
        assistantLogo.setAccentColors(p.accent, p.accentStrong)
        assistantCard.background = neonPanelDrawable(p.surfaceRaised, p.accentSoft, 22, p.accent)
        assistantGreeting.setTextColor(p.textPrimary)
        assistantStatus.setTextColor(p.textSecondary)
        voiceButton.setTextColor(p.accent)
        voiceButton.background = roundedDrawable(p.accentSoft, 25, p.accent, 1)
        modeTitle.setTextColor(p.textPrimary)
        modeSubtitle.setTextColor(p.textSecondary)
        dateText.setTextColor(p.textSecondary)
        timeText.setTextColor(p.textPrimary)
        favoritesTitle.setTextColor(p.textPrimary)
        allAppsTitle.setTextColor(p.textPrimary)
        emptyAppsText.setTextColor(p.textSecondary)

        settingsButton.background = roundedDrawable(p.surfaceRaised, 15, p.border, 1)
        settingsButton.setColorFilter(p.textPrimary)
        clearSearchButton.background = roundedDrawable(Color.TRANSPARENT, 14)
        clearSearchButton.setColorFilter(p.textSecondary)

        (searchBox.parent as? View)?.background = roundedDrawable(p.surfaceRaised, 18, p.border, 1)
        searchBox.setTextColor(p.textPrimary)
        searchBox.setHintTextColor(p.textSecondary)

        statusPill.setTextColor(p.accent)
        statusPill.background = roundedDrawable(p.accentSoft, 14)

        hiddenButton.setTextColor(p.textPrimary)
        hiddenButton.background = roundedDrawable(p.surfaceRaised, 15, p.border, 1)
        sortButton.setTextColor(p.textPrimary)
        sortButton.background = roundedDrawable(p.surfaceRaised, 15, p.border, 1)

        val dockItems = listOf(navHome, navFavorites, navMiles, navAi, navSettings)
        dockItems.forEach {
            it.setTextColor(p.textSecondary)
            it.background = roundedDrawable(Color.TRANSPARENT, 16)
        }
        val selectedDock = if (workMode) navAi else navHome
        selectedDock.setTextColor(p.accent)
        selectedDock.background = roundedDrawable(p.accentSoft, 16, p.accent, 1)
        navMiles.setTextColor(Color.WHITE)
        navMiles.background = neonPanelDrawable(p.accentStrong, p.accentSoft, 28, p.accent)
        navMiles.elevation = dp(7).toFloat()

        updateModeButtons()
    }

    private fun updateModeHeader() {
        modeTitle.text = if (workMode) "Miles · Work" else "Miles · Home"
        modeSubtitle.text = if (workMode) {
            "Focus mode • Miles is ready"
        } else {
            "Your personal space • Miles is ready"
        }

        val visibleCount = getVisibleApps("").size
        statusPill.text = "$visibleCount apps"
        hiddenButton.text = "Hidden ${getCurrentHidden().size}"
        favoritesTitle.text = if (workMode) "Work favorites" else "Home favorites"
        allAppsTitle.text = when (sortMode) {
            SORT_REVERSE -> "All apps · Z–A"
            SORT_FAVORITES_FIRST -> "All apps · Favorites first"
            else -> "All apps · A–Z"
        }
        updateClock()
    }

    private fun updateClock() {
        if (!::timeText.isInitialized) return
        val now = Date()
        val timePattern = if (android.text.format.DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
        timeText.text = SimpleDateFormat(timePattern, Locale.getDefault()).format(now)
        dateText.text = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now)
    }

    private fun updateModeButtons() {
        val p = palette
        val selectedText = Color.WHITE
        val idleText = p.textSecondary

        if (workMode) {
            workButton.setTextColor(selectedText)
            workButton.background = roundedDrawable(p.accentStrong, 14)
            homeButton.setTextColor(idleText)
            homeButton.background = roundedDrawable(Color.TRANSPARENT, 14)
        } else {
            homeButton.setTextColor(selectedText)
            homeButton.background = roundedDrawable(p.accentStrong, 14)
            workButton.setTextColor(idleText)
            workButton.background = roundedDrawable(Color.TRANSPARENT, 14)
        }
        (workButton.parent as? View)?.background = roundedDrawable(p.surfaceRaised, 17, p.border, 1)
    }

    private fun renderFavorites(animate: Boolean) {
        favoritesGrid.removeAllViews()
        favoritesGrid.columnCount = resolvedGridColumns()
        val favorites = getCurrentFavorites()
        val hidden = getCurrentHidden()
        val favoriteApps = allApps
            .filter { getAppId(it) in favorites && getAppId(it) !in hidden }
            .sortedBy { appLabel(it).lowercase(Locale.getDefault()) }

        favoritesTitle.visibility = if (favoriteApps.isEmpty()) View.GONE else View.VISIBLE
        favoritesGrid.visibility = if (favoriteApps.isEmpty()) View.GONE else View.VISIBLE

        favoriteApps.forEachIndexed { index, app ->
            favoritesGrid.addView(createAppTile(app, isFavoriteSection = true, index = index, animate = animate))
        }
    }

    private fun renderApps(query: String, animate: Boolean) {
        appGrid.removeAllViews()
        appGrid.columnCount = resolvedGridColumns()

        val apps = getVisibleApps(query)
        statusPill.text = if (query.isBlank()) "${apps.size} apps" else "${apps.size} found"

        emptyAppsText.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
        appGrid.visibility = if (apps.isEmpty()) View.GONE else View.VISIBLE
        emptyAppsText.text = when {
            query.isNotBlank() -> "No apps match “$query”."
            getCurrentHidden().isNotEmpty() -> "Every app in this profile is hidden."
            else -> "No launchable apps were found."
        }

        apps.forEachIndexed { index, app ->
            appGrid.addView(createAppTile(app, isFavoriteSection = false, index = index, animate = animate))
        }
    }

    private fun getVisibleApps(query: String): List<ResolveInfo> {
        val hidden = getCurrentHidden()
        val favorites = getCurrentFavorites()
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())

        val filtered = allApps.filter { app ->
            val id = getAppId(app)
            val label = appLabel(app).lowercase(Locale.getDefault())
            val packageName = app.activityInfo.packageName.lowercase(Locale.getDefault())
            id !in hidden && (normalizedQuery.isBlank() || label.contains(normalizedQuery) || packageName.contains(normalizedQuery))
        }

        return when (sortMode) {
            SORT_REVERSE -> filtered.sortedByDescending { appLabel(it).lowercase(Locale.getDefault()) }
            SORT_FAVORITES_FIRST -> filtered.sortedWith(
                compareByDescending<ResolveInfo> { getAppId(it) in favorites }
                    .thenBy { appLabel(it).lowercase(Locale.getDefault()) }
            )
            else -> filtered.sortedBy { appLabel(it).lowercase(Locale.getDefault()) }
        }
    }

    private fun createAppTile(
        app: ResolveInfo,
        isFavoriteSection: Boolean,
        index: Int,
        animate: Boolean
    ): View {
        val p = palette
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setPadding(dp(4), dp(if (compactMode) 7 else 10), dp(4), dp(if (compactMode) 7 else 10))
            background = neonPanelDrawable(
                if (isFavoriteSection) p.accentSoft else p.surfaceRaised,
                if (isFavoriteSection) p.surfaceRaised else p.background,
                18,
                if (isFavoriteSection) p.accent else Color.argb(210, Color.red(p.accent), Color.green(p.accent), Color.blue(p.accent))
            )
            elevation = dp(if (isFavoriteSection) 2 else 1).toFloat()
            contentDescription = appLabel(app)
        }

        val icon = ImageView(this).apply {
            try {
                setImageDrawable(app.loadIcon(packageManager))
            } catch (_: Exception) {
                setImageResource(android.R.drawable.sym_def_app_icon)
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            adjustViewBounds = true
        }

        tile.addView(icon, LinearLayout.LayoutParams(dp(iconSizeDp), dp(iconSizeDp)))

        if (showLabels) {
            val label = TextView(this).apply {
                text = appLabel(app)
                textSize = labelSizeSp
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(p.textPrimary)
                setPadding(dp(2), dp(if (compactMode) 4 else 7), dp(2), 0)
                includeFontPadding = false
            }
            tile.addView(label, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        tile.setOnClickListener {
            haptic(tile)
            tile.animate().scaleX(0.93f).scaleY(0.93f).setDuration(70L).withEndAction {
                tile.animate().scaleX(1f).scaleY(1f).setDuration(90L).start()
                launchApp(app)
            }.start()
        }

        tile.setOnLongClickListener {
            haptic(tile, strong = true)
            showAppActions(app)
            true
        }

        if (animate && index < 24) {
            tile.alpha = 0f
            tile.translationY = dp(12).toFloat()
            tile.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 18L).coerceAtMost(250L))
                .setDuration(210L)
                .start()
        }

        val spacing = dp(if (compactMode) 3 else 4)
        val columns = resolvedGridColumns()
        val measuredGridWidth = appGrid.width.takeIf { it > 0 } ?: favoritesGrid.width.takeIf { it > 0 }
        val fallbackWidth = resources.displayMetrics.widthPixels - systemInsetLeft - systemInsetRight - dp(54)
        val availableWidth = (measuredGridWidth ?: fallbackWidth).coerceAtLeast(dp(320))
        val cellWidth = ((availableWidth - spacing * 2 * columns) / columns).coerceAtLeast(dp(60))
        return tile.apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = cellWidth
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                setMargins(spacing, spacing, spacing, spacing)
                setGravity(Gravity.FILL_HORIZONTAL)
            }
        }
    }

    private fun showAppActions(app: ResolveInfo) {
        val id = getAppId(app)
        val favorites = getCurrentFavorites()
        val favoriteAction = if (id in favorites) "Remove from favorites" else "Add to favorites"
        val actions = arrayOf(
            "Open",
            favoriteAction,
            "Hide from ${currentModeName()}",
            "App info",
            "Uninstall"
        )

        AlertDialog.Builder(this)
            .setTitle(appLabel(app))
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> launchApp(app)
                    1 -> toggleFavorite(app)
                    2 -> hideApp(app)
                    3 -> openAppDetails(app)
                    4 -> requestUninstall(app)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleFavorite(app: ResolveInfo) {
        val id = getAppId(app)
        val favorites = getCurrentFavorites()
        val added = if (id in favorites) {
            favorites.remove(id)
            false
        } else {
            favorites.add(id)
            true
        }
        saveCollections()
        renderFavorites(animate = true)
        renderApps(searchBox.text?.toString().orEmpty(), animate = true)
        Toast.makeText(this, if (added) "Added to ${currentModeName()} favorites" else "Removed from favorites", Toast.LENGTH_SHORT).show()
    }

    private fun hideApp(app: ResolveInfo) {
        val id = getAppId(app)
        AlertDialog.Builder(this)
            .setTitle("Hide ${appLabel(app)}?")
            .setMessage("It will be hidden only from the ${currentModeName()} profile. You can restore it from Hidden apps.")
            .setPositiveButton("Hide") { _, _ ->
                getCurrentHidden().add(id)
                saveCollections()
                refreshEverything(animate = true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showHiddenAppsDialog() {
        val hidden = getCurrentHidden()
        val hiddenApps = allApps
            .filter { getAppId(it) in hidden }
            .sortedBy { appLabel(it).lowercase(Locale.getDefault()) }

        if (hiddenApps.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Hidden apps")
                .setMessage("Nothing is hidden in the ${currentModeName()} profile.")
                .setPositiveButton("Done", null)
                .show()
            return
        }

        val labels = hiddenApps.map(::appLabel).toTypedArray()
        val selected = BooleanArray(hiddenApps.size)

        AlertDialog.Builder(this)
            .setTitle("Restore hidden apps")
            .setMultiChoiceItems(labels, selected) { _, which, checked -> selected[which] = checked }
            .setPositiveButton("Restore selected") { _, _ ->
                var restored = 0
                hiddenApps.forEachIndexed { index, app ->
                    if (selected[index] && hidden.remove(getAppId(app))) restored++
                }
                if (restored > 0) {
                    saveCollections()
                    refreshEverything(animate = true)
                    Toast.makeText(this, "$restored app${if (restored == 1) "" else "s"} restored", Toast.LENGTH_SHORT).show()
                }
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
            "Grid density: ${resolvedGridColumns()} columns",
            "Icon size: $iconSizeDp dp",
            "Label size: ${labelSizeSp.toInt()} sp",
            "App labels: ${if (showLabels) "On" else "Off"}",
            "Compact tiles: ${if (compactMode) "On" else "Off"}",
            "Startup animation: ${if (startupEnabled) "On" else "Off"}",
            "Haptic feedback: ${if (hapticsEnabled) "On" else "Off"}",
            "Set Miles as home app",
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
                        showLabels = !showLabels
                        saveAppearance()
                        refreshEverything(animate = true)
                    }
                    5 -> {
                        compactMode = !compactMode
                        saveAppearance()
                        refreshEverything(animate = true)
                    }
                    6 -> {
                        startupEnabled = !startupEnabled
                        saveAppearance()
                        Toast.makeText(this, "Startup animation ${if (startupEnabled) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                    }
                    7 -> {
                        hapticsEnabled = !hapticsEnabled
                        saveAppearance()
                    }
                    8 -> openHomeSettings()
                    9 -> confirmResetAppearance()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun chooseGridColumns() {
        val values = intArrayOf(3, 4, 5)
        val labels = arrayOf("Roomy", "Balanced", "Compact")
        val checked = values.indexOf(gridColumns).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Grid density")
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
        val checked = values.indexOfFirst { it == labelSizeSp }.coerceAtLeast(0)
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
                showLabels = true
                compactMode = false
                startupEnabled = true
                hapticsEnabled = true
                gridColumns = 4
                iconSizeDp = 54
                labelSizeSp = 11f
                sortMode = SORT_ALPHABETICAL
                saveAppearance()
                refreshEverything(animate = true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveAppearance() {
        prefs.edit()
            .putBoolean(KEY_DARK_THEME, darkTheme)
            .putBoolean(KEY_SHOW_LABELS, showLabels)
            .putBoolean(KEY_COMPACT_MODE, compactMode)
            .putBoolean(KEY_STARTUP_ENABLED, startupEnabled)
            .putBoolean(KEY_HAPTICS_ENABLED, hapticsEnabled)
            .putInt(KEY_GRID_COLUMNS, gridColumns)
            .putInt(KEY_ICON_SIZE, iconSizeDp)
            .putFloat(KEY_LABEL_SIZE, labelSizeSp)
            .putInt(KEY_SORT_MODE, sortMode)
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

    private fun cleanMissingPreferences() {
        val installedIds = allApps.map(::getAppId).toSet()
        workFavorites.retainAll(installedIds)
        homeFavorites.retainAll(installedIds)
        workHidden.retainAll(installedIds)
        homeHidden.retainAll(installedIds)
        saveCollections()
    }

    private fun startVoiceInput() {
        assistantStatus.text = "Listening…"
        assistantLogo.setListening(true)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Miles")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            startActivityForResult(intent, REQUEST_VOICE)
        } catch (_: ActivityNotFoundException) {
            assistantLogo.setListening(false)
            assistantStatus.text = "Voice recognition isn't available on this phone."
            Toast.makeText(this, "Voice recognition isn't available.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_VOICE) return
        if (resultCode != RESULT_OK) {
            assistantLogo.setListening(false)
            assistantStatus.text = "Tap to talk • Hold to open ChatGPT"
            return
        }
        val heard = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (heard.isBlank()) {
            assistantLogo.setListening(false)
            assistantStatus.text = "I didn't catch that. Tap and try again."
            return
        }
        assistantLogo.setListening(false)
        assistantLogo.playReaction()
        assistantGreeting.text = "You said: “$heard”"
        handleMilesCommand(heard)
    }

    private fun handleMilesCommand(spoken: String) {
        val command = spoken.trim().lowercase(Locale.getDefault())
        when {
            command == "hey miles" || command == "miles" -> {
                assistantGreeting.text = "I'm here."
                assistantStatus.text = "Try “open YouTube” or “switch to Work.”"
            }
            command.contains("work mode") || command == "work" || command.contains("switch to work") -> {
                switchMode(true)
                assistantGreeting.text = "Work mode is ready."
                assistantStatus.text = "Focused apps, fewer distractions."
            }
            command.contains("home mode") || command == "home" || command.contains("switch to home") -> {
                switchMode(false)
                assistantGreeting.text = "Home mode is ready."
                assistantStatus.text = "Your personal space is open."
            }
            command.startsWith("open ") || command.startsWith("launch ") -> {
                val requested = command.substringAfter(' ').trim()
                val match = allApps.firstOrNull {
                    appLabel(it).lowercase(Locale.getDefault()).contains(requested) ||
                        it.activityInfo.packageName.lowercase(Locale.getDefault()).contains(requested.replace(" ", ""))
                }
                if (match != null) {
                    assistantGreeting.text = "Opening ${appLabel(match)}."
                    assistantStatus.text = "On it."
                    launchApp(match)
                } else {
                    assistantGreeting.text = "I couldn't find “$requested.”"
                    assistantStatus.text = "I've placed it in app search."
                    searchBox.setText(requested)
                    searchBox.requestFocus()
                }
            }
            command.startsWith("search ") || command.startsWith("find ") -> {
                val query = command.substringAfter(' ').trim()
                searchBox.setText(query)
                searchBox.requestFocus()
                assistantGreeting.text = "Searching for “$query.”"
                assistantStatus.text = "Results are below."
            }
            command.contains("chatgpt") || command.contains("ask miles") -> openChatGpt()
            else -> {
                assistantGreeting.text = "I heard you."
                assistantStatus.text = "Opening ChatGPT for the full conversation."
                openChatGpt(spoken)
            }
        }
    }

    private fun openChatGpt(initialPrompt: String? = null) {
        val appIntent = packageManager.getLaunchIntentForPackage("com.openai.chatgpt")
        if (appIntent != null) {
            startActivity(appIntent)
            return
        }
        val url = if (initialPrompt.isNullOrBlank()) {
            "https://chatgpt.com/"
        } else {
            "https://chatgpt.com/?q=" + Uri.encode(initialPrompt)
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open ChatGPT.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppDetails(app: ResolveInfo) {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${app.activityInfo.packageName}")
            })
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open app details.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestUninstall(app: ResolveInfo) {
        try {
            startActivity(Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${app.activityInfo.packageName}")
            })
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Uninstall is not available for this app.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openHomeSettings() {
        val intents = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                startActivity(intent)
                return
            } catch (_: Exception) {
                // Try the next supported settings page.
            }
        }
        Toast.makeText(this, "Unable to open home app settings.", Toast.LENGTH_SHORT).show()
    }

    private fun launchApp(app: ResolveInfo) {
        val info = app.activityInfo
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(info.packageName, info.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        try {
            startActivity(launchIntent)
        } catch (_: Exception) {
            try {
                packageManager.getLaunchIntentForPackage(info.packageName)?.let { fallback ->
                    startActivity(fallback)
                    return
                }
                Toast.makeText(this, "Unable to open this app.", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this, "Unable to open this app.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadLaunchableApps(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return try {
            packageManager.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != packageName }
                .distinctBy(::getAppId)
                .sortedBy { appLabel(it).lowercase(Locale.getDefault()) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getCurrentFavorites(): MutableSet<String> = if (workMode) workFavorites else homeFavorites

    private fun getCurrentHidden(): MutableSet<String> = if (workMode) workHidden else homeHidden

    private fun currentModeName(): String = if (workMode) "Work" else "Home"

    private fun getAppId(app: ResolveInfo): String = "${app.activityInfo.packageName}/${app.activityInfo.name}"

    private fun appLabel(app: ResolveInfo): String = try {
        app.loadLabel(packageManager).toString().ifBlank { app.activityInfo.packageName }
    } catch (_: Exception) {
        app.activityInfo.packageName
    }

    private fun newGrid(): GridLayout = GridLayout(this).apply {
        columnCount = resolvedGridColumns()
        alignmentMode = GridLayout.ALIGN_BOUNDS
        useDefaultMargins = false
        isColumnOrderPreserved = true
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun sectionTitle(): TextView = TextView(this).apply {
        textSize = 18f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(2), 0, dp(2), dp(7))
        includeFontPadding = false
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
        if (strokeColor != null && strokeWidthDp > 0) setStroke(dp(strokeWidthDp), strokeColor)
    }

    private fun neonPanelDrawable(
        topColor: Int,
        bottomColor: Int,
        radiusDp: Int,
        strokeColor: Int
    ): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(topColor, bottomColor)
    ).apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radiusDp).toFloat()
        setStroke(dp(1), strokeColor)
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

    private fun haptic(view: View, strong: Boolean = false) {
        if (!hapticsEnabled) return
        view.performHapticFeedback(
            if (strong) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP
        )
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val PREFS_NAME = "miles_launcher"
        private const val KEY_WORK_MODE = "work_mode"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_SHOW_LABELS = "show_labels"
        private const val KEY_COMPACT_MODE = "compact_mode"
        private const val KEY_STARTUP_ENABLED = "startup_enabled"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_ICON_SIZE = "icon_size"
        private const val KEY_LABEL_SIZE = "label_size"
        private const val KEY_SORT_MODE = "sort_mode"
        private const val KEY_WORK_FAVORITES = "work_favorites"
        private const val KEY_HOME_FAVORITES = "home_favorites"
        private const val KEY_WORK_HIDDEN = "work_hidden"
        private const val KEY_HOME_HIDDEN = "home_hidden"

        private const val SORT_ALPHABETICAL = 0
        private const val SORT_REVERSE = 1
        private const val SORT_FAVORITES_FIRST = 2
        private const val REQUEST_VOICE = 7301
        private const val PROFILE_OUT_MS = 45L
        private const val PROFILE_IN_MS = 90L
    }
}

class ProfileSwipeFrameLayout(context: Context) : FrameLayout(context) {

    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null

    private var downX = 0f
    private var downY = 0f
    private var interceptingSwipe = false
    private var swipeTriggered = false
    private val touchSlop = (24f * resources.displayMetrics.density)
    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent): Boolean = true

        override fun onFling(
            first: MotionEvent?,
            second: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val start = first ?: return false
            val dx = second.x - start.x
            val dy = second.y - start.y
            if (kotlin.math.abs(dx) < touchSlop * 2f) return false
            if (kotlin.math.abs(dx) <= kotlin.math.abs(dy) * 1.2f) return false
            if (kotlin.math.abs(velocityX) < 280f) return false
            swipeTriggered = true
            if (dx < 0f) onSwipeLeft?.invoke() else onSwipeRight?.invoke()
            return true
        }
    })

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                interceptingSwipe = false
                swipeTriggered = false
                detector.onTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (kotlin.math.abs(dx) > touchSlop && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.2f) {
                    interceptingSwipe = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> interceptingSwipe = false
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = detector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP && !swipeTriggered) {
            val dx = event.x - downX
            val dy = event.y - downY
            if (kotlin.math.abs(dx) > touchSlop * 2.2f && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.2f) {
                swipeTriggered = true
                if (dx < 0f) onSwipeLeft?.invoke() else onSwipeRight?.invoke()
            }
        }
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            interceptingSwipe = false
            parent?.requestDisallowInterceptTouchEvent(false)
        }
        return handled || swipeTriggered || interceptingSwipe || super.onTouchEvent(event)
    }
}

class CyberBackdropView(context: Context) : View(context) {
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(24, 0, 174, 255)
        strokeWidth = resources.displayMetrics.density
    }
    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, 0, 196, 255)
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val step = 42f * resources.displayMetrics.density
        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += step
        }
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += step
        }
        val radius = 1.5f * resources.displayMetrics.density
        var row = 0
        y = step
        while (y < height) {
            val offset = if (row % 2 == 0) step * 0.5f else step
            x = offset
            while (x < width) {
                canvas.drawCircle(x, y, radius, nodePaint)
                x += step * 2f
            }
            row++
            y += step * 2f
        }
    }
}

class MilesLogoView(context: Context) : View(context) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val thinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var accent = Color.rgb(34, 132, 255)
    private var accentStrong = Color.rgb(0, 91, 224)
    private var phase = 0f
    private var energy = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3600L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        addUpdateListener {
            phase = it.animatedFraction
            energy = 0.5f + 0.5f * kotlin.math.sin(phase * Math.PI * 2.0).toFloat()
            invalidate()
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setAccentColors(primary: Int, strong: Int) {
        accent = primary
        accentStrong = strong
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val cx = width / 2f
        val cy = height / 2f
        val outer = size * 0.43f
        val rotation = phase * 360f

        // Ambient holographic aura.
        fillPaint.color = Color.argb(
            (28 + energy * 30).toInt(),
            Color.red(accent), Color.green(accent), Color.blue(accent)
        )
        fillPaint.setShadowLayer(size * (0.10f + energy * 0.035f), 0f, 0f, accent)
        canvas.drawCircle(cx, cy, outer * (0.96f + energy * 0.035f), fillPaint)
        fillPaint.clearShadowLayer()

        // Counter-rotating segmented energy rings.
        strokePaint.strokeWidth = size * 0.034f
        strokePaint.color = Color.argb(220, Color.red(accent), Color.green(accent), Color.blue(accent))
        val outerRect = RectF(cx - outer, cy - outer, cx + outer, cy + outer)
        canvas.drawArc(outerRect, rotation - 82f, 108f, false, strokePaint)
        canvas.drawArc(outerRect, rotation + 72f, 62f, false, strokePaint)
        canvas.drawArc(outerRect, rotation + 188f, 86f, false, strokePaint)

        thinPaint.strokeWidth = size * 0.014f
        thinPaint.color = Color.argb(145, 135, 225, 255)
        val innerRing = outer * 0.82f
        val innerRect = RectF(cx - innerRing, cy - innerRing, cx + innerRing, cy + innerRing)
        canvas.drawArc(innerRect, -rotation * 0.72f, 142f, false, thinPaint)
        canvas.drawArc(innerRect, 196f - rotation * 0.72f, 104f, false, thinPaint)

        // Futuristic hexagonal glass core.
        val coreR = size * 0.305f
        val hex = Path()
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60.0) - 30.0)
            val x = cx + coreR * kotlin.math.cos(angle).toFloat()
            val y = cy + coreR * kotlin.math.sin(angle).toFloat()
            if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
        }
        hex.close()

        fillPaint.color = Color.argb(232, 11, 24, 47)
        fillPaint.setShadowLayer(size * 0.055f, 0f, size * 0.018f, Color.argb(180, 0, 0, 0))
        canvas.drawPath(hex, fillPaint)
        fillPaint.clearShadowLayer()

        strokePaint.strokeWidth = size * 0.025f
        strokePaint.color = Color.argb(235, Color.red(accent), Color.green(accent), Color.blue(accent))
        strokePaint.setShadowLayer(size * 0.025f, 0f, 0f, accent)
        canvas.drawPath(hex, strokePaint)
        strokePaint.clearShadowLayer()

        // Interior circuit traces.
        thinPaint.strokeWidth = size * 0.012f
        thinPaint.color = Color.argb(115, 105, 205, 255)
        canvas.drawLine(cx - coreR * 0.76f, cy, cx - coreR * 0.50f, cy, thinPaint)
        canvas.drawLine(cx + coreR * 0.50f, cy, cx + coreR * 0.76f, cy, thinPaint)
        canvas.drawLine(cx, cy - coreR * 0.76f, cx, cy - coreR * 0.54f, thinPaint)
        canvas.drawCircle(cx - coreR * 0.82f, cy, size * 0.014f, fillPaint.apply { color = accent })
        canvas.drawCircle(cx + coreR * 0.82f, cy, size * 0.014f, fillPaint)
        canvas.drawCircle(cx, cy - coreR * 0.82f, size * 0.014f, fillPaint)

        // Angular M monogram: metallic white core with blue energy edge.
        val mPath = Path().apply {
            moveTo(cx - size * 0.205f, cy + size * 0.18f)
            lineTo(cx - size * 0.205f, cy - size * 0.175f)
            lineTo(cx - size * 0.035f, cy - size * 0.015f)
            lineTo(cx, cy + size * 0.035f)
            lineTo(cx + size * 0.035f, cy - size * 0.015f)
            lineTo(cx + size * 0.205f, cy - size * 0.175f)
            lineTo(cx + size * 0.205f, cy + size * 0.18f)
        }

        strokePaint.strokeWidth = size * 0.105f
        strokePaint.color = Color.argb(190, Color.red(accentStrong), Color.green(accentStrong), Color.blue(accentStrong))
        strokePaint.setShadowLayer(size * (0.035f + energy * 0.025f), 0f, 0f, accent)
        canvas.drawPath(mPath, strokePaint)
        strokePaint.clearShadowLayer()

        strokePaint.strokeWidth = size * 0.066f
        strokePaint.color = Color.rgb(235, 246, 255)
        canvas.drawPath(mPath, strokePaint)

        thinPaint.strokeWidth = size * 0.014f
        thinPaint.color = Color.argb(220, 135, 230, 255)
        canvas.drawPath(mPath, thinPaint)

        // Hidden Miles face: eyes live inside the M and pulse subtly.
        val blink = phase > 0.76f && phase < 0.80f
        val eyeHeight = if (blink) size * 0.010f else size * (0.025f + energy * 0.006f)
        fillPaint.color = Color.rgb(140, 235, 255)
        fillPaint.setShadowLayer(size * 0.030f, 0f, 0f, accent)
        canvas.drawRoundRect(
            RectF(cx - size * 0.088f, cy + size * 0.075f - eyeHeight / 2f,
                cx - size * 0.030f, cy + size * 0.075f + eyeHeight / 2f),
            eyeHeight, eyeHeight, fillPaint
        )
        canvas.drawRoundRect(
            RectF(cx + size * 0.030f, cy + size * 0.075f - eyeHeight / 2f,
                cx + size * 0.088f, cy + size * 0.075f + eyeHeight / 2f),
            eyeHeight, eyeHeight, fillPaint
        )
        fillPaint.clearShadowLayer()

        // Orbiting data nodes.
        for (i in 0 until 3) {
            val a = Math.toRadians((rotation + i * 120f).toDouble())
            val nodeX = cx + outer * kotlin.math.cos(a).toFloat()
            val nodeY = cy + outer * kotlin.math.sin(a).toFloat()
            fillPaint.color = if (i == 0) Color.WHITE else Color.rgb(120, 225, 255)
            fillPaint.setShadowLayer(size * 0.022f, 0f, 0f, accent)
            canvas.drawCircle(nodeX, nodeY, size * if (i == 0) 0.027f else 0.018f, fillPaint)
            fillPaint.clearShadowLayer()
        }
    }
}


class MilesRobotView(context: Context) : View(context) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var accent = Color.rgb(34, 132, 255)
    private var accentStrong = Color.rgb(0, 91, 224)
    private var phase = 0f
    private var listening = false
    private var reactionBoost = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2600L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        addUpdateListener {
            phase = it.animatedFraction
            if (reactionBoost > 0f) reactionBoost = (reactionBoost - 0.035f).coerceAtLeast(0f)
            invalidate()
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setAccentColors(primary: Int, strong: Int) {
        accent = primary
        accentStrong = strong
        invalidate()
    }

    fun setListening(value: Boolean) {
        listening = value
        reactionBoost = if (value) 1f else 0.35f
        invalidate()
    }

    fun playGreeting() {
        reactionBoost = 1f
        animate().translationY(-height * 0.06f).setDuration(180L).withEndAction {
            animate().translationY(0f).setDuration(260L).start()
        }.start()
    }

    fun playReaction() {
        reactionBoost = 1f
        animate().scaleX(1.08f).scaleY(1.08f).setDuration(140L).withEndAction {
            animate().scaleX(1f).scaleY(1f).setDuration(220L).start()
        }.start()
    }

    fun playProfileSwitch(direction: Int) {
        reactionBoost = 1f
        val look = if (direction < 0) -width * 0.055f else width * 0.055f
        animate().cancel()
        animate()
            .translationX(look)
            .rotation(if (direction < 0) -3.5f else 3.5f)
            .setDuration(70L)
            .withEndAction {
                animate().translationX(0f).rotation(0f).setDuration(100L).start()
            }
            .start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return
        val cx = width / 2f
        val bob = kotlin.math.sin(phase * Math.PI * 2.0).toFloat() * size * 0.025f
        val cy = height / 2f + bob
        val pulse = if (listening) 0.72f + 0.28f * kotlin.math.sin(phase * Math.PI * 4.0).toFloat() else 0.35f

        fillPaint.color = Color.argb((38 + pulse * 42).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent))
        fillPaint.setShadowLayer(size * (0.10f + pulse * 0.035f), 0f, 0f, accent)
        canvas.drawCircle(cx, cy, size * (0.42f + pulse * 0.025f), fillPaint)
        fillPaint.clearShadowLayer()

        strokePaint.strokeWidth = size * 0.025f
        strokePaint.color = Color.argb(150, Color.red(accent), Color.green(accent), Color.blue(accent))
        val ringR = size * 0.43f
        canvas.drawArc(RectF(cx-ringR, cy-ringR, cx+ringR, cy+ringR), -70f + phase*80f, 235f, false, strokePaint)

        val body = RectF(cx-size*0.20f, cy+size*0.10f, cx+size*0.20f, cy+size*0.34f)
        fillPaint.color = Color.rgb(225, 234, 246)
        canvas.drawRoundRect(body, size*0.10f, size*0.10f, fillPaint)
        strokePaint.color = Color.argb(190, Color.red(accentStrong), Color.green(accentStrong), Color.blue(accentStrong))
        strokePaint.strokeWidth = size*0.025f
        canvas.drawRoundRect(body, size*0.10f, size*0.10f, strokePaint)

        val head = RectF(cx-size*0.27f, cy-size*0.25f, cx+size*0.27f, cy+size*0.13f)
        fillPaint.color = Color.rgb(240, 246, 255)
        fillPaint.setShadowLayer(size*0.045f, 0f, size*0.018f, Color.argb(100, 0, 0, 0))
        canvas.drawRoundRect(head, size*0.15f, size*0.15f, fillPaint)
        fillPaint.clearShadowLayer()
        strokePaint.color = accent
        strokePaint.strokeWidth = size*0.028f
        canvas.drawRoundRect(head, size*0.15f, size*0.15f, strokePaint)

        val face = RectF(cx-size*0.20f, cy-size*0.17f, cx+size*0.20f, cy+size*0.045f)
        fillPaint.color = Color.rgb(12, 26, 48)
        canvas.drawRoundRect(face, size*0.09f, size*0.09f, fillPaint)

        val blinkWindow = phase > 0.78f && phase < 0.82f
        val eyeH = if (blinkWindow) size*0.012f else size*0.055f
        fillPaint.color = Color.rgb(118, 224, 255)
        fillPaint.setShadowLayer(size*0.035f, 0f, 0f, accent)
        canvas.drawRoundRect(RectF(cx-size*0.125f, cy-size*0.095f-eyeH/2, cx-size*0.055f, cy-size*0.095f+eyeH/2), eyeH, eyeH, fillPaint)
        canvas.drawRoundRect(RectF(cx+size*0.055f, cy-size*0.095f-eyeH/2, cx+size*0.125f, cy-size*0.095f+eyeH/2), eyeH, eyeH, fillPaint)
        fillPaint.clearShadowLayer()

        strokePaint.color = Color.rgb(116, 205, 255)
        strokePaint.strokeWidth = size*0.018f
        val smile = RectF(cx-size*0.075f, cy-size*0.055f, cx+size*0.075f, cy+size*0.025f)
        canvas.drawArc(smile, 18f, 144f, false, strokePaint)

        strokePaint.color = Color.rgb(210, 224, 242)
        strokePaint.strokeWidth = size*0.07f
        val wave = kotlin.math.sin(phase * Math.PI * 4.0).toFloat() * (0.25f + reactionBoost*0.75f)
        canvas.drawLine(cx-size*0.20f, cy+size*0.16f, cx-size*(0.31f+0.035f*wave), cy+size*(0.06f-0.10f*wave), strokePaint)
        canvas.drawLine(cx+size*0.20f, cy+size*0.16f, cx+size*0.31f, cy+size*0.24f, strokePaint)

        // Headset-style ear pods and a friendly waving hand make Miles read clearly at small sizes.
        fillPaint.color = Color.rgb(205, 220, 240)
        strokePaint.color = accent
        strokePaint.strokeWidth = size * 0.018f
        val leftEar = RectF(cx-size*0.315f, cy-size*0.135f, cx-size*0.255f, cy-size*0.005f)
        val rightEar = RectF(cx+size*0.255f, cy-size*0.135f, cx+size*0.315f, cy-size*0.005f)
        canvas.drawRoundRect(leftEar, size*0.03f, size*0.03f, fillPaint)
        canvas.drawRoundRect(rightEar, size*0.03f, size*0.03f, fillPaint)
        canvas.drawRoundRect(leftEar, size*0.03f, size*0.03f, strokePaint)
        canvas.drawRoundRect(rightEar, size*0.03f, size*0.03f, strokePaint)

        val handX = cx - size*(0.33f+0.035f*wave)
        val handY = cy + size*(0.035f-0.10f*wave)
        fillPaint.color = Color.rgb(232, 241, 252)
        canvas.drawCircle(handX, handY, size*0.052f, fillPaint)
        strokePaint.color = accent
        strokePaint.strokeWidth = size*0.012f
        canvas.drawCircle(handX, handY, size*0.052f, strokePaint)
        for (i in -2..2) {
            val dx = i * size * 0.021f
            canvas.drawLine(handX + dx, handY-size*0.035f, handX + dx*1.15f, handY-size*0.085f, strokePaint)
        }

        // Illuminated chest badge with a tiny M monogram.
        fillPaint.color = Color.rgb(10, 30, 58)
        canvas.drawCircle(cx, cy+size*0.22f, size*0.064f, fillPaint)
        strokePaint.color = accent
        strokePaint.strokeWidth = size*0.014f
        canvas.drawCircle(cx, cy+size*0.22f, size*0.064f, strokePaint)
        val badgeM = Path().apply {
            moveTo(cx-size*0.032f, cy+size*0.242f)
            lineTo(cx-size*0.032f, cy+size*0.198f)
            lineTo(cx, cy+size*0.225f)
            lineTo(cx+size*0.032f, cy+size*0.198f)
            lineTo(cx+size*0.032f, cy+size*0.242f)
        }
        strokePaint.strokeWidth = size*0.012f
        strokePaint.color = Color.rgb(170, 240, 255)
        canvas.drawPath(badgeM, strokePaint)

        fillPaint.color = accent
        fillPaint.setShadowLayer(size*0.035f, 0f, 0f, accent)
        canvas.drawCircle(cx, cy+size*0.22f, size*(0.018f + pulse*0.006f), fillPaint)
        fillPaint.clearShadowLayer()

        strokePaint.strokeWidth = size*0.018f
        strokePaint.color = accent
        canvas.drawLine(cx, cy-size*0.25f, cx, cy-size*0.33f, strokePaint)
        fillPaint.color = Color.rgb(132, 232, 255)
        canvas.drawCircle(cx, cy-size*0.35f, size*0.035f, fillPaint)
    }
}

