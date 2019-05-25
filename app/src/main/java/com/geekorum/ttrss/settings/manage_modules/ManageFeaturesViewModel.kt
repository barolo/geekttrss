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
package com.geekorum.ttrss.settings.manage_modules

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.geekorum.ttrss.features_manager.Features
import com.geekorum.ttrss.features_manager.ImmutableModuleManager
import com.geekorum.ttrss.features_manager.OnDemandModuleManager
import javax.inject.Inject

class ManageFeaturesViewModel @Inject constructor(
    private val moduleManager: OnDemandModuleManager
) : ViewModel() {
    private val moduleStatus = MutableLiveData<List<FeatureStatus>>().apply {
        value = Features.allFeatures.map {
            FeatureStatus(it,
                it in moduleManager.installedModules
            )
        }
    }

    val canModify: LiveData<Boolean> = MutableLiveData<Boolean>().apply {
        value = moduleManager !is ImmutableModuleManager
    }

    val features: LiveData<List<FeatureStatus>> = moduleStatus

    fun installModule(module: String) {

    }

    fun uninstallModule(module: String) {
        moduleManager.uninstall(module)
        refreshModuleStatus()
    }

    private fun refreshModuleStatus() {
        moduleStatus.value = Features.allFeatures.map {
            FeatureStatus(it,
                it in moduleManager.installedModules
            )
        }

    }
}

data class FeatureStatus(
    val name: String,
    val installed: Boolean
)