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
package com.geekorum.ttrss.add_feed

import android.accounts.Account
import androidx.lifecycle.ViewModel
import com.geekorum.geekdroid.dagger.ViewModelKey
import com.geekorum.ttrss.accounts.NetworkLoginModule
import com.geekorum.ttrss.accounts.PerAccount
import com.geekorum.ttrss.di.AssistedFactoriesModule
import com.geekorum.ttrss.network.TinyrssApiModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap


/**
 * Dependency injection pieces for the Sync functionality.
 *
 * AddFeedService has a SubComponent of the ApplicationComponent, that allows it to inject to [AddFeedServicer].
 * ArticleSyncAdapter has a SubSubComponent which provides the actual SyncComponent
 *
 */

@Module
abstract class AndroidInjectorsModule {

    @ContributesAndroidInjector(modules = [AddFeedComponentModule::class])
    abstract fun contributeAddFeedServiceInjector(): AddFeedService

    @ContributesAndroidInjector(modules = [ViewModelsModule::class])
    abstract fun contributeAddFeedActivityInjector(): AddFeedActivity

}


@Module(subcomponents = [AddFeedComponent::class])
private abstract class AddFeedComponentModule


@Subcomponent(modules = [AssistedFactoriesModule::class, NetworkLoginModule::class, TinyrssApiModule::class])
@PerAccount
internal interface AddFeedComponent {

    val addFeedJobFactory: AddFeedJob.Factory

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun seedAccount(account: Account): Builder

        fun build(): AddFeedComponent
    }
}


@Module
private abstract class ViewModelsModule {
    @Binds
    @IntoMap
    @ViewModelKey(AddFeedViewModel::class)
    abstract fun getAddFeedViewModel(addFeedViewModel: AddFeedViewModel): ViewModel

}
