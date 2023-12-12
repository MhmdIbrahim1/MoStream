package com.lagradost.cloudstream3.ui.loginregister

data class UserSign(
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val imagePath: String? = ""
) {
    constructor() : this("", "", "", "")
}