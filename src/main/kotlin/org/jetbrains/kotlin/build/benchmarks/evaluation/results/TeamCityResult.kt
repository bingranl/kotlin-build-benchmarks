package org.jetbrains.kotlin.build.benchmarks.evaluation.results

import org.jetbrains.kotlin.build.benchmarks.dsl.Scenario
import org.jetbrains.kotlin.build.benchmarks.dsl.Step
import org.jetbrains.kotlin.build.benchmarks.evaluation.AbstractBenchmarksProgressListener
import org.jetbrains.kotlin.build.benchmarks.utils.Either

class TeamCityMetricReporter : AbstractBenchmarksProgressListener() {
    private lateinit var currentScenario: Scenario
    private var currentScenarioIteration: Int = 0

    override fun scenarioStarted(scenario: Scenario) {
        if (::currentScenario.isInitialized && currentScenario == scenario) {
            currentScenarioIteration++
        } else {
            currentScenarioIteration = 0
        }
        currentScenario = scenario
        startTest("${scenario.name} (iteration $currentScenarioIteration)")
    }

    override fun stepFinished(step: Step, result: Either<StepResult>) {
        when (result) {
            is Either.Success -> {
                reportMessage("Step finished")
            }
            is Either.Failure -> {
                reportMessage("Step finished with error: ${result.reason}", MessageStatus.FAILURE)
            }
        }
    }

    override fun scenarioFinished(scenario: Scenario, result: Either<ScenarioResult>) {
        when (result) {
            is Either.Success -> {
                for ((stepIndex, stepResult) in result.value.stepResults.withIndex()) {
                    if (!stepResult.step.isMeasured) continue
                    var prefix = "";
                    stepResult.buildResult.timeMetrics.walkTimeMetrics(
                        fn = { metric, time ->
                            val statisticKey = specialCharactersToUnderscore("${scenario.name}.iter-$currentScenarioIteration.step-${stepIndex + 1}.$prefix$metric")
                            reportStatistics(statisticKey, time.asMs.toString())
                        },
                        onEnter = {
                            prefix += "$it."
                        },
                        onExit = {
                            prefix = prefix.substring(0, prefix.length - (it.length + 1))
                        }
                    )
                }
            }
            is Either.Failure -> {
                failTest("${scenario.name} (iteration $currentScenarioIteration)", result.reason)
            }
        }
        finishTest("${scenario.name} (iteration $currentScenarioIteration)")
    }
}

val nonAlphabeticalCharactersAndDot = Regex("[^\\w.]")
fun specialCharactersToUnderscore(key: String): String {
    return key.replace(nonAlphabeticalCharactersAndDot, "_")
}

fun escapeTcCharacters(message: String) = message
        .replace("|", "||")
        .replace("\n", "|n")
        .replace("\r", "|r")
        .replace("'", "|'")
        .replace("[", "|[")
        .replace("]", "|]")

fun reportStatistics(key: String, value: String) {
    println("##teamcity[buildStatisticValue key='${escapeTcCharacters(key)}' value='${escapeTcCharacters(value)}']")
}

enum class MessageStatus {
    NORMAL, WARNING, FAILURE, ERROR
}

fun reportMessage(msg: String, status: MessageStatus = MessageStatus.NORMAL) {
    println("##teamcity[message text='${escapeTcCharacters(msg)}' status='$status']")
}

fun startTest(name: String) {
    println("##teamcity[testStarted name='${escapeTcCharacters(name)}']")
}

fun failTest(name: String, msg: String) {
    println("##teamcity[testFailed name='${escapeTcCharacters(name)}' message='${escapeTcCharacters(msg)}']")
}

fun finishTest(name: String) {
    println("##teamcity[testFinished name='${escapeTcCharacters(name)}']")
}