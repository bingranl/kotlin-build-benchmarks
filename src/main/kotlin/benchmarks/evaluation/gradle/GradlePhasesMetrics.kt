/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package benchmarks.evaluation.gradle

import benchmarks.evaluation.results.MutableMetricsContainer
import benchmarks.evaluation.results.ValueMetric
import benchmarks.utils.TimeInterval

enum class GradlePhasesMetrics(val parent: GradlePhasesMetrics? = null) {
    GRADLE_BUILD,
    CONFIGURATION(GRADLE_BUILD),
    EXECUTION(GRADLE_BUILD),
    COMPILATION_TASKS(EXECUTION),
    NON_COMPILATION_TASKS(EXECUTION),
    UP_TO_DATE_CHECKS(EXECUTION),
    UP_TO_DATE_CHECKS_BEFORE_TASK(UP_TO_DATE_CHECKS),
    UP_TO_DATE_CHECKS_AFTER_TASK(UP_TO_DATE_CHECKS)
}

operator fun MutableMetricsContainer<TimeInterval>.set(metric: GradlePhasesMetrics, time: TimeInterval) {
    set(metric.name, ValueMetric(time), metric.parent?.name)
}