import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
    application
    id("gg.jte.gradle") version "3.1.11"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "github.buriedincode"
version = "0.3.1"

println("Kotlin v${KotlinVersion.CURRENT}")
println("Java v${System.getProperty("java.version")}")
println("Arch: ${System.getProperty("os.arch")}")

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://maven.reposilite.com/snapshots")
}

dependencies {
    implementation("com.sksamuel.hoplite", "hoplite-core", "2.7.5")
    implementation("org.jetbrains.kotlinx", "kotlinx-datetime", "0.6.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.6.3")
    runtimeOnly("org.postgresql", "postgresql", "42.7.3")
    runtimeOnly("org.xerial", "sqlite-jdbc", "3.45.3.0")

    // Exposed
    val exposedVersion = "0.50.1"
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)

    // Jackson
    val jacksonVersion = "2.17.1"
    implementation("com.fasterxml.jackson.core", "jackson-databind", jacksonVersion)
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", jacksonVersion)
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", jacksonVersion)

    // Javalin
    val javalinVersion = "6.1.4"
    implementation("io.javalin", "javalin", javalinVersion)
    implementation("io.javalin", "javalin-rendering", javalinVersion)

    // Jte
    val jteVersion = "3.1.11"
    implementation("gg.jte", "jte", jteVersion)
    implementation("gg.jte", "jte-kotlin", jteVersion)

    // Log4j2
    implementation("org.apache.logging.log4j", "log4j-api-kotlin", "1.4.0")
    runtimeOnly("org.apache.logging.log4j", "log4j-slf4j2-impl", "2.23.1")
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("github.buriedincode.bookshelf.AppKt")
    applicationName = "Bookshelf"
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.2.1")
}

jte {
    precompile()
    kotlinCompileArgs.set(arrayOf("-jvm-target", "21"))
}

tasks.jar {
    dependsOn(tasks.precompileJte)
    from(
        fileTree("jte-classes") {
            include("**/*.class")
            include("**/*.bin") // Only required if you use binary templates
        },
    )
    manifest.attributes["Main-Class"] = "github.buriedincode.bookshelf.AppKt"
}

tasks.shadowJar {
    dependsOn(tasks.precompileJte)
    from(
        fileTree("jte-classes") {
            include("**/*.class")
            include("**/*.bin") // Only required if you use binary templates
        },
    )
    manifest.attributes["Main-Class"] = "github.buriedincode.bookshelf.AppKt"
    mergeServiceFiles()
    archiveClassifier.set("fatJar")
    archiveVersion.set("")
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
