/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package benchmarks.evaluation.gradle

import benchmarks.utils.TimeInterval
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.ProgressEvent
import org.gradle.tooling.events.ProgressListener
import org.gradle.tooling.events.configuration.ProjectConfigurationFinishEvent
import org.gradle.tooling.events.task.TaskFinishEvent
import java.util.LinkedHashMap

internal class BuildRecordingProgressListener : ProgressListener {
    private lateinit var firstEvent: ProgressEvent
    private lateinit var lastEvent: ProgressEvent
    private lateinit var lastProjectConfigurationFinishEvent: ProjectConfigurationFinishEvent
    private var mySnapshotBeforeTaskTimeMs = 0L
    private var mySnapshotAfterTaskTimeMs = 0L
    private var myJavaInstrumentationTimeMs = 0L

    val snapshotBeforeTaskTime: TimeInterval
        get() = TimeInterval.ms(mySnapshotBeforeTaskTimeMs)

    val snapshotAfterTaskTime: TimeInterval
        get() = TimeInterval.ms(mySnapshotAfterTaskTimeMs)

    val configTime: TimeInterval
        get() = TimeInterval.ms(lastProjectConfigurationFinishEvent.eventTime - firstEvent.eventTime)

    val taskExecutionTime: TimeInterval
        get() = TimeInterval.ms(lastEvent.eventTime - lastProjectConfigurationFinishEvent.eventTime)

    val allBuildTime: TimeInterval
        get() = TimeInterval.ms(lastEvent.eventTime - firstEvent.eventTime)

    val javaInstrumentationTimeMs: TimeInterval
        get() = TimeInterval.ms(myJavaInstrumentationTimeMs)

    val taskTimes = LinkedHashMap<String, TimeInterval>()

    private var isFirst = true

    override fun statusChanged(event: ProgressEvent) {
        if (isFirst) {
            isFirst = false
            firstEvent = event
        }
        lastEvent = event

        if (event is ProjectConfigurationFinishEvent) {
            lastProjectConfigurationFinishEvent = event
        }
        if (event is FinishEvent) {
            if (event is TaskFinishEvent) {
                taskTimes[event.descriptor.taskPath] = TimeInterval.ms(event.totalTimeMs)
            }

            when {
                event.displayName.startsWith("Snapshot inputs and outputs before executing task ") -> {
                    mySnapshotBeforeTaskTimeMs += event.totalTimeMs
                }
                event.displayName.startsWith("Snapshot outputs after executing task ") -> {
                    mySnapshotAfterTaskTimeMs += event.totalTimeMs
                }
                event.displayName.startsWith("Execute instrument java classes ") -> {
                    myJavaInstrumentationTimeMs += event.totalTimeMs
                }
            }
        }
    }

    private val FinishEvent.totalTimeMs: Long
        get() = result.endTime - result.startTime
}
