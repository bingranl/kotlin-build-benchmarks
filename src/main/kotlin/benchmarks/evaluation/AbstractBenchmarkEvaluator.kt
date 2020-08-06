/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package benchmarks.evaluation

import benchmarks.dsl.Scenario
import benchmarks.dsl.Step
import benchmarks.dsl.Suite
import benchmarks.dsl.Tasks
import benchmarks.evaluation.results.MetricsContainer
import benchmarks.evaluation.results.ScenarioResult
import benchmarks.evaluation.results.StepResult
import benchmarks.evaluation.validation.checkBenchmarks
import benchmarks.utils.Either

abstract class AbstractBenchmarkEvaluator {
    private val changesApplier = ChangesApplier()
    protected val progress = CompositeBenchmarksProgressListener()

    fun addListener(progressListener: BenchmarksProgressListener) {
        progress.add(progressListener)
    }

    open fun runBenchmarks(benchmarks: Suite) {
        checkBenchmarks(benchmarks)

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

