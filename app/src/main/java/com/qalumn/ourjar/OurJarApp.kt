package com.qalumn.ourjar

import android.app.Application
import com.qalumn.ourjar.data.Repo
import com.qalumn.ourjar.widget.WidgetSync

class OurJarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
        WidgetSync.schedulePeriodic(this)
    }
}
