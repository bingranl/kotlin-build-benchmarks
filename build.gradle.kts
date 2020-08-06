group = "org.jetbrains"
version = "1.0-SNAPSHOT"

plugins {
    java
    application
}

buildscript {
    val kotlinVersion = System.getenv("BOOTSTRAP_VERSION") ?: "1.4.255-SNAPSHOT"
    extra["kotlinVersion"] = kotlinVersion

    repositories {
        mavenLocal()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

apply {
    plugin("kotlin")
}

repositories {
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    mavenLocal()
}

val toolingApiVersion = "6.2.2"
val kotlinVersion: String by extra

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("gradle-build-metrics", kotlinVersion))
    implementation("org.gradle:gradle-tooling-api:$toolingApiVersion")
    // The tooling API need an SLF4J implementation available at runtime, replace this with any other implementation
    runtimeOnly("org.slf4j:slf4j-simple:1.7.10")
}

application {
    mainClassName = "RunAllBenchmarksKt"
}

tasks.getByName("run", JavaExec::class) {
    workingDir = File("../../")
}

tasks.register("runFast", JavaExec::class) {
    classpath = sourceSets.main.get().runtimeClasspath
    workingDir = File("../../")
    main = "RunFastBenchmarksKt"
}