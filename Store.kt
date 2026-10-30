package com.sovereign.solo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** The System. Cold, exact, no mercy. Every rule lives here and never depends on willpower. */
object Store {
    val ENT = setOf(
        "com.google.android.youtube", "com.zhiliaoapp.musically", "com.instagram.android",
        "com.netflix.mediaclient", "com.facebook.katana", "com.twitter.android",
        "com.snapchat.android", "com.crunchyroll.crunchyroid", "com.reddit.frontpage",
        "com.dts.freefireth", "com.tencent.ig"
    )
    // Study tools: never blocked unless you mark them yourself.
    val ALLOW = setOf(
        "com.android.chrome", "com.chrome.beta", "org.mozilla.firefox", "com.google.android.dialer", "com.android.dialer",
        "com.google.android.calculator", "com.android.calculator2", "com.samsung.android.calculator",
        "com.google.android.keep", "com.google.android.apps.docs", "com.google.android.documentsui", "com.android.documentsui",
        "com.google.android.apps.messaging", "com.android.mms"
    )
    // Settings and installers are locked during a gate or penalty so the guard cannot be switched off in the moment.
    val STRICT = setOf("com.android.settings", "com.google.android.packageinstaller", "com.android.packageinstaller")
    val SUBJECTS = listOf("Mathematics", "Physics", "Chemistry", "Computer Science", "English", "Kiswahili",
        "Community Service Learning", "Workout", "Reading and writing")
    private val LONGS = setOf("gateEnd", "passEnd", "lockEnd", "signed", "examAt")
    const val PASS_MIN = 25
    const val TOKEN_COST = 500
    const val STAR_MIN = 60
    const val MIN_LEVEL_FOR_TOKEN = 3
    const val LABOR_WORDS = 500

    private fun p(c: Context) = c.getSharedPreferences("solo3", Context.MODE_PRIVATE)
    private fun n(c: Context, k: String) = p(c).getInt(k, 0)
    private fun l(c: Context, k: String) = p(c).getLong(k, 0L)
    private fun add(c: Context, k: String, v: Int) { p(c).edit().putInt(k, n(c, k) + v).apply() }
    fun now() = System.currentTimeMillis()

    // Identity and targets
    fun name(c: Context) = p(c).getString("name", "") ?: ""
    fun sign(c: Context, nm: String) { p(c).edit().putString("name", nm).putLong("signed", now()).apply() }
    fun curfew(c: Context) = p(c).getInt("cf", 22)
    fun gateMin(c: Context) = p(c).getInt("gm", 90)
    fun setCurfew(c: Context, v: Int) { p(c).edit().putInt("cf", v.coerceIn(18, 24)).apply() }
    fun setGateMin(c: Context, v: Int) { p(c).edit().putInt("gm", v.coerceIn(20, 180)).apply() }
    fun examName(c: Context) = p(c).getString("examName", "") ?: ""
    fun setExam(c: Context, name: String, days: Int) { p(c).edit().putString("examName", name).putLong("examAt", now() + days * 86400000L).apply() }
    fun examDays(c: Context): Int { val e = l(c, "examAt"); return if (e == 0L) -1 else Math.ceil((e - now()) / 86400000.0).toInt() }
    fun goalLine(c: Context): String {
        val d = examDays(c)
        val lock = "LOCK-IN ${minOf(streak(c), 7)}/7"
        return if (d < 0) lock else lock + " · " + (if (d == 0) "EXAM TODAY" else "EXAM IN $d DAYS") + (if (examName(c).isNotEmpty()) " (${examName(c)})" else "")
    }

    // State
    fun xp(c: Context) = n(c, "xp")
    fun gold(c: Context) = n(c, "gold")
    fun fatigue(c: Context) = n(c, "fatigue")
    fun stars(c: Context) = n(c, "stars")
    fun tokens(c: Context) = n(c, "tokens")
    fun streak(c: Context) = n(c, "streak")
    fun gateEnd(c: Context) = l(c, "gateEnd")
    fun passEnd(c: Context) = l(c, "passEnd")
    fun lockEnd(c: Context) = l(c, "lockEnd")
    fun gateOn(c: Context) = now() < gateEnd(c)
    fun gateReady(c: Context) = !gateOn(c) && gateEnd(c) > 0
    fun passOn(c: Context) = now() < passEnd(c)
    fun gateSubject(c: Context) = p(c).getString("gSubj", "") ?: ""
    fun gateMinutes(c: Context) = n(c, "gMin")
    fun gateIsDrill(c: Context) = p(c).getBoolean("gDrill", false)
    fun drill(c: Context) = p(c).getString("drill", "") ?: ""
    fun labor(c: Context) = p(c).getBoolean("labor", false)
    fun clearLabor(c: Context) { p(c).edit().putBoolean("labor", false).apply() }

    // Attributes
    fun str(c: Context) = 10 + n(c, "str")
    fun intStat(c: Context) = 10 + n(c, "int")
    fun agi(c: Context) = 10 + n(c, "agi")
    fun per(c: Context) = 10 + n(c, "foc")
    fun vit(c: Context) = maxOf(0, 100 - fatigue(c))

    fun level(xp: Int): Int { var x = xp; var l = 1; while (x >= 100 + 25 * (l - 1)) { x -= 100 + 25 * (l - 1); l++ }; return l }
    fun levelXp(xp: Int): Pair<Int, Int> { var x = xp; var l = 1; while (x >= 100 + 25 * (l - 1)) { x -= 100 + 25 * (l - 1); l++ }; return Pair(x, 100 + 25 * (l - 1)) }
    fun rank(l: Int) = when { l >= 30 -> "S"; l >= 20 -> "A"; l >= 15 -> "B"; l >= 10 -> "C"; l >= 5 -> "D"; else -> "E" }
    fun title(c: Context) = when (rank(level(xp(c)))) { "E" -> "UNAWAKENED"; "D" -> "AWAKENED"; "C" -> "KNIGHT"; "B" -> "STRATEGIST"; "A" -> "ELITE"; else -> "SHADOW MONARCH" }
    fun job(c: Context): String {
        val m = mapOf("WARRIOR" to str(c), "SCHOLAR" to intStat(c), "EXECUTOR" to agi(c), "SENTINEL" to per(c))
        return m.maxByOrNull { it.value }?.key ?: "WARRIOR"
    }
    fun words(s: String) = s.trim().split(Regex("\s+")).filter { it.isNotEmpty() }.size

    // Time and phase
    fun isSleep(c: Context): Boolean { val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); return h >= curfew(c) || h < 5 }
    fun phase(c: Context) = when { isSleep(c) -> "SLEEP MODE"; gateOn(c) -> "DUNGEON MODE"; labor(c) -> "DIGITAL LABOR"; passOn(c) -> "PASS ACTIVE"; else -> "ENTERTAINMENT LOCKED" }
    private fun day(off: Int = 0): String { val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -off); return "%04d%02d%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)) }
    private fun touch(c: Context) {
        val t = day(); val last = p(c).getString("lastDay", "") ?: ""
        if (last == t) return
        val s = if (last == day(1)) n(c, "streak") + 1 else 1
        val e = p(c).edit().putString("lastDay", t).putInt("streak", s)
        if (s % 7 == 0) e.putInt("stars", n(c, "stars") + 1)
        e.apply()
    }
    private fun gain(c: Context, subject: String, pts: Int) {
        add(c, when (subject) { "Workout" -> "str"; "Reading and writing" -> "foc"; else -> "int" }, pts)
    }

    // Hazards
    private fun ss(c: Context, k: String): MutableSet<String> = (p(c).getStringSet(k, emptySet()) ?: emptySet()).toMutableSet()
    fun isHazard(c: Context, pkg: String): Boolean {
        if (pkg == c.packageName) return false
        if (pkg in ss(c, "hzOff")) return false
        if (pkg in ss(c, "hzOn")) return true
        if (pkg in ALLOW) return false
        if (pkg in ENT) return true
        return try {
            val cat = c.packageManager.getApplicationInfo(pkg, 0).category
            cat == ApplicationInfo.CATEGORY_GAME || cat == ApplicationInfo.CATEGORY_VIDEO || cat == ApplicationInfo.CATEGORY_SOCIAL
        } catch (e: Exception) { false }
    }
    fun toggleHazard(c: Context, pkg: String) {
        val on = ss(c, "hzOn"); val off = ss(c, "hzOff")
        if (isHazard(c, pkg)) { on.remove(pkg); off.add(pkg) } else { off.remove(pkg); on.add(pkg) }
        p(c).edit().putStringSet("hzOn", on).putStringSet("hzOff", off).apply()
    }
    fun blocked(c: Context, pkg: String): Boolean {
        if (pkg == c.packageName || pkg == "com.android.systemui") return false
        if (pkg in STRICT) return gateOn(c) || labor(c)
        if (!isHazard(c, pkg)) return false
        return isSleep(c) || gateOn(c) || labor(c) || now() < lockEnd(c) || !passOn(c)
    }

    // Gates
    fun enterGate(c: Context, subject: String, mins: Int, drill: Boolean) {
        p(c).edit().putLong("gateEnd", now() + mins * 60000L).putString("gSubj", subject).putInt("gMin", mins).putBoolean("gDrill", drill).apply()
    }
    private fun checkDay(c: Context): String {
        val t = day()
        if ((p(c).getString("okDay", "") ?: "") == t) return ""
        val gates = if ((p(c).getString("gDay", "") ?: "") == t) n(c, "gCnt") else 0
        if (gates >= 1 && quests(c).all { questDone(c, it) }) {
            p(c).edit().putString("okDay", t).putInt("tokens", tokens(c) + 1).apply()
            return " DAILY REQUIREMENTS MET: +1 TOKEN."
        }
        return ""
    }
    fun completeGate(c: Context, evidence: String): String {
        if (!gateReady(c)) return "GATE STILL RUNNING."
        if (words(evidence) < 3) return "EVIDENCE REQUIRED: 3 OR MORE WORDS. NO EVIDENCE, NO REWARD."
        val m = gateMinutes(c); val xpG = maxOf(10, m / 2); val goldG = m * 4
        touch(c)
        val t = day(); val cnt = if ((p(c).getString("gDay", "") ?: "") == t) n(c, "gCnt") + 1 else 1
        val oldL = level(xp(c))
        val e = p(c).edit().putInt("xp", xp(c) + xpG).putInt("gold", gold(c) + goldG).putInt("tokens", tokens(c) + 1)
            .putLong("gateEnd", 0).putBoolean("gDrill", false).putString("gDay", t).putInt("gCnt", cnt)
        if (gateIsDrill(c)) e.putString("drill", "")
        e.apply(); gain(c, gateSubject(c), maxOf(1, m / 30)); add(c, "foc", 2); add(c, "str", 1)
        val newL = level(xp(c))
        val lvlUp = if (newL > oldL) " [LEVEL UP! LV.$newL (${rank(newL)}-RANK)]" else ""
        return "GATE CLEARED: +$xpG XP, +$goldG GOLD, +1 TOKEN.$lvlUp" + checkDay(c)
    }
    fun abandonGate(c: Context): String {
        val f = fatigue(c) + 25
        val subj = gateSubject(c).ifEmpty { "Mathematics" }
        val e = p(c).edit().putLong("gateEnd", 0).putBoolean("labor", true).putString("drill", subj)
        if (f >= 100) e.putInt("fatigue", 0).putLong("lockEnd", now() + 24 * 3600000L) else e.putInt("fatigue", f)
        e.apply()
        return "GATE ABANDONED. FATIGUE +25. DIGITAL LABOR ACTIVE. 90-MINUTE DRILL OWED: $subj."
    }

    // Entertainment tokens
    fun useToken(c: Context): String {
        if (isSleep(c)) return "SLEEP MODE. ENTERTAINMENT DENIED."
        if (gateOn(c)) return "GATE ACTIVE. ENTERTAINMENT DENIED."
        if (labor(c)) return "DIGITAL LABOR ACTIVE. COMPLETE IT FIRST."
        if (now() < lockEnd(c)) return "FATIGUE LOCKOUT ACTIVE."
        if (tokens(c) < 1) return "NO TOKENS. CLEAR A GATE OR ALL DAILY QUESTS."
        p(c).edit().putInt("tokens", tokens(c) - 1).putLong("passEnd", now() + PASS_MIN * 60000L).apply()
        return "TOKEN SPENT. $PASS_MIN MINUTES. THE CLOCK DOES NOT STOP."
    }
    fun buyToken(c: Context): String {
        if (level(xp(c)) < MIN_LEVEL_FOR_TOKEN) return "RANK TOO LOW. REACH LEVEL $MIN_LEVEL_FOR_TOKEN."
        if (gold(c) < TOKEN_COST) return "INSUFFICIENT GOLD."
        p(c).edit().putInt("gold", gold(c) - TOKEN_COST).putInt("tokens", tokens(c) + 1).apply()
        return "TOKEN PURCHASED."
    }
    fun redeemStar(c: Context): String {
        if (isSleep(c)) return "SLEEP MODE. ENTERTAINMENT DENIED."
        if (gateOn(c) || labor(c)) return "DENIED WHILE A GATE OR PENALTY IS ACTIVE."
        if (stars(c) < 1) return "NO STAR TOKENS. SEVEN ACTIVE DAYS IN A ROW EARNS ONE."
        p(c).edit().putInt("stars", stars(c) - 1).putLong("passEnd", now() + STAR_MIN * 60000L).apply()
        return "STAR TOKEN SPENT. $STAR_MIN MINUTES."
    }

    // Quests
    fun quests(c: Context): List<String> {
        val s = p(c).getString("quests", "") ?: ""
        return if (s.isBlank()) listOf("Complete assigned homework", "Revise today's lessons", "Solve past-paper questions") else s.split("
").filter { it.isNotBlank() }
    }
    fun addQuest(c: Context, t: String) { p(c).edit().putString("quests", (quests(c) + t.trim()).joinToString("
")).apply() }
    fun removeQuest(c: Context, t: String) { p(c).edit().putString("quests", quests(c).filter { it != t }.joinToString("
")).apply() }
    fun importLines(c: Context, text: String): Int {
        val have = quests(c).map { it.lowercase() }.toMutableSet(); var added = 0
        val strip = Regex("^\\s*(?:[-*•]\\s*)?(?:\\d{1,2}[:.]\\d{2}\\s*(?:am|pm)?\\s*(?:[-–—]|to)?\\s*(?:\\d{1,2}[:.]\\d{2}\\s*(?:am|pm)?)?\\s*[:\\-–—]?\\s*)?", RegexOption.IGNORE_CASE)
        for (raw in text.lines()) {
            val t = raw.replace(Regex("[*#|`]"), " ").replace(strip, "").trim()
            if (t.length < 4 || t.lowercase() in have || added >= 12) continue
            addQuest(c, t); have.add(t.lowercase()); added++
        }
        return added
    }
    fun suggestXp(t: String): Int {
        val s = t.lowercase()
        return when {
            listOf("math", "physic", "chem", "code", "comput", "program").any { it in s } -> 30
            listOf("english", "kiswahili", "swahili", "essay", "csl", "community").any { it in s } -> 25
            listOf("workout", "exercise", "gym", "run").any { it in s } -> 15
            "read" in s -> 10
            else -> 15
        }
    }
    fun questDone(c: Context, t: String) = p(c).getString("q" + t.hashCode(), "") == day()
    fun doQuest(c: Context, t: String, evidence: String): String {
        if (questDone(c, t)) return "ALREADY CLEARED TODAY."
        if (words(evidence) < 3) return "EVIDENCE REQUIRED: 3 OR MORE WORDS. NO EVIDENCE, NO REWARD."
        val x = suggestXp(t); val s = t.lowercase()
        touch(c)
        val oldL = level(xp(c))
        p(c).edit().putString("q" + t.hashCode(), day()).putInt("xp", xp(c) + x).putInt("gold", gold(c) + x * 3).apply()
        gain(c, if ("workout" in s || "exercise" in s) "Workout" else if ("read" in s) "Reading and writing" else "Mathematics", 1)
        add(c, "agi", 1)
        val newL = level(xp(c))
        val lvlUp = if (newL > oldL) " [LEVEL UP! LV.$newL (${rank(newL)}-RANK)]" else ""
        return "QUEST CLEARED: +$x XP, +${x * 3} GOLD.$lvlUp" + checkDay(c)
    }

    fun settle(c: Context): String? {
        val t = day(); val s = p(c).getString("settled", "") ?: ""
        if (s == t) return null
        p(c).edit().putString("settled", t).apply()
        if (s.isEmpty() || (p(c).getString("okDay", "") ?: "") == s) return null
        val subj = drill(c).ifEmpty { "Mathematics" }
        p(c).edit().putBoolean("labor", true).putString("drill", subj).putInt("fatigue", minOf(99, fatigue(c) + 15)).putInt("streak", 0).apply()
        return "YESTERDAY'S REQUIREMENTS WERE NOT MET. PENALTY QUEST ISSUED: FATIGUE +15, STREAK RESET, DIGITAL LABOR ACTIVE, 90-MINUTE DRILL OWED ON $subj."
    }

    // One person, one data sync
    fun exportAll(c: Context): String {
        val j = JSONObject()
        for ((k, v) in p(c).all) { if (v is Set<*>) j.put(k, JSONArray(v.toList())) else j.put(k, v) }
        return Base64.encodeToString(j.toString().toByteArray(), Base64.NO_WRAP)
    }
    fun importAll(c: Context, code: String): Boolean {
        return try {
            val str = String(Base64.decode(code.trim(), Base64.NO_WRAP))
            val j = JSONObject(str)
            val e = p(c).edit().clear()
            val keys = j.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (k in LONGS) e.putLong(k, j.getLong(k))
                else when (val v = j.get(k)) {
                    is Int -> e.putInt(k, v)
                    is Long -> e.putLong(k, v)
                    is Boolean -> e.putBoolean(k, v)
                    is String -> e.putString(k, v)
                    is JSONArray -> {
                        val set = mutableSetOf<String>()
                        for (i in 0 until v.length()) set.add(v.getString(i))
                        e.putStringSet(k, set)
                    }
                }
            }
            e.apply(); true
        } catch (ex: Exception) { false }
    }
}
