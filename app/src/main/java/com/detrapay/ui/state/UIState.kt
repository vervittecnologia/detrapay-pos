package com.detrapay.ui.state

sealed class UIState<T>(val data: T? = null, val message: String? = null, val exception: Exception? = null) {
    class Success<T>(data: T) : UIState<T>(data)
    class Error<T>(message: String, exception: Exception? = null, data: T? = null, val retryData: Any? = null) : UIState<T>(data, message, exception)
    class Loading<T>(message: String? = null, data: T? = null) : UIState<T>(data, message)
    class Idle<T> : UIState<T>()
}
