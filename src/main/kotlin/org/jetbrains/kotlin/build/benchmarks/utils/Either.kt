/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.benchmarks.utils

sealed class Either<out T> {
    class Success<T>(val value: T) : Either<T>()
    class Failure(val reason: String) : Either<Nothing>() {
        constructor(e: Exception) : this(e.stackTraceString())
    }
}

inline fun <reified T, reified R> Either<T>.mapSuccess(fn: (T) -> R): Either<R> =
    when (this) {
        is Either.Success<T> -> Either.Success(fn(value))
        is Either.Failure -> this
    }

