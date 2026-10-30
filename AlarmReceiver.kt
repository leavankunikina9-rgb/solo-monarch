package com.sovereign.solo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Store.settle(context)
        RankWidget.update(context)
    }
}
