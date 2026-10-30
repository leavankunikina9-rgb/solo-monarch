package com.sovereign.solo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class RankWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget)
            val lv = Store.level(Store.xp(context))
            views.setTextViewText(R.id.w_head, "[!] STATUS Matrix")
            views.setTextViewText(R.id.w_rank, "LEVEL $lv (${Store.rank(lv)}-RANK)")
            views.setTextViewText(R.id.w_job, "JOB: ${Store.job(context)} | ${Store.title(context)}")
            views.setTextViewText(R.id.w_goal, Store.goalLine(context))

            val pi = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.w_head, pi)
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    companion object {
        fun update(context: Context) {
            val intent = Intent(context, RankWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, RankWidget::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
