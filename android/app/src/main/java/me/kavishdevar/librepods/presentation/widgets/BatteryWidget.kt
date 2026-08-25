/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

@file:OptIn(ExperimentalMaterial3Api::class)

package me.kavishdevar.librepods.presentation.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.compose.material3.ExperimentalMaterial3Api
import me.kavishdevar.librepods.MainActivity
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.services.ServiceManager

class BatteryWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        refresh(context)
    }

    companion object {
        fun refresh(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, BatteryWidget::class.java)
            )
            if (ids.isEmpty()) return
            val service = ServiceManager.getService()
            if (service != null) {
                service.updateBatteryWidget()
            } else {
                paintDisconnected(context, appWidgetManager, ids)
            }
        }

        private fun paintDisconnected(
            context: Context,
            appWidgetManager: AppWidgetManager,
            ids: IntArray
        ) {
            val views = RemoteViews(context.packageName, R.layout.battery_widget)
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.battery_widget, open)
            views.setTextViewText(R.id.left_battery_widget, "—")
            views.setTextViewText(R.id.right_battery_widget, "—")
            views.setTextViewText(R.id.case_battery_widget, "—")
            views.setProgressBar(R.id.left_battery_progress, 100, 0, false)
            views.setProgressBar(R.id.right_battery_progress, 100, 0, false)
            views.setProgressBar(R.id.case_battery_progress, 100, 0, false)
            views.setViewVisibility(R.id.left_charging_icon, View.GONE)
            views.setViewVisibility(R.id.right_charging_icon, View.GONE)
            views.setViewVisibility(R.id.case_charging_icon, View.GONE)
            views.setViewVisibility(R.id.widget_status, View.VISIBLE)
            views.setTextViewText(
                R.id.widget_status,
                context.getString(R.string.widget_disconnected)
            )
            appWidgetManager.updateAppWidget(ids, views)
        }
    }
}
