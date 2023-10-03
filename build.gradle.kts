plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("org.jlleitschuh.gradle.ktlint") version "11.6.0"
    id("com.github.ben-manes.versions") version "0.48.0"
    id("gg.jte.gradle") version "3.1.1"
}

group = "github.buriedincode"
version = "0.1.0"

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
    runtimeOnly("org.xerial", "sqlite-jdbc", "3.43.0.0")

    // Exposed
    val exposedVersion = "0.44.0"
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)

    // Hoplite
    val hopliteVersion = "2.7.4"
    implementation("com.sksamuel.hoplite", "hoplite-core", hopliteVersion)
    implementation("com.sksamuel.hoplite", "hoplite-hocon", hopliteVersion)
    implementation("com.sksamuel.hoplite", "hoplite-json", hopliteVersion)
    implementation("com.sksamuel.hoplite", "hoplite-toml", hopliteVersion)
    implementation("com.sksamuel.hoplite", "hoplite-yaml", hopliteVersion)

    // Jackson
    val jacksonVersion = "2.15.2"
    implementation("com.fasterxml.jackson.core", "jackson-databind", jacksonVersion)
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", jacksonVersion)
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", jacksonVersion)

    // Javalin
    val javalinVersion = "5.6.2"
    implementation("io.javalin", "javalin", javalinVersion)
    implementation("io.javalin", "javalin-rendering", javalinVersion)

    // Jte
    val jteVersion = "3.1.1"
    implementation("gg.jte", "jte", jteVersion)
    implementation("gg.jte", "jte-kotlin", jteVersion)

    // Log4j2
    implementation("org.apache.logging.log4j", "log4j-api-kotlin", "1.2.0")
    runtimeOnly("org.apache.logging.log4j", "log4j-slf4j2-impl", "2.20.0")
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
    version.set("1.0.0")
}

jte {
    precompile()
}

tasks.jar {
    dependsOn(tasks.precompileJte)
    from(
        fileTree("jte-classes") {
            include("**/*.class")
            include("**/*.bin") // Only required if you use binary templates
        },
    )
}
