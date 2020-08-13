@file:JvmName("ScenariosKt")

package org.jetbrains.kotlin.build.benchmarks.scenarios

import org.jetbrains.kotlin.build.benchmarks.dsl.Tasks
import org.jetbrains.kotlin.build.benchmarks.dsl.TypeOfChange
import org.jetbrains.kotlin.build.benchmarks.dsl.suite

fun fastBenchmarks(projectName: String, vararg defaultTasksToRun: Tasks) =
    allBenchmarks(projectName, *defaultTasksToRun).let { suite ->
        suite.copy(scenarios = suite.scenarios.filter { scenario -> scenario.expectedSlowBuildReason == null }.toTypedArray())
    }

fun allBenchmarks(projectName: String, vararg defaultTasksToRun: Tasks) =
    mapOf(
        "kotlin" to { kotlinBenchmarks(*defaultTasksToRun) },
        "gavra0" to { gavra0Benchmarks(*defaultTasksToRun) },
        "space" to { spaceBenchmarks(*defaultTasksToRun) }
    )[projectName]?.let { it() } ?: throw IllegalStateException("Test suit for $projectName is not defined!")


fun kotlinBenchmarks(vararg defaultTasksToRun: Tasks) =
    suite("kotlin") {
        val coreUtilStrings = changeableFile("coreUtil/StringsKt")
        val coreUtilCoreLib = changeableFile("coreUtil/CoreLibKt")

        defaultTasks(*defaultTasksToRun)
        defaultJdk = System.getProperty("JDK_8")

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
                changeFile(coreUtilStrings, TypeOfChange.ADD_PRIVATE_FUNCTION)
            }
        }

        scenario("add public function") {
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PUBLIC_FUNCTION)
            }
        }

        scenario("add private class") {
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PRIVATE_CLASS)
            }
        }

        scenario("add public class") {
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PUBLIC_CLASS)
            }
        }

        scenario("build after error") {
            step {
                doNotMeasure()
                expectBuildToFail()
                changeFile(coreUtilStrings, TypeOfChange.INTRODUCE_COMPILE_ERROR)
            }
            step {
                changeFile(coreUtilStrings, TypeOfChange.FIX_COMPILE_ERROR)
            }
        }

        scenario("change popular inline function") {
            step {
                changeFile(coreUtilCoreLib, TypeOfChange.CHANGE_INLINE_FUNCTION)
            }
        }
    }

fun spaceBenchmarks(vararg defaultTasksToRun: Tasks) =
        suite("space") {
            defaultTasks(*defaultTasksToRun)
        }

fun gavra0Benchmarks(vararg defaultTasksToRun: Tasks) =
    suite("gavra0") {
        defaultTasks(*defaultTasksToRun)
        defaultArguments("-Dorg.gradle.workers.max=8", "--parallel", "--watch-fs")
        defaultJdk = System.getProperty("JDK_8")

        val stdlibFileTreeWalk = changeableFile("stdlib/FileTreeWalkKt")
        val coreDescriptorsClassDescriptorsBase = changeableFile("coreDescriptors/ClassDescriptorBaseJava")

        scenario("build") {
            step {
                doNotMeasure()
                runTasks(Tasks.KOTLIN_GRADLE_PLUGIN_COMPILE_JAVA)
            }
        }

        scenario("abi change to stdlib") {
            step {
                changeFile(stdlibFileTreeWalk, TypeOfChange.ADD_PUBLIC_FUNCTION)
                runTasks(Tasks.KOTLIN_GRADLE_PLUGIN_COMPILE_JAVA)
            }
            repeat = 10U
        }

        scenario("abi change to core.descriptors") {
            step {
                changeFile(coreDescriptorsClassDescriptorsBase, TypeOfChange.ADD_PUBLIC_FUNCTION)
                runTasks(Tasks.DIST, Tasks.IDEA_PLUGIN)
            }
            repeat = 10U
        }
    }