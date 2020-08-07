package org.jetbrains.kotlin.build.benchmarks.evaluation.results

import org.jetbrains.kotlin.build.benchmarks.dsl.Scenario
import org.jetbrains.kotlin.build.benchmarks.evaluation.AbstractBenchmarksProgressListener
import org.jetbrains.kotlin.build.benchmarks.utils.Either
import org.jetbrains.kotlin.build.benchmarks.utils.mapSuccess

class TeamCityMetricReporter : AbstractBenchmarksProgressListener() {
    override fun scenarioFinished(scenario: Scenario, result: Either<ScenarioResult>) {
        result.mapSuccess { scenarioResult ->
            for ((stepIndex, stepResult) in scenarioResult.stepResults.withIndex()) {
                if (!stepResult.step.isMeasured) continue
                var prefix = "";
                stepResult.buildResult.timeMetrics.walkTimeMetrics(
                        fn = { metric, time ->
                            val statisticKey = specialCharactersToUnderscore("${scenario.name}.${stepIndex + 1}.$prefix$metric")
                            reportStatistics(statisticKey, time.asNs.toString())
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