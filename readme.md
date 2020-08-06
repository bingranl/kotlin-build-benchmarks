This project contains benchmarks of the Kotlin project build.

Overview
=====

* A benchmark suite consists of multiple benchmark scenarios.
* A benchmark scenario consists of multiple steps. A scenario might be run multiple times (optionally, one time by default).
* A benchmark step modifies some files and runs some tasks.
* A file change replaces contents of a target file with contents of modified file (e.g. see 
[addPrivateClass.benchmark](src/main/resources/change-files/coreUtil/StringsKt/addPrivateClass.benchmark))
* Benchmarks are described via simple Kotlin DSL (see [scenarios.kt](src/main/kotlin/org/jetbrains/kotlin/build/benchmarks/scenarios/scenarios.kt)) 
and are run via Gradle Tooling API. Tooling API was used because Gradle Profiler did not allow to define multi-step scenarios at the time.
Also working with file copies containing hand-written changes is more flexible than using predefined types of changes of Gradle Profiler
(e.g. "add a particular method with a particular visibility to a particular class" instead of "apply Kotlin ABI change to the file";
Kotlin IC is more granular than simply ABI/non-ABI changes).
