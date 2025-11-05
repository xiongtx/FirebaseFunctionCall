// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.4" apply false

    // Add Spotless for code formatting
    id("com.diffplug.spotless") version "6.25.0"
}

spotless {
    kotlin {
        target("app/src/**/*.kt")
        targetExclude("**/build/**", "**/generated/**")
        ktlint("1.0.1").editorConfigOverride(
            mapOf(
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                "ktlint_standard_discouraged-comment-location" to "disabled",
                "ktlint_standard_no-wildcard-imports" to "disabled",
            ),
        )
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint("1.0.1")
    }

    format("xml") {
        target("app/src/**/*.xml")
        targetExclude("**/build/**")
        trimTrailingWhitespace()
        indentWithSpaces(4)
        endWithNewline()
    }
}

// Hide default Spotless tasks from task list
tasks.matching { task ->
    task.name.startsWith("spotless") && task.name != "format"
}.configureEach {
    group = null
}

// Format task - only formats code, no verification
tasks.register("format") {
    group = "formatting"
    description = "Formats all code (Kotlin, Gradle, XML)"
    dependsOn("spotlessApply")
}

// Lint task - runs all linting checks (formatting + Android lint)
tasks.register("lint") {
    group = "verification"
    description = "Runs all linting checks (formatting, Android lint, etc.)"
    dependsOn("spotlessCheck", ":app:lint")
}
