/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package benchmarks.evaluation

import benchmarks.dsl.Scenario
import benchmarks.dsl.Step
import benchmarks.dsl.Tasks
import benchmarks.evaluation.results.ScenarioResult
import benchmarks.evaluation.results.StepResult
import benchmarks.utils.Either

interface BenchmarksProgressListener {
    fun scenarioStarted(scenario: Scenario)
    fun scenarioFinished(scenario: Scenario, result: Either<ScenarioResult>)
    fun stepStarted(step: Step)
    fun stepFinished(step: Step, result: Either<StepResult>)
    fun taskExecutionStarted(tasks: Array<Tasks>)
    fun cleanupStarted()
    fun cleanupFinished()
    fun allFinished()
}