import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.jte) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.versions)
}

println("Kotlin v${KotlinVersion.CURRENT}")
println("Java v${System.getProperty("java.version")}")
println("Arch: ${System.getProperty("os.arch")}")

allprojects {
    group = "github.buriedincode"
    version = "0.3.1"

    repositories {
        mavenCentral()
        mavenLocal()
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version = "1.3.1"
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation(rootProject.libs.kotlin.logging)
        implementation(rootProject.libs.kotlinx.datetime)
        runtimeOnly(rootProject.libs.log4j2.slf4j2.impl)
        runtimeOnly(rootProject.libs.sqlite.jdbc)
    }

    kotlin {
        jvmToolchain(17)
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    gradleReleaseChannel = "current"
    resolutionStrategy {
        componentSelection {
            all {
                if (isNonStable(candidate.version) && !isNonStable(currentVersion)) {
                    reject("Release candidate")
                }
            }
        }
    }
}
