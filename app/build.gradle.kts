plugins {
    application
    alias(libs.plugins.jte)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":openlibrary"))
    implementation(libs.bundles.exposed)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.javalin)
    implementation(libs.bundles.jte)
    implementation(libs.hoplite.core)
    runtimeOnly(libs.postgres)
}

application {
    mainClass = "github.buriedincode.bookshelf.AppKt"
    applicationName = "Bookshelf"
}

jte {
    precompile()
    kotlinCompileArgs = arrayOf("-jvm-target", "17")
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
}
