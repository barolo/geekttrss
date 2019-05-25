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
