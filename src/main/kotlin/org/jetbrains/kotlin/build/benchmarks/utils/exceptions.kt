/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.benchmarks.utils

import java.io.PrintWriter
import java.io.StringWriter

fun Exception.stackTraceString(): String =
    StringWriter().also { sw ->
        printStackTrace(PrintWriter(sw))
    }.toString()