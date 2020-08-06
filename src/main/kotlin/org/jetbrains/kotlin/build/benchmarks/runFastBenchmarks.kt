package org.jetbrains.kotlin.build.benchmarks/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.build.benchmarks.DEFAULT_TASKS
import org.jetbrains.kotlin.build.benchmarks.mainImpl
import org.jetbrains.kotlin.build.benchmarks.scenarios.fastBenchmarks

fun main() {
    mainImpl(fastBenchmarks(*DEFAULT_TASKS))
}
