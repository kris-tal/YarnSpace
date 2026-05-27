package com.yarnspace.app.feature.notifs.work

interface NotifsWorkScheduler {
    /**
     * Ensures a background polling job is scheduled to run again in the future.
     * (We implement this as a one-time work that self-reschedules.)
     */
    fun scheduleUnreadPolling()

    /** Run an unread check as soon as possible (used on login and when opening the notifs UI). */
    fun triggerUnreadCheckNow()

    fun cancelUnreadPolling()
}


