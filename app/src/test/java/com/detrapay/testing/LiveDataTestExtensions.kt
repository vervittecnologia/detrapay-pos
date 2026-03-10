package com.detrapay.testing

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

fun <T> LiveData<T>.getOrAwaitValue(
    timeout: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(value: T) {
            data = value
            latch.countDown()
            removeObserver(this)
        }
    }

    observeForever(observer)

    if (!latch.await(timeout, timeUnit)) {
        removeObserver(observer)
        throw TimeoutException("LiveData value was never set.")
    }

    @Suppress("UNCHECKED_CAST")
    return data as T
}

fun <T> LiveData<T>.getOrAwaitValueMatching(
    timeout: Long = 5,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
    predicate: (T) -> Boolean,
): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(value: T) {
            if (predicate(value)) {
                data = value
                latch.countDown()
                removeObserver(this)
            }
        }
    }

    observeForever(observer)

    if (!latch.await(timeout, timeUnit)) {
        removeObserver(observer)
        throw TimeoutException("LiveData value matching predicate was never set.")
    }

    @Suppress("UNCHECKED_CAST")
    return data as T
}
