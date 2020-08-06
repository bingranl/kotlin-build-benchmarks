/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package benchmarks.evaluation.gradle

import benchmarks.dsl.Scenario
import benchmarks.dsl.Step
import benchmarks.dsl.Suite
import benchmarks.dsl.Tasks
import benchmarks.evaluation.AbstractBenchmarkEvaluator
import benchmarks.evaluation.BuildResult
import benchmarks.evaluation.results.MutableMetricsContainer
import benchmarks.evaluation.results.StepResult
import benchmarks.evaluation.results.ValueMetric
import benchmarks.utils.Either
import benchmarks.utils.TimeInterval
import benchmarks.utils.mapSuccess
import benchmarks.utils.stackTraceString
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.jetbrains.kotlin.gradle.internal.build.metrics.GradleBuildMetricsData
import java.io.File
import java.io.ObjectInputStream
import java.util.*
import kotlin.collections.LinkedHashMap

class GradleBenchmarkEvaluator : AbstractBenchmarkEvaluator() {
    private lateinit var c: ProjectConnection

    override fun runBenchmarks(benchmarks: Suite) {
        val root = File(".").absoluteFile
        c = GradleConnector.newConnector().forProjectDirectory(root).connect()

        try {
            super.runBenchmarks(benchmarks)
        } finally {
            c.close()
        }
    }

    override fun runBuild(suite: Suite, scenario: Scenario, step: Step): Either<StepResult> {
        val tasksToExecute = step.tasks ?: suite.defaultTasks
        return runBuild(tasksToExecute, step.isExpectedToFail)
            .mapSuccess { metrics -> StepResult(step, metrics) }
    }

    override fun runBuild(tasksToExecute: Array<Tasks>, isExpectedToFail: Boolean): Either<BuildResult> {
        val tasksPaths = tasksToExecute.map { it.path }.toTypedArray()

        val gradleBuildListener = BuildRecordingProgressListener()
        val metricsFile = File.createTempFile("kt-benchmarks-", "-metrics").apply { deleteOnExit() }

        try {
            progress.taskExecutionStarted(tasksToExecute)
            c.newBuild()
                .forTasks(*tasksPaths)
                .addArguments("-Pkotlin.internal.single.build.metrics.file=${metricsFile.absolutePath}")
                .addProgressListener(gradleBuildListener).run()
        } catch (e: Exception) {
            if (!isExpectedToFail) {
                return Either.Failure(e)
            }
        }

        val timeMetrics = MutableMetricsContainer<TimeInterval>()
        timeMetrics[GradlePhasesMetrics.GRADLE_BUILD] = gradleBuildListener.allBuildTime
        timeMetrics[GradlePhasesMetrics.CONFIGURATION] = gradleBuildListener.configTime
        timeMetrics[GradlePhasesMetrics.EXECUTION] = gradleBuildListener.taskExecutionTime
        // todo: split inputs and outputs checks time
        timeMetrics[GradlePhasesMetrics.UP_TO_DATE_CHECKS] =
            gradleBuildListener.snapshotBeforeTaskTime + gradleBuildListener.snapshotAfterTaskTime
        timeMetrics[GradlePhasesMetrics.UP_TO_DATE_CHECKS_BEFORE_TASK] = gradleBuildListener.snapshotBeforeTaskTime
        timeMetrics[GradlePhasesMetrics.UP_TO_DATE_CHECKS_AFTER_TASK] = gradleBuildListener.snapshotAfterTaskTime

        if (metricsFile.exists() && metricsFile.length() > 0) {
            try {
                val buildData = ObjectInputStream(metricsFile.inputStream().buffered()).use { input ->
                    input.readObject() as GradleBuildMetricsData
                }
                addTaskExecutionData(timeMetrics, buildData, gradleBuildListener.taskTimes, gradleBuildListener.javaInstrumentationTimeMs)
            } catch (e: Exception) {
                System.err.println("Could not read metrics: ${e.stackTraceString()}")
            } finally {
                metricsFile.delete()
            }
        }

        return Either.Success(BuildResult(timeMetrics))
    }

    private fun addTaskExecutionData(
        timeMetrics: MutableMetricsContainer<TimeInterval>,
        buildData: GradleBuildMetricsData,
        taskTimes: Map<String, TimeInterval>,
        javaInstrumentationTime: TimeInterval
    ) {
        var compilationTime = TimeInterval(0)
        var nonCompilationTime = TimeInterval(0)

        val taskDataByType = buildData.taskData.values.groupByTo(TreeMap()) { shortTaskTypeName(it.typeFqName) }
        for ((typeFqName, tasksData) in taskDataByType) {
            val aggregatedTimeNs = LinkedHashMap<String, Long>()
            var timeForTaskType = TimeInterval(0)
            fun replaceRootName(name: String) = if (buildData.parentMetric[name] == null) typeFqName else name

            for (taskData in tasksData) {
                if (!taskData.didWork) continue

                timeForTaskType += taskTimes.getOrElse(taskData.path) { TimeInterval(0) }

                for ((metricName, timeNs) in taskData.timeMetrics) {
                    if (timeNs <= 0) continue

                    // replace root metric name with task type fq name
                    val name = replaceRootName(metricName)
                    aggregatedTimeNs[name] = aggregatedTimeNs.getOrDefault(name, 0L) + timeNs
                }
            }
            val taskTypeContainer = MutableMetricsContainer<TimeInterval>()
            for ((metricName, timeNs) in aggregatedTimeNs) {
                val parentName = buildData.parentMetric[metricName]?.let { replaceRootName(it) }
                val value = ValueMetric(TimeInterval.ns(timeNs))
                taskTypeContainer.set(metricName, value, parentName)
            }
            if (typeFqName == "JavaCompile") {
                taskTypeContainer.set("Not null instrumentation", ValueMetric(javaInstrumentationTime), "JavaCompile")
            }

            val parentMetric = if (typeFqName in compileTasksTypes) {
                compilationTime += timeForTaskType
                GradlePhasesMetrics.COMPILATION_TASKS.name
            } else {
                nonCompilationTime += timeForTaskType
                GradlePhasesMetrics.NON_COMPILATION_TASKS.name
            }
            timeMetrics.set(typeFqName, taskTypeContainer, parentMetric = parentMetric)
        }

        timeMetrics[GradlePhasesMetrics.COMPILATION_TASKS] = compilationTime
        timeMetrics[GradlePhasesMetrics.NON_COMPILATION_TASKS] = nonCompilationTime
    }

    private val compileTasksTypes = setOf("JavaCompile", "KotlinCompile", "KotlinCompileCommon", "Kotlin2JsCompile")

    private fun shortTaskTypeName(fqName: String) =
        fqName.substringAfterLast(".").removeSuffix("_Decorated")
}
