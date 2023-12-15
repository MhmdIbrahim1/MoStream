package com.lagradost.cloudstream3.ui.loginregister

data class UserSign(
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val userStatus: String?,
    val imagePath: String? = ""
) {
    constructor() : this("", "", "", "")
}