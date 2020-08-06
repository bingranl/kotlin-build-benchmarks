/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package benchmarks.evaluation

import benchmarks.evaluation.results.MetricsContainer
import benchmarks.utils.TimeInterval

class BuildResult(val timeMetrics: MetricsContainer<TimeInterval>)