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
package com.geekorum.ttrss.accounts

import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.databinding.InverseMethod
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geekorum.geekdroid.app.lifecycle.Event
import com.geekorum.ttrss.R
import com.geekorum.ttrss.network.ApiCallException
import com.geekorum.ttrss.network.impl.LoginRequestPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import retrofit2.HttpException
import javax.inject.Inject

/**
 * ViewModel for LoginActivity
 */
internal class LoginViewModel @Inject constructor(
    private val accountManager: TinyrssAccountManager,
    private val networkComponentBuilder: AuthenticatorNetworkComponent.Builder
) : ViewModel() {

    var username = ""
    var password = ""
    var http_auth_username = ""
    var http_auth_password = ""
    var httpUrl: HttpUrl? = null
    private lateinit var action: String
    private var account: Account? = null

    val loginInProgress = MutableLiveData<Boolean>()
    val loginFailedEvent = MutableLiveData<Event<LoginFailedError>>()
    val actionCompleteEvent = MutableLiveData<Event<ActionCompleteEvent>>()
    val fieldErrors = MutableLiveData<FieldErrorStatus>().apply {
        value = FieldErrorStatus()
    }

    val areFieldsCorrect = Transformations.map(fieldErrors) {
        val editionDone = (it.hasEditAllFields || (!canChangeUsernameOrUrl && it.hasEditPassword))
        editionDone && it.areFieldsCorrect
    }

    val canChangeUsernameOrUrl: Boolean
        get() = (action != LoginActivity.ACTION_CONFIRM_CREDENTIALS)

    fun initialize(action: String, account: Account? = null) {
        check(action in listOf(LoginActivity.ACTION_ADD_ACCOUNT, LoginActivity.ACTION_CONFIRM_CREDENTIALS)) {
            "unknown action"
        }
        this.action = action
        this.account = account
        username = account?.username ?: ""
        httpUrl = HttpUrl.parse(account?.url ?: "")
    }

    fun checkValidUrl(text: CharSequence) {
        val current = checkNotNull(fieldErrors.value)
        val invalidUrlMsgId = when {
            text.isEmpty() -> R.string.error_field_required
            httpUrl == null -> R.string.error_invalid_http_url
            httpUrl?.pathSegments()?.last()?.isNotEmpty() == true -> R.string.error_http_url_must_end_wish_slash
            else -> null
        }
        fieldErrors.value = current.copy(invalidUrlMsgId = invalidUrlMsgId, hasEditUrl = true)
    }

    fun checkNonEmptyPassword(text: CharSequence) {
        val current = checkNotNull(fieldErrors.value)
        val invalidPasswordMsgId = when {
            text.isEmpty() -> R.string.error_field_required
            else -> null
        }
        fieldErrors.value = current.copy(invalidPasswordMsgId = invalidPasswordMsgId, hasEditPassword = true)
    }

    fun checkNonEmptyUsername(text: CharSequence) {
        val current = checkNotNull(fieldErrors.value)
        val invalidNameMsgId = when {
            text.isEmpty() -> R.string.error_field_required
            else -> null
        }
        fieldErrors.value = current.copy(invalidNameMsgId = invalidNameMsgId, hasEditName = true)
    }

    private fun checkFieldsCorrect(): Boolean {
        val current = checkNotNull(fieldErrors.value)
        fieldErrors.value = current.copy(hasAttemptLogin = true)
        return httpUrl != null &&  fieldErrors.value!!.areFieldsCorrect
    }

    @JvmOverloads
    fun confirmLogin(id: Int = EditorInfo.IME_NULL): Boolean {
        val handleAction = (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL)
        if (handleAction && checkFieldsCorrect()) {
            viewModelScope.launch {
                doLogin()
            }
        }
        return handleAction
    }

    @VisibleForTesting
    internal suspend fun doLogin() {
        val serverInformation = DataServerInformation(httpUrl!!.toString(), http_auth_username, http_auth_password)
        val urlModule = TinyRssServerInformationModule(serverInformation)
        val networkComponent = networkComponentBuilder
            .tinyRssServerInformationModule(urlModule)
            .build()
        val tinyRssApi = networkComponent.getTinyRssApi()
        val loginPayload = LoginRequestPayload(username, password)

        try {
            loginInProgress.value = true
            val response = tinyRssApi.login(loginPayload).await()

            when {
                response.isStatusOk -> onUserLoggedIn()

                response.error == ApiCallException.ApiError.LOGIN_FAILED
                        || response.error == ApiCallException.ApiError.NOT_LOGGED_IN ->
                    loginFailedEvent.value = LoginFailedEvent(R.string.error_login_failed)

                response.error == ApiCallException.ApiError.API_DISABLED ->
                    loginFailedEvent.value = LoginFailedEvent(R.string.error_api_disabled)

                else -> loginFailedEvent.value = LoginFailedEvent(R.string.error_unknown)
            }
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> loginFailedEvent.value = LoginFailedEvent(R.string.error_http_unauthorized)
                403 -> loginFailedEvent.value = LoginFailedEvent(R.string.error_http_forbidden)
                404 -> loginFailedEvent.value = LoginFailedEvent(R.string.error_http_not_found)
                else -> loginFailedEvent.value = LoginFailedEvent(R.string.error_unknown)
            }
        } catch (e: Exception) {
            loginFailedEvent.value = LoginFailedEvent(R.string.error_unknown)
        } finally {
            loginInProgress.value = false
        }
    }


    private fun onUserLoggedIn() {
        when (action) {
            LoginActivity.ACTION_CONFIRM_CREDENTIALS -> onConfirmCredentialsSuccess()
            LoginActivity.ACTION_ADD_ACCOUNT -> onAddAccountSuccess()
        }
    }

    private fun onConfirmCredentialsSuccess() {
        accountManager.updatePassword(account!!, password)
        actionCompleteEvent.value = ActionCompleteSuccessEvent(account!!)
    }

    private fun onAddAccountSuccess() = viewModelScope.launch {
        val result = addAccount()
        actionCompleteEvent.value = if (result != null) {
            ActionCompleteSuccessEvent(result)
        } else {
            ActionCompleteFailedEvent()
        }
    }

    private suspend fun addAccount(): Account? {
        return withContext(Dispatchers.IO) {
            val account = Account(username, httpUrl!!.toString())
            val success = accountManager.addAccount(account, password)
            if (success) {
                accountManager.initializeAccountSync(account)
                return@withContext account
            }
            return@withContext null
        }
    }

    private data class DataServerInformation(
        override val apiUrl: String,
        override val basicHttpAuthUsername: String? = null,
        override val basicHttpAuthPassword: String? = null
    ) : ServerInformation()

    data class LoginFailedError(@StringRes val errorMsgId: Int)
    private fun LoginFailedEvent(errorMsgId: Int) = Event(LoginFailedError(errorMsgId))

    sealed class ActionCompleteEvent {
        class Success(val account: Account) : ActionCompleteEvent()
        class Failed : ActionCompleteEvent()
    }

    private fun ActionCompleteSuccessEvent(account: Account) = Event(ActionCompleteEvent.Success(account))
    private fun ActionCompleteFailedEvent() = Event(ActionCompleteEvent.Failed())

    data class FieldErrorStatus(
        val invalidUrlMsgId: Int? = null,
        val invalidNameMsgId: Int? = null,
        val invalidPasswordMsgId: Int? = null,
        val hasEditUrl: Boolean = false,
        val hasEditPassword: Boolean = false,
        val hasEditName: Boolean = false,
        val hasAttemptLogin: Boolean = false
    ) {
        val areFieldsCorrect= (invalidNameMsgId == null && invalidPasswordMsgId == null && invalidUrlMsgId == null)

        val hasEditAllFields = (hasEditUrl && hasEditName && hasEditPassword)
    }

    companion object {

        @JvmStatic
        @InverseMethod("convertHttpUrlToString")
        fun convertStringToHttpUrl(url: String): HttpUrl? {
            return HttpUrl.parse(url)?.newBuilder() // remove fragment and query
                ?.query(null)
                ?.encodedFragment(null)
                ?.build()
        }

        @JvmStatic
        fun convertHttpUrlToString(url: HttpUrl?): String {
            // this method is only called when binding the model to the view
            // this doesn't happen when the user modify the content of the text field
            // so basically it only happen with url == null :
            // - on first initialisation
            // - on view recreation ?
            return url?.toString() ?: "https://"
        }

    }
}
