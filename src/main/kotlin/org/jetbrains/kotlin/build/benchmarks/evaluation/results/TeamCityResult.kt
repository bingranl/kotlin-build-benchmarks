package org.jetbrains.kotlin.build.benchmarks.evaluation.results

import org.jetbrains.kotlin.build.benchmarks.dsl.Scenario
import org.jetbrains.kotlin.build.benchmarks.dsl.Step
import org.jetbrains.kotlin.build.benchmarks.evaluation.AbstractBenchmarksProgressListener
import org.jetbrains.kotlin.build.benchmarks.utils.Either

class TeamCityMetricReporter : AbstractBenchmarksProgressListener() {
    override fun scenarioStarted(scenario: Scenario) {
        startTest(scenario.name)
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

    override fun scenarioFinished(scenario: Scenario, result: Either<List<ScenarioResult>>) {
        when (result) {
            is Either.Success -> {
                for ((scenarioRun, scenarioResult) in result.value.withIndex()) {
                    for ((stepIndex, stepResult) in scenarioResult.stepResults.withIndex()) {
                        if (!stepResult.step.isMeasured) continue
                        var prefix = "";
                        stepResult.buildResult.timeMetrics.walkTimeMetrics(
                            fn = { metric, time ->
                                val fullMetricName = "$prefix$metric"
                                val statisticKey =
                                    specialCharactersToUnderscore("${scenario.name}.iter-${scenarioRun + 1}.step-${stepIndex + 1}.$fullMetricName")
                                if (scenario.trackedMetrics?.contains(fullMetricName) != false) {
                                    setParameter("env.br.$statisticKey", time.asMs.toString())
                                }
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
            }
            is Either.Failure -> {
                failTest(scenario.name, result.reason)
            }
        }
        finishTest(scenario.name)
    }

    override fun cleanupStarted() {
        reportMessage("Cleanup after last scenario is started")
    }

    override fun cleanupFinished() {
        reportMessage("Cleanup after last scenario is finished")
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

fun setParameter(key: String, value: String) {
    println("##teamcity[setParameter key='${escapeTcCharacters(key)}' value='${escapeTcCharacters(value)}']")
}

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