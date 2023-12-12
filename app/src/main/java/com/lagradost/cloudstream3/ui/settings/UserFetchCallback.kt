package com.lagradost.cloudstream3.ui.settings

import com.lagradost.cloudstream3.ui.loginregister.UserSign

interface UserFetchCallback {
    fun onLoading()
    fun onSuccess(user: UserSign)
    fun onError(errorMessage: String)
}
