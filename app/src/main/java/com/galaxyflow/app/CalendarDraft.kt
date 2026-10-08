package com.galaxyflow.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import java.util.Calendar

/** A user-controlled draft handoff; no Calendar write/read permission or saved-event claim. */
object CalendarDraft {
    fun intent(now: Calendar = Calendar.getInstance()): Intent {
        val start = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 16); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
            putExtra(CalendarContract.Events.TITLE, "Review work priorities")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start.timeInMillis + 15 * 60_000)
        }
    }
    fun open(activity: Activity): Boolean = try {
        activity.startActivity(intent())
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
