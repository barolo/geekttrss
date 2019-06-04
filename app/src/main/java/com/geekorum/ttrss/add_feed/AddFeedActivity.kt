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
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.geekorum.geekdroid.app.BottomSheetDialogActivity
import com.geekorum.geekdroid.app.lifecycle.EventObserver
import com.geekorum.geekdroid.dagger.DaggerDelegateViewModelsFactory
import com.geekorum.ttrss.R
import com.geekorum.ttrss.databinding.ActivityAddFeedBinding
import com.geekorum.ttrss.html.FeedInformation
import dagger.android.AndroidInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Chrome share offline page as multipart/related content send in EXTRA_STREAM
 * Mime multipart can be parsed with mime4j
 * 'org.apache.james:apache-mime4j-core:0.8.1'
 * 'org.apache.james:apache-mime4j-dome:0.8.1'
 * who doesn't have much dependencies and are used by k9mail
 */
class AddFeedActivity : BottomSheetDialogActivity(), CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    @Inject
    internal lateinit var viewModelFactory: DaggerDelegateViewModelsFactory

    private lateinit var feedAdapter: FeedAdapter
    private lateinit var accountsAdapter: AccountsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        job = Job()
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProviders.of(this, viewModelFactory)[AddFeedViewModel::class.java]

        val urlString = intent.data?.toString() ?: intent.extras?.getString(Intent.EXTRA_TEXT) ?: ""

        val url = HttpUrl.parse(urlString)
        url?.let {
            viewModel.init(url)
        }

        val binding = ActivityAddFeedBinding.inflate(layoutInflater, null, false)
        binding.viewModel = viewModel
        binding.setLifecycleOwner(this)
        setContentView(binding.root)

        feedAdapter = FeedAdapter(this)
        binding.availableFeeds.adapter = feedAdapter

        accountsAdapter = AccountsAdapter(this)
        binding.availableAccounts.adapter = accountsAdapter

        viewModel.accounts.observe(this, Observer {
            val accounts = checkNotNull(it)
            with(accountsAdapter) {
                clear()
                addAll(*accounts)
            }
        })

        viewModel.availableFeeds.observe(this, Observer {
            val feeds = checkNotNull(it)
            val text = feeds.firstOrNull()?.title ?: getString(R.string.activity_add_feed_no_feeds_available)
            binding.availableFeedsSingle.text = text
            binding.loadingProgress.hide()
            with(feedAdapter) {
                clear()
                addAll(feeds)
            }

            if (feeds.isEmpty()) {
                launch {
                    delay(TimeUnit.MILLISECONDS.toMillis(1500))
                    finish()
                }
            }
        })

        viewModel.complete.observe(this, EventObserver {
            finish()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

private class FeedAdapter(context: Context) :
    ArrayAdapter<FeedInformation>(context, android.R.layout.simple_dropdown_item_1line, mutableListOf()) {
    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = super.getView(position, convertView, parent) as TextView
        val item = checkNotNull(getItem(position))
        v.text = item.title
        return v
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = super.getDropDownView(position, convertView, parent) as TextView
        val item = checkNotNull(getItem(position))
        v.text = item.title
        return v
    }
}


private class AccountsAdapter(context: Context) : ArrayAdapter<Account>(context, R.layout.item_choose_account, R.id.account_row_text, mutableListOf()) {
    init {
        setDropDownViewResource(R.layout.item_choose_account)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = super.getView(position, convertView, parent)
        val imageView = v.findViewById<ImageView>(R.id.account_row_icon)
        imageView.setImageResource(R.mipmap.ic_launcher)
        val textView = v.findViewById<TextView>(R.id.account_row_text)
        val item = checkNotNull(getItem(position))
        textView.text = item.name
        return v
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = super.getDropDownView(position, convertView, parent)
        val imageView = v.findViewById<ImageView>(R.id.account_row_icon)
        imageView.setImageResource(R.mipmap.ic_launcher)
        val textView = v.findViewById<TextView>(R.id.account_row_text)
        val item = checkNotNull(getItem(position))
        textView.text = item.name
        return v
    }
}
