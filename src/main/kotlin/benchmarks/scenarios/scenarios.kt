package benchmarks.scenarios

import benchmarks.dsl.ChangeableFile
import benchmarks.dsl.Tasks
import benchmarks.dsl.TypeOfChange
import benchmarks.dsl.suite

fun fastBenchmarks(vararg defaultTasksToRun: Tasks) =
    allBenchmarks(*defaultTasksToRun).let { suite ->
        suite.copy(scenarios = suite.scenarios.filter { scenario -> scenario.expectedSlowBuildReason == null }.toTypedArray())
    }

fun allBenchmarks(vararg defaultTasksToRun: Tasks) =
    suite {
        defaultTasks(*defaultTasksToRun)

        scenario("clean build") {
            expectSlowBuild("clean build")
            step {
                doNotMeasure()
                runTasks(Tasks.CLEAN)
            }
            step {}
        }

        scenario("add private function") {
            step {
                changeFile(ChangeableFile.CORE_UTIL_STRINGS, TypeOfChange.ADD_PRIVATE_FUNCTION)
            }
        }

        scenario("add public function") {
            step {
                changeFile(ChangeableFile.CORE_UTIL_STRINGS, TypeOfChange.ADD_PUBLIC_FUNCTION)
            }
        }

        scenario("add private class") {
            step {
                changeFile(ChangeableFile.CORE_UTIL_STRINGS, TypeOfChange.ADD_PRIVATE_CLASS)
            }
        }

        scenario("add public class") {
            step {
                changeFile(ChangeableFile.CORE_UTIL_STRINGS, TypeOfChange.ADD_PUBLIC_CLASS)
            }
        }

        scenario("build after error") {
            step {
                doNotMeasure()
                expectBuildToFail()
                changeFile(ChangeableFile.CORE_UTIL_STRINGS, TypeOfChange.INTRODUCE_COMPILE_ERROR)
            }
            step {
                changeFile(ChangeableFile.CORE_UTIL_STRINGS, TypeOfChange.FIX_COMPILE_ERROR)
            }
        }

        scenario("change popular inline function") {
            step {
                changeFile(ChangeableFile.CORE_UTIL_CORE_LIB, TypeOfChange.CHANGE_INLINE_FUNCTION)
            }
        }
    }
