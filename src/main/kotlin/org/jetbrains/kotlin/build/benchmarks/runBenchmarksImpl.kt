/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.benchmarks

import org.jetbrains.kotlin.build.benchmarks.dsl.Suite
import org.jetbrains.kotlin.build.benchmarks.dsl.Tasks
import org.jetbrains.kotlin.build.benchmarks.evaluation.gradle.GradleBenchmarkEvaluator
import org.jetbrains.kotlin.build.benchmarks.evaluation.results.CompactResultListener
import org.jetbrains.kotlin.build.benchmarks.evaluation.results.TeamCityMetricReporter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

internal val DEFAULT_TASKS = arrayOf(Tasks.DIST, Tasks.COMPILER_TEST_CLASSES, Tasks.IDEA_TEST_CLASSES)

internal fun mainImpl(benchmarksProvider: (projectName: String, defaultTasksToRun: Array<Tasks>) -> Suite) {
    val isTeamCityRun = System.getenv("TEAMCITY_VERSION") != null
    val testedProjectPath = System.getenv("TESTED_PROJECT_PATH") ?: "../kotlin/"
    val testedProjectName = System.getenv("TESTED_PROJECT_NAME") ?: "kotlin"
    val benchmarks = benchmarksProvider(testedProjectName, DEFAULT_TASKS)

    val eval = GradleBenchmarkEvaluator(File(testedProjectPath)).apply {
        if (isTeamCityRun) {
            addListener(TeamCityMetricReporter())
        } else {
            addListener(SimpleLoggingBenchmarkListener())
        }
        val dir = File("build/benchmark-results").apply { mkdirs() }

        val time = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().time)
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