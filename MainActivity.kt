package com.sovereign.solo

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
    private val ui = Handler(Looper.getMainLooper())
    private var activeTab = 0 // 0: STATUS, 1: QUESTS, 2: GATES, 3: INVENTORY, 4: LUNA
    private var mainContainer: LinearLayout? = null

    private val timerRunnable = object : Runnable {
        override fun run() {
            renderHeader()
            ui.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.settle(this)
        handleIntentShare(intent)
        buildSystemHUD()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentShare(intent)
    }

    private fun handleIntentShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            val added = Store.importLines(this, text)
            Toast.makeText(this, "[SYSTEM] IMPORTED $added QUESTS FROM SHARE.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        buildSystemHUD()
        ui.post(timerRunnable)
        RankWidget.update(this)
    }

    override fun onPause() {
        super.onPause()
        ui.removeCallbacks(timerRunnable)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Core Launcher Intercept
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun makeBoxDrawable(borderColor: String = "#00E5FF", bgColor: String = "#E6030914"): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgColor))
            setStroke(dp(1), Color.parseColor(borderColor))
            cornerRadius = dp(4).toFloat()
        }
    }

    private fun tv(t: String, size: Float, color: Int = Color.parseColor("#00E5FF"), bold: Boolean = true) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
        typeface = Typeface.create(Typeface.MONOSPACE, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun hudButton(text: String, active: Boolean = true, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isEnabled = active
        setTextColor(if (active) Color.WHITE else Color.GRAY)
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        background = makeBoxDrawable(if (active) "#00E5FF" else "#334155", if (active) "#101D36" else "#0F172A")
        setOnClickListener { onClick() }
    }

    private fun buildSystemHUD() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EE030914"))
            setPadding(dp(12), dp(24), dp(12), dp(12))
        }

        renderHeaderPanel(root)
        renderFavoriteDock(root)
        renderNavTabs(root)

        mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = makeBoxDrawable("#00E5FF", "#10080D1A")
        }

        when (activeTab) {
            0 -> renderStatusTab(mainContainer!!)
            1 -> renderQuestsTab(mainContainer!!)
            2 -> renderGatesTab(mainContainer!!)
            3 -> renderInventoryTab(mainContainer!!)
            4 -> renderLunaTab(mainContainer!!)
        }

        val scroll = ScrollView(this).apply {
            addView(mainContainer)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(scroll)
        setContentView(root)
    }

    private var headerLevelTv: TextView? = null
    private var headerJobTv: TextView? = null
    private var headerSubTv: TextView? = null

    private fun renderHeaderPanel(root: LinearLayout) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = makeBoxDrawable("#00E5FF", "#0A0F1D")
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        topRow.addView(tv("[!] STATUS / SYSTEM HUD", 12f, Color.parseColor("#00E5FF")), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        
        val aiBtn = hudButton("🎙 LUNA", true) { launchVoice() }.apply {
            layoutParams = LinearLayout.LayoutParams(dp(70), dp(30))
            textSize = 9f
        }
        topRow.addView(aiBtn)
        header.addView(topRow)

        val xp = Store.xp(this)
        val lv = Store.level(xp)
        val (cur, need) = Store.levelXp(xp)
        val nameStr = Store.name(this).ifEmpty { "LEON" }

        headerLevelTv = tv("PLAYER: $nameStr  |  LV. $lv  [ ${Store.rank(lv)}-RANK ]", 15f, Color.WHITE)
        headerJobTv = tv("JOB: ${Store.job(this)}  |  TITLE: ${Store.title(this)}", 11f, Color.parseColor("#A000FF"))
        headerSubTv = tv("XP: $cur/$need | GOLD: ${Store.gold(this)} | TOKENS: ${Store.tokens(this)} | ${Store.goalLine(this)}", 10f, Color.parseColor("#94A3B8"))

        header.addView(headerLevelTv)
        header.addView(headerJobTv)
        header.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = need; progress = cur
            setPadding(0, dp(4), 0, dp(4))
        })
        header.addView(headerSubTv)
        root.addView(header)
    }

    private fun renderHeader() {
        val xp = Store.xp(this)
        val lv = Store.level(xp)
        val (cur, need) = Store.levelXp(xp)
        val nameStr = Store.name(this).ifEmpty { "LEON" }
        headerLevelTv?.text = "PLAYER: $nameStr  |  LV. $lv  [ ${Store.rank(lv)}-RANK ]"
        headerJobTv?.text = "JOB: ${Store.job(this)}  |  TITLE: ${Store.title(this)}"
        headerSubTv?.text = "XP: $cur/$need | GOLD: ${Store.gold(this)} | TOKENS: ${Store.tokens(this)} | ${Store.goalLine(this)}"
    }

    private fun renderFavoriteDock(root: LinearLayout) {
        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(4))
        }
        val favs = listOf(
            Triple("PHONE", Intent(Intent.ACTION_DIAL), null),
            Triple("CHROME", Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")), "com.android.chrome"),
            Triple("SETTINGS", Intent(Settings.ACTION_SETTINGS), null),
            Triple("ALARM", Intent(android.provider.AlarmClock.ACTION_SET_ALARM), null)
        )
        favs.forEach { (name, intent, pkg) ->
            val btn = TextView(this).apply {
                text = name; textSize = 9f
                setTextColor(Color.parseColor("#00E5FF"))
                gravity = Gravity.CENTER
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                background = makeBoxDrawable("#00E5FF", "#100F172A")
                setPadding(dp(4), dp(6), dp(4), dp(6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(2), 0, dp(2), 0) }
                setOnClickListener {
                    if (pkg != null && Store.blocked(this@MainActivity, pkg)) {
                        startActivity(Intent(this@MainActivity, BlockActivity::class.java).putExtra("pkg", name))
                    } else {
                        try { startActivity(intent) } catch (e: Exception) { Toast.makeText(this@MainActivity, "[SYSTEM] ACTION UNUSED.", Toast.LENGTH_SHORT).show() }
                    }
                }
            }
            dock.addView(btn)
        }
        root.addView(dock)
    }

    private fun renderNavTabs(root: LinearLayout) {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
        }
        val tabs = listOf("STATUS", "QUESTS", "GATES", "APPS", "LUNA")
        tabs.forEachIndexed { idx, label ->
            val tabBtn = Button(this).apply {
                text = label
                textSize = 9f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                setTextColor(if (activeTab == idx) Color.BLACK else Color.parseColor("#00E5FF"))
                background = makeBoxDrawable(if (activeTab == idx) "#00E5FF" else "#1E293B", if (activeTab == idx) "#00E5FF" else "#0F172A")
                layoutParams = LinearLayout.LayoutParams(0, dp(32), 1f).apply { setMargins(dp(1), 0, dp(1), 0) }
                setOnClickListener { activeTab = idx; buildSystemHUD() }
            }
            nav.addView(tabBtn)
        }
        root.addView(nav)
    }

    private fun renderStatusTab(panel: LinearLayout) {
        panel.addView(tv("STATUS MATRIX", 13f, Color.WHITE))
        panel.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { setMargins(0, dp(4), 0, dp(8)) }; setBackgroundColor(Color.parseColor("#334155")) })

        panel.addView(tv("STR (DISCIPLINE) : ${Store.str(this)}", 12f, Color.parseColor("#00E5FF")))
        panel.addView(tv("INT (KNOWLEDGE)  : ${Store.intStat(this)}", 12f, Color.parseColor("#00E5FF")))
        panel.addView(tv("AGI (EXECUTION)  : ${Store.agi(this)}", 12f, Color.parseColor("#00E5FF")))
        panel.addView(tv("PER (PERCEPTION) : ${Store.per(this)}", 12f, Color.parseColor("#00E5FF")))
        panel.addView(tv("VIT (ENERGY)     : ${Store.vit(this)}%", 12f, Color.parseColor("#00E5FF")))

        panel.addView(Space(this, null).apply { minimumHeight = dp(10) })
        panel.addView(tv("[!] ONE PERSON, ONE DATA SYNC", 11f, Color.parseColor("#A000FF")))
        
        val syncRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        syncRow.addView(hudButton("EXPORT CODE") {
            val code = Store.exportAll(this)
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("solo_sync", code))
            Toast.makeText(this, "[SYSTEM] DATA COPIED TO CLIPBOARD.", Toast.LENGTH_LONG).show()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
        
        syncRow.addView(hudButton("IMPORT CODE") {
            val input = EditText(this)
            AlertDialog.Builder(this).setTitle("PASTE SYNC CODE").setView(input)
                .setPositiveButton("IMPORT") { _, _ ->
                    if (Store.importAll(this, input.text.toString())) {
                        Toast.makeText(this, "[SYSTEM] DATA SYNCED SUCCESSFULLY.", Toast.LENGTH_SHORT).show()
                        buildSystemHUD()
                    } else Toast.makeText(this, "[SYSTEM] INVALID SYNC CODE.", Toast.LENGTH_SHORT).show()
                }.setNegativeButton("CANCEL", null).show()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
        panel.addView(syncRow)
    }

    private fun renderQuestsTab(panel: LinearLayout) {
        panel.addView(tv("DAILY MISSIONS", 13f, Color.WHITE))
        panel.addView(tv("Evidence required for task verification.", 10f, Color.GRAY))
        panel.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { setMargins(0, dp(4), 0, dp(8)) }; setBackgroundColor(Color.parseColor("#334155")) })

        Store.quests(this).forEach { q ->
            val done = Store.questDone(this, q)
            val qRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val cb = CheckBox(this).apply {
                text = q; textSize = 11f
                setTextColor(if (done) Color.GRAY else Color.parseColor("#9BE7FF"))
                isChecked = done; isEnabled = !done
                setOnClickListener {
                    if (!done) {
                        val input = EditText(this@MainActivity).apply { hint = "Proof / Reflection (3+ words)" }
                        AlertDialog.Builder(this@MainActivity).setTitle("SUBMIT MISSION EVIDENCE").setView(input)
                            .setPositiveButton("CLAIM") { _, _ ->
                                val res = Store.doQuest(this@MainActivity, q, input.text.toString())
                                Toast.makeText(this@MainActivity, res, Toast.LENGTH_SHORT).show()
                                buildSystemHUD()
                            }.setNegativeButton("CANCEL") { _, _ -> isChecked = false }.show()
                    }
                }
            }
            qRow.addView(cb, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            panel.addView(qRow)
        }
    }

    private fun renderGatesTab(panel: LinearLayout) {
        panel.addView(tv("DUNGEON FOCUS GATES", 13f, Color.WHITE))
        panel.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { setMargins(0, dp(4), 0, dp(8)) }; setBackgroundColor(Color.parseColor("#334155")) })

        val gOn = Store.gateOn(this)
        val ready = Store.gateReady(this)

        panel.addView(hudButton("ENTER GATE (${Store.gateMin(this)} MIN)", !gOn && !ready) {
            Store.enterGate(this, "Mathematics", Store.gateMin(this), false)
            buildSystemHUD()
        })
        panel.addView(Space(this, null).apply { minimumHeight = dp(6) })
        panel.addView(hudButton("CLAIM GATE REWARD", ready) {
            val input = EditText(this).apply { hint = "Summary of work completed" }
            AlertDialog.Builder(this).setTitle("CLAIM GATE REWARD").setView(input)
                .setPositiveButton("SUBMIT") { _, _ ->
                    val res = Store.completeGate(this, input.text.toString())
                    Toast.makeText(this, res, Toast.LENGTH_SHORT).show()
                    buildSystemHUD()
                }.setNegativeButton("CANCEL", null).show()
        })
        panel.addView(Space(this, null).apply { minimumHeight = dp(6) })
        panel.addView(hudButton("ABANDON GATE", gOn) {
            val res = Store.abandonGate(this)
            Toast.makeText(this, res, Toast.LENGTH_SHORT).show()
            buildSystemHUD()
        })
        panel.addView(Space(this, null).apply { minimumHeight = dp(12) })
        panel.addView(hudButton("USE TOKEN (${Store.PASS_MIN} MIN PASS)") {
            Toast.makeText(this, Store.useToken(this), Toast.LENGTH_SHORT).show()
            buildSystemHUD()
        })
    }

    private fun renderInventoryTab(panel: LinearLayout) {
        panel.addView(tv("APPLICATION INVENTORY", 13f, Color.WHITE))
        panel.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { setMargins(0, dp(4), 0, dp(8)) }; setBackgroundColor(Color.parseColor("#334155")) })

        val search = EditText(this).apply {
            hint = "Search inventory..."
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); textSize = 11f
            background = makeBoxDrawable("#334155", "#0F172A")
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val listLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val apps = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { Pair(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }.sortedBy { it.first.lowercase() }

        fun updateList(query: String) {
            listLayout.removeAllViews()
            apps.filter { it.first.contains(query, true) && it.second != packageName }.forEach { (name, pkg) ->
                val ent = Store.isHazard(this, pkg)
                val item = tv((if (ent) "☠ [HAZARD] " else "⚡ ") + name, 12f, if (ent) Color.parseColor("#F43F5E") else Color.parseColor("#00E5FF")).apply {
                    setPadding(0, dp(6), 0, dp(6))
                    setOnClickListener { launchApp(name, pkg) }
                    setOnLongClickListener {
                        Store.toggleHazard(this@MainActivity, pkg)
                        Toast.makeText(this@MainActivity, "[SYSTEM] TOGGLED HAZARD FOR $name", Toast.LENGTH_SHORT).show()
                        updateList(search.text.toString())
                        true
                    }
                }
                listLayout.addView(item)
            }
        }

        updateList("")
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateList(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        panel.addView(search)
        panel.addView(Space(this, null).apply { minimumHeight = dp(6) })
        panel.addView(listLayout)
    }

    private fun renderLunaTab(panel: LinearLayout) {
        panel.addView(tv("LUNA AI ASSISTANT", 13f, Color.WHITE))
        panel.addView(tv(Luna.next(this), 10f, Color.parseColor("#38BDF8")))
        panel.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { setMargins(0, dp(4), 0, dp(8)) }; setBackgroundColor(Color.parseColor("#334155")) })

        val input = EditText(this).apply {
            hint = "Enter system command..."
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); textSize = 11f
            background = makeBoxDrawable("#334155", "#0F172A")
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        panel.addView(input)
        panel.addView(Space(this, null).apply { minimumHeight = dp(6) })
        panel.addView(hudButton("EXECUTE COMMAND") {
            val res = Luna.ask(this, input.text.toString())
            Toast.makeText(this, res, Toast.LENGTH_SHORT).show()
            Luna.openPkg?.let { pkg -> launchApp(pkg, pkg) }
            buildSystemHUD()
        })
    }

    private fun launchVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "LUNA LISTENING...")
        }
        try { startActivityForResult(intent, 999) } catch (e: Exception) { Toast.makeText(this, "[LUNA] SPEECH OFFLINE.", Toast.LENGTH_SHORT).show() }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 999 && resultCode == RESULT_OK && data != null) {
            val matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val res = Luna.ask(this, matches[0])
                Toast.makeText(this, res, Toast.LENGTH_SHORT).show()
                Luna.openPkg?.let { pkg -> launchApp(pkg, pkg) }
                buildSystemHUD()
            }
        }
    }

    private fun launchApp(name: String, pkg: String) {
        if (Store.blocked(this, pkg)) {
            startActivity(Intent(this, BlockActivity::class.java).putExtra("pkg", name))
            return
        }
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }
}
