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

abstract class AbstractBenchmarksProgressListener : BenchmarksProgressListener {
    override fun scenarioStarted(scenario: Scenario) {}
    override fun scenarioFinished(scenario: Scenario, result: Either<ScenarioResult>) {}
    override fun stepStarted(step: Step) {}
    override fun stepFinished(step: Step, result: Either<StepResult>) {}
    override fun allFinished() {}
    override fun taskExecutionStarted(tasks: Array<Tasks>) {}
    override fun cleanupStarted() {}
    override fun cleanupFinished() {}
}