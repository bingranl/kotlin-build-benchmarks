group = "org.jetbrains"
version = "1.0-SNAPSHOT"

plugins {
    java
    application
}

buildscript {
    val kotlinVersion = System.getenv("BOOTSTRAP_VERSION") ?: "1.4.20-dev-2975"
    extra["kotlinVersion"] = kotlinVersion
    val bootstrapRepo = "https://buildserver.labs.intellij.net/guestAuth/app/rest/builds/buildType:(id:Kotlin_KotlinDev_CompilerDistAndMavenArtifacts),number:$kotlinVersion,branch:default:any/artifacts/content/maven"
    extra["bootstrapRepo"] = bootstrapRepo

    repositories {
        mavenLocal()
        maven {
            url = uri(bootstrapRepo)
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

apply {
    plugin("kotlin")
}

val toolingApiVersion = "6.2.2"
val kotlinVersion: String by extra
val bootstrapRepo: String by extra

repositories {
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    mavenLocal()
    maven {
        url = uri(bootstrapRepo)
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("gradle-build-metrics", kotlinVersion))
    implementation("org.gradle:gradle-tooling-api:$toolingApiVersion")
    // The tooling API need an SLF4J implementation available at runtime, replace this with any other implementation
    runtimeOnly("org.slf4j:slf4j-simple:1.7.10")
}

application {
    mainClassName = "org.jetbrains.kotlin.build.benchmarks.RunAllBenchmarksKt"
}

tasks.register("runFast", JavaExec::class) {
    classpath = sourceSets.main.get().runtimeClasspath
    main = "org.jetbrains.kotlin.build.benchmarks.RunFastBenchmarksKt"
}