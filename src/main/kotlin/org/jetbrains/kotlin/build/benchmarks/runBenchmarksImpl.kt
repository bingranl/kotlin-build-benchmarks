package org.jetbrains.kotlin.build.benchmarks/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.build.benchmarks.dsl.Suite
import org.jetbrains.kotlin.build.benchmarks.dsl.Tasks
import org.jetbrains.kotlin.build.benchmarks.evaluation.gradle.GradleBenchmarkEvaluator
import org.jetbrains.kotlin.build.benchmarks.evaluation.results.CompactResultListener
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

internal val DEFAULT_TASKS = arrayOf(Tasks.DIST, Tasks.COMPILER_TEST_CLASSES, Tasks.IDEA_TEST_CLASSES)

internal fun mainImpl(benchmarks: Suite) {
    val eval = GradleBenchmarkEvaluator(File("../kotlin")).apply {
        addListener(SimpleLoggingBenchmarkListener())
        val dir = File("build/benchmark-results").apply { mkdirs() }

        val time = SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(Calendar.getInstance().time)
        val compactResultFile = dir.resolve("$time.result.bin")
        addListener(CompactResultListener(compactResultFile))

        buildLogsOutputStreamProvider = { scenario, step, repeat ->
            val file = File(dir, "$time-build-$scenario-#$repeat-$step.log")
            file.parentFile.mkdirs()
            FileOutputStream(file)
        }
    }
    eval.runBenchmarks(benchmarks)
}