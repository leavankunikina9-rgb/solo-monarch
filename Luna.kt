package com.sovereign.solo

import android.content.Context
import android.content.Intent

/** Luna: cold, precise assistant that parses voice and text inputs. */
object Luna {
    var openPkg: String? = null

    private fun subjectOf(t: String): String? = when {
        "math" in t -> "Mathematics"; "phys" in t -> "Physics"; "chem" in t -> "Chemistry"
        "comput" in t || "cod" in t -> "Computer Science"; "english" in t -> "English"
        "swahili" in t -> "Kiswahili"; "community" in t || "csl" in t -> "Community Service Learning"
        "workout" in t || "exercise" in t -> "Workout"; "read" in t || "writ" in t -> "Reading and writing"
        else -> null
    }

    fun next(c: Context): String {
        if (Store.labor(c)) return "DIGITAL LABOR ACTIVE. COMPLETE 500-WORD DRILL."
        if (Store.isSleep(c)) return "SLEEP MODE. REST REQUIRED FOR OPTIMAL PROGRESSION."
        if (Store.gateOn(c)) return "GATE RUNNING ON ${Store.gateSubject(c)}. MAINTAIN FOCUS."
        if (Store.gateReady(c)) return "GATE CLEARED. SUBMIT EVIDENCE TO CLAIM."
        if (Store.drill(c).isNotEmpty()) return "DRILL OWED ON ${Store.drill(c)}. SAY START GATE."
        val todo = Store.quests(c).firstOrNull { !Store.questDone(c, it) }
        return if (todo != null) "NEXT QUEST: $todo. SAY START GATE TO BEGIN." else "ALL QUESTS CLEARED. REST OR BUY ENTERTAINMENT TOKEN."
    }

    fun status(c: Context): String {
        val lv = Store.level(Store.xp(c))
        return "LEVEL $lv, RANK ${Store.rank(lv)}. XP ${Store.xp(c)}, GOLD ${Store.gold(c)}, TOKENS ${Store.tokens(c)}. STREAK ${Store.streak(c)} DAYS. ${Store.phase(c)}."
    }

    fun ask(c: Context, raw: String): String {
        openPkg = null
        val t = raw.lowercase().trim()
        if (t.isEmpty()) return "SYSTEM LISTENING..."
        if (Regex("abandon|quit the gate|give up").containsMatchIn(t))
            return if (Store.gateOn(c)) Store.abandonGate(c) else "NO ACTIVE GATE."
        if (Regex("^(claim|finish|complete) .*(gate|session)").containsMatchIn(t)) {
            val ev = Regex("(?:evidence|proof|because)\\s+(.+)$").find(t)?.groupValues?.get(1) ?: ""
            return if (Store.gateReady(c)) Store.completeGate(c, ev) else "NO CLEARED GATE WAITING."
        }
        if (Regex("^(start|begin|enter|launch)").containsMatchIn(t)) {
            if (Store.labor(c)) return "CLEAR DIGITAL LABOR LOCK FIRST."
            if (Store.isSleep(c)) return "SLEEP MODE ACTIVE."
            if (Store.gateOn(c)) return "GATE ALREADY RUNNING."
            val d = Store.drill(c)
            val s = subjectOf(t) ?: (if (d.isNotEmpty()) d else null) ?: return "SPECIFY SUBJECT: MATHEMATICS, PHYSICS, CHEMISTRY, COMPUTER SCIENCE, ENGLISH, KISWAHILI, CSL, WORKOUT."
            val isDrill = d.isNotEmpty() && s == d
            val m = if (isDrill) 90 else (Regex("(\\d{2,3})").find(t)?.value?.toInt() ?: Store.gateMin(c)).coerceIn(20, 180)
            Store.enterGate(c, s, m, isDrill)
            return "DUNGEON GATE OPENED: $s FOR $m MIN. HAZARDS LOCKED."
        }
        if (Regex("buy .*token|entertainment token|buy pass").containsMatchIn(t)) return Store.buyToken(c)
        if (Regex("use .*token|spend token|start pass").containsMatchIn(t)) return Store.useToken(c)
        if (Regex("\\bstars?\\|voucher").containsMatchIn(t)) return Store.redeemStar(c)
        Regex("^add (?:a )?quest:?\\\s+(.+)$").find(t)?.let { Store.addQuest(c, it.groupValues[1]); return "QUEST ADDED. SUGGESTED XP: ${Store.suggestXp(it.groupValues[1])}." }
        Regex("^(?:tick|done|complete|finish)\\s+(.+?)\\s+(?:evidence|proof|because)\\s+(.+)$").find(t)?.let { m ->
            val key = m.groupValues[1]
            val q = Store.quests(c).firstOrNull { it.lowercase().contains(key) } ?: return "QUEST NOT FOUND MATCHING $key."
            return Store.doQuest(c, q, m.groupValues[2])
        }
        if (Regex("^(tick|done|complete)").containsMatchIn(t)) return "TO TICK A QUEST: tick homework evidence solved 10 problems."
        Regex("^open\\s+(.+)$").find(t)?.let { m ->
            val want = m.groupValues[1]
            val apps = c.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            val hit = apps.firstOrNull { it.loadLabel(c.packageManager).toString().lowercase().contains(want) } ?: return "APPLICATION NOT FOUND: $want."
            openPkg = hit.activityInfo.packageName
            return "LAUNCHING ${hit.loadLabel(c.packageManager)}."
        }
        if (Regex("status|progress|rank").containsMatchIn(t)) return status(c)
        if (Regex("next|what now").containsMatchIn(t)) return next(c)
        return "COMMAND UNRECOGNIZED. SAY HELP FOR COMMAND LIST."
    }
}
