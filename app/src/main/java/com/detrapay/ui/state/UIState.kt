package com.detrapay.ui.state

sealed class UIState<T>(val data: T? = null, val message: String? = null, val exception: Exception? = null) {
    class Success<T>(data: T) : UIState<T>(data)
    class Error<T>(message: String, exception: Exception? = null, data: T? = null) : UIState<T>(data, message, exception)
    class Loading<T>(data: T? = null) : UIState<T>(data)
}
