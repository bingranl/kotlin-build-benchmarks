/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.benchmarks.dsl

fun suite(name: String, fn: SuiteBuilder.() -> Unit): Suite =
    SuiteBuilderImpl(name).apply(fn).build()

interface SuiteBuilder {
    fun scenario(name: String, fn: ScenarioBuilder.() -> Unit)
    fun defaultTasks(vararg tasks: Tasks)
    fun changeableFile(name: String): ChangeableFile
}

interface ScenarioBuilder {
    fun step(fn: StepWithFileChangesBuilder.() -> Unit)
    fun revertLastStep(fn: StepBuilder.() -> Unit)
    fun expectSlowBuild(reason: String)
    var repeat: UByte
}

interface StepBuilder {
    var isMeasured: Boolean
    fun doNotMeasure() {
        isMeasured = false
    }

    var isExpectedToFail: Boolean
    fun expectBuildToFail() {
        isExpectedToFail = true
    }

    fun runTasks(vararg tasksToRun: Tasks)
}

interface StepWithFileChangesBuilder : StepBuilder {
    fun changeFile(changeableFile: ChangeableFile, typeOfChange: TypeOfChange)
}

class SuiteBuilderImpl(private val projectName: String) : SuiteBuilder {
    private var defaultTasks = arrayOf<Tasks>()
    private val scenarios = arrayListOf<Scenario>()
    private val changeableFiles = arrayListOf<ChangeableFile>()

    override fun scenario(name: String, fn: ScenarioBuilder.() -> Unit) {
        scenarios.add(ScenarioBuilderImpl(name = name).apply(fn).build())
    }

    override fun defaultTasks(vararg tasks: Tasks) {
        defaultTasks = arrayOf(*tasks)
    }

    override fun changeableFile(name: String): ChangeableFile {
        val changeableFile = ChangeableFile(projectName, name)
        changeableFiles.add(changeableFile)
        return changeableFile
    }

    fun build() =
        Suite(scenarios = scenarios.toTypedArray(), defaultTasks = defaultTasks, changeableFiles = changeableFiles.toTypedArray())
}

class ScenarioBuilderImpl(private val name: String) : ScenarioBuilder {
    override var repeat: UByte = 1U

    private var expectedSlowBuildReason: String? = null
    override fun expectSlowBuild(reason: String) {
        expectedSlowBuildReason = reason
    }

    private val steps = arrayListOf<Step>()

    override fun step(fn: StepWithFileChangesBuilder.() -> Unit) {
        steps.add(SimpleStepBuilder().apply(fn).build())
    }

    override fun revertLastStep(fn: StepBuilder.() -> Unit) {
        steps.add(RevertStepBuilder().apply(fn).build())
    }

    fun build() =
        Scenario(
            name = name,
            steps = steps.toTypedArray(),
            expectedSlowBuildReason = expectedSlowBuildReason,
            repeat = repeat
        )
}

abstract class AbstractStepBuilder : StepBuilder {
    override var isMeasured = true
    override var isExpectedToFail = false
    protected var tasks: Array<Tasks>? = null

    override fun runTasks(vararg tasksToRun: Tasks) {
        this.tasks = arrayOf(*tasksToRun)
    }
}

class SimpleStepBuilder : AbstractStepBuilder(), StepWithFileChangesBuilder {
    private val fileChanges = arrayListOf<FileChange>()

    override fun changeFile(changeableFile: ChangeableFile, typeOfChange: TypeOfChange) {
        fileChanges.add(FileChange(changeableFile, typeOfChange))
    }

    fun build() =
        Step.SimpleStep(
            isMeasured = this.isMeasured,
            isExpectedToFail = this.isExpectedToFail,
            tasks = this.tasks,
            fileChanges = this.fileChanges.toTypedArray()
        )
}

class RevertStepBuilder : AbstractStepBuilder() {
    fun build() =
        Step.RevertLastStep(
            isMeasured = this.isMeasured,
            isExpectedToFail = this.isExpectedToFail,
            tasks = this.tasks
        )
}