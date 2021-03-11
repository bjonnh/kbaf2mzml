

// We don't use that, impossible to build properly using gradle
// Use the build_adapter.sh script on a linux machine
//include(":baf2sql_adapter")
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        jcenter()
        maven("https://plugins.gradle.org/m2/")
    }

    plugins {
        val changelogVersion: String by settings
        val versionsPluginVersion: String by settings
        val kotlinVersion: String by settings
        val jlinkVersion: String by settings
        val detektVersion: String by settings
        val ktlintVersion: String by settings
        val kotlinterVersion: String by settings
        val openjfxPluginVersion: String by settings

        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.changelog") version changelogVersion
        id("com.github.ben-manes.versions") version versionsPluginVersion
        id("org.beryx.jlink") version jlinkVersion

        id("io.gitlab.arturbosch.detekt") version detektVersion
        id("org.jmailen.kotlinter") version kotlinterVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintVersion

        id("org.openjfx.javafxplugin") version openjfxPluginVersion
    }
}

val projectName: String by settings
rootProject.name = projectName