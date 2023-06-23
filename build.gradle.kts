plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("kapt") version "1.8.21"
    application
    id("com.github.ben-manes.versions") version "0.47.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
    id("gg.jte.gradle") version "2.3.2"
}

group = "github.buriedincode"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://maven.reposilite.com/snapshots")
}

dependencies {
    runtimeOnly("org.xerial:sqlite-jdbc:3.42.0.0")

    // Konf
    val konf_version = "1.1.2"
    implementation("com.uchuhimo:konf-core:$konf_version")
    implementation("com.uchuhimo:konf-yaml:$konf_version")
    
    // Javalin
    val javalin_version = "5.6.1"
    implementation("io.javalin:javalin:$javalin_version")
    implementation("io.javalin:javalin-rendering:$javalin_version")
    implementation("io.javalin.community.openapi:javalin-openapi-plugin:$javalin_version")
    implementation("io.javalin.community.openapi:javalin-swagger-plugin:$javalin_version")
    implementation("io.javalin.community.openapi:javalin-redoc-plugin:$javalin_version")
    kapt("io.javalin.community.openapi:openapi-annotation-processor:$javalin_version")

    // Jte
    val jte_version = "3.0.0"
    implementation("gg.jte:jte:$jte_version")
    implementation("gg.jte:jte-kotlin:$jte_version")

    // Exposed
    val exposed_version = "0.41.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")

    // Jackson
    val jackson_version = "2.15.2"
    implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_version")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")

    // Log4j2
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.2.0")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("github.buriedincode.bookshelf.AppKt")
}

jte {
    precompile()
}

tasks.jar {
    dependsOn(tasks.precompileJte)
    from(fileTree("jte-classes") {
        include("**/*.class")
        include("**/*.bin") // Only required if you use binary templates
    })
}
