plugins {
    `java-library`
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(libs.bundles.kotlinx.serialization)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.kotlin.reflect)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
