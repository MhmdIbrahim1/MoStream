package com.lagradost.cloudstream3.utils

sealed class NetworkResult<T>(
    val data: T? = null,
    val message: String? = null
){
    class Success<T>(data: T?): NetworkResult<T>(data)
    class Error<T>(message: String?):NetworkResult<T>(message = message)
    class Loading<T>(): NetworkResult<T>()
    class UnSpecified<T>(): NetworkResult<T>()
}
