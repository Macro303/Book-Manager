import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm") version "1.9.21"
    application
    id("gg.jte.gradle") version "3.1.5"
    id("com.github.ben-manes.versions") version "0.50.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

group = "github.buriedincode"
version = "0.2.1"

println("Bookshelf v$version")
println("Kotlin v${KotlinVersion.CURRENT}")
println("Java v${System.getProperty("java.version")}")
println("Arch: ${System.getProperty("os.arch")}")

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://maven.reposilite.com/snapshots")
}

dependencies {
    implementation("org.postgresql", "postgresql", "42.7.0")
    implementation("mysql", "mysql-connector-java", "8.0.33")
    runtimeOnly("org.xerial", "sqlite-jdbc", "3.44.1.0")

    // Exposed
    val exposedVersion = "0.45.0"
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)

    // Hoplite
    val hopliteVersion = "2.7.5"
    implementation("com.sksamuel.hoplite", "hoplite-core", hopliteVersion)
    implementation("com.sksamuel.hoplite", "hoplite-hocon", hopliteVersion)
    implementation("com.sksamuel.hoplite", "hoplite-json", hopliteVersion)
    implementation("com.sksamuel.hoplite", "hoplite-toml", hopliteVersion)
    implementation("com.sksamuel.hoplite", "hoplite-yaml", hopliteVersion)

    // Jackson
    val jacksonVersion = "2.16.0"
    implementation("com.fasterxml.jackson.core", "jackson-databind", jacksonVersion)
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", jacksonVersion)
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", jacksonVersion)

    // Javalin
    val javalinVersion = "5.6.3"
    implementation("io.javalin", "javalin", javalinVersion)
    implementation("io.javalin", "javalin-rendering", "5.6.2")

    // Jte
    val jteVersion = "3.1.5"
    implementation("gg.jte", "jte", jteVersion)
    implementation("gg.jte", "jte-kotlin", jteVersion)

    // Log4j2
    implementation("org.apache.logging.log4j", "log4j-api-kotlin", "1.3.0")
    runtimeOnly("org.apache.logging.log4j", "log4j-slf4j2-impl", "2.22.0")
}

kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("github.buriedincode.bookshelf.AppKt")
    applicationName = "Bookshelf"
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.0.1")
}

jte {
    precompile()
    kotlinCompileArgs.set(arrayOf("-jvm-target", "17"))
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
