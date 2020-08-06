/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.benchmarks.evaluation

import org.jetbrains.kotlin.build.benchmarks.dsl.Scenario
import org.jetbrains.kotlin.build.benchmarks.dsl.Step
import org.jetbrains.kotlin.build.benchmarks.dsl.Suite
import org.jetbrains.kotlin.build.benchmarks.dsl.Tasks
import org.jetbrains.kotlin.build.benchmarks.evaluation.results.ScenarioResult
import org.jetbrains.kotlin.build.benchmarks.evaluation.results.StepResult
import org.jetbrains.kotlin.build.benchmarks.evaluation.validation.checkBenchmarks
import org.jetbrains.kotlin.build.benchmarks.utils.Either
import java.io.File

abstract class AbstractBenchmarkEvaluator(private val projectPath: File) {
    private val changesApplier = ChangesApplier(projectPath)
    protected val progress = CompositeBenchmarksProgressListener()

    fun addListener(progressListener: BenchmarksProgressListener) {
        progress.add(progressListener)
    }

    open fun runBenchmarks(benchmarks: Suite) {
        checkBenchmarks(projectPath, benchmarks)

        try {
            scenario@ for (scenario in benchmarks.scenarios) {
                for (i in (1U..scenario.repeat.toUInt())) {
                    cleanup(benchmarks, scenario)
                    progress.scenarioStarted(scenario)

                    val stepsResults = arrayListOf<StepResult>()
                    for (step in scenario.steps) {
                        progress.stepStarted(step)

                        if (!changesApplier.applyStepChanges(step)) {
                            System.err.println("Aborting scenario: could not apply step changes")
                            continue@scenario
                        }
                        val stepResult = try {
                            runBuild(benchmarks, scenario, step)
                        } catch (e: Exception) {
                            Either.Failure(e)
                        }

                        when (stepResult) {
                            is Either.Failure -> {
                                System.err.println("Aborting scenario: step failed")
                                progress.stepFinished(step, stepResult)
                                continue@scenario
                            }
                            is Either.Success<StepResult> -> {
                                stepsResults.add(stepResult.value)
                                progress.stepFinished(step, stepResult)
                            }
                        }
                    }

                    progress.scenarioFinished(scenario, Either.Success(ScenarioResult(stepsResults)))
                }
            }
            progress.allFinished()
        } finally {
            changesApplier.revertAppliedChanges()
        }
    }

    private fun cleanup(benchmarks: Suite, scenario: Scenario) {
        if (!changesApplier.hasAppliedChanges) return

        progress.cleanupStarted()
        // ensure tasks in scenario are not affected by previous scenarios
        changesApplier.revertAppliedChanges()
        val tasksToBeRun = scenario.steps.flatMapTo(LinkedHashSet()) { (it.tasks ?: benchmarks.defaultTasks).toList() }
        tasksToBeRun.remove(Tasks.CLEAN)

        if (tasksToBeRun.isNotEmpty()) {
            runBuild(tasksToBeRun.toTypedArray())
        }
        progress.cleanupFinished()
    }

    protected abstract fun runBuild(suite: Suite, scenario: Scenario, step: Step): Either<StepResult>
    protected abstract fun runBuild(tasksToExecute: Array<Tasks>, isExpectedToFail: Boolean = false): Either<BuildResult>
}

