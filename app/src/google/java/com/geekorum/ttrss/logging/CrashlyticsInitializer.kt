/*
 * Geekttrss is a RSS feed reader application on the Android Platform.
 *
 * Copyright (C) 2017-2019 by Frederic-Charles Barthelery.
 *
 * This file is part of Geekttrss.
 *
 * Geekttrss is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Geekttrss is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Geekttrss.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.geekorum.ttrss.logging

import android.app.Application
import android.os.StrictMode.allowThreadDiskWrites
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.geekorum.geekdroid.dagger.AppInitializer
import com.geekorum.ttrss.BuildConfig
import com.geekorum.ttrss.debugtools.withStrictMode
import io.fabric.sdk.android.Fabric

/**
 * [AppInitializer] to initialize crashlytics
 */
class CrashlyticsInitializer : AppInitializer {
    override fun initialize(app: Application) {
        withStrictMode(allowThreadDiskWrites()) {
            val crashlytics = Crashlytics.Builder()
                .core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build()
            Fabric.with(app, crashlytics)
        }
    }
}
