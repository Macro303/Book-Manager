plugins {
    kotlin("jvm") version "1.9.0"
    id("gg.jte.gradle") version "3.0.1"
    kotlin("kapt") version "1.9.0"
    application
    id("com.github.ben-manes.versions") version "0.47.0"
}

group = "github.buriedincode"
version = "0.1.0"

println("Bookshelf v${version}")
println("Kotlin v${KotlinVersion.CURRENT}")
println("Java v${System.getProperty("java.version")}")
println("Arch: ${System.getProperty("os.arch")}")

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    runtimeOnly("org.xerial:sqlite-jdbc:3.42.0.0")

    // Konf
    val konfVersion = "1.1.2"
    implementation("com.uchuhimo:konf-core:$konfVersion")
    implementation("com.uchuhimo:konf-yaml:$konfVersion")
    
    // Javalin
    val javalinVersion = "5.6.1"
    implementation("io.javalin:javalin:$javalinVersion")
    implementation("io.javalin:javalin-rendering:5.6.0")
    implementation("io.javalin.community.openapi:javalin-openapi-plugin:$javalinVersion")
    implementation("io.javalin.community.openapi:javalin-swagger-plugin:$javalinVersion")
    implementation("io.javalin.community.openapi:javalin-redoc-plugin:$javalinVersion")
    kapt("io.javalin.community.openapi:openapi-annotation-processor:$javalinVersion")

    // Jte
    val jteVersion = "3.0.1"
    implementation("gg.jte:jte:$jteVersion")
    implementation("gg.jte:jte-kotlin:$jteVersion")

    // Exposed
    val exposedVersion = "0.41.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // Jackson
    val jacksonVersion = "2.15.2"
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Log4j2
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.2.0")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
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
