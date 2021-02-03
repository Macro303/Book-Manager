import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
	id("application")
	kotlin("jvm") version "1.4.30"
	id("com.github.ben-manes.versions") version "0.36.0"
}

repositories {
	mavenCentral()
	mavenLocal()
	jcenter()
}

dependencies {
	implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8")
	implementation(group = "com.konghq", name = "unirest-java", version = "3.11.05")
	implementation(group = "org.json", name = "json", version = "20200518")
	implementation(group = "org.yaml", name = "snakeyaml", version = "1.27")
	runtimeOnly(group = "org.xerial", name = "sqlite-jdbc", version = "3.32.3.2")

	//Ktor
	val ktorVersion = "1.4.2"
	implementation(group = "io.ktor", name = "ktor-server-netty", version = ktorVersion)
	implementation(group = "io.ktor", name = "ktor-gson", version = ktorVersion)
	implementation(group = "io.ktor", name = "ktor-freemarker", version = ktorVersion)

	//Log4j
	val logVersion = "2.14.0"
	implementation(group = "org.apache.logging.log4j", name = "log4j-api", version = logVersion)
	runtimeOnly(group = "org.apache.logging.log4j", name = "log4j-core", version = logVersion)
	runtimeOnly(group = "org.apache.logging.log4j", name = "log4j-slf4j-impl", version = logVersion)
	runtimeOnly(group = "org.apache.logging.log4j", name = "log4j-jcl", version = logVersion)
}

application {
	mainClass.set("macro.library.LibraryKt")
	applicationName = "Book-Manager"
}

tasks.jar {
	manifest {
		attributes(
			"Main-Class" to application.mainClass,
			"Multi-Release" to true
		)
	}
}

tasks.compileKotlin {
	sourceCompatibility = "11"
	targetCompatibility = "11"

	kotlinOptions.jvmTarget = "11"
	kotlinOptions.apiVersion = "1.4"
	kotlinOptions.languageVersion = "1.4"
}

fun isNonStable(version: String): Boolean {
	val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
	val regex = "^[0-9,.v-]+(-r)?$".toRegex()
	val isStable = stableKeyword || regex.matches(version)
	return isStable.not()
}

tasks.named("dependencyUpdates", DependencyUpdatesTask::class.java).configure {
	// Example 1: reject all non stable versions
	rejectVersionIf {
		isNonStable(candidate.version)
	}

	// Example 2: disallow release candidates as upgradable versions from stable versions
	rejectVersionIf {
		isNonStable(candidate.version) && !isNonStable(currentVersion)
	}

	// Example 3: using the full syntax
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