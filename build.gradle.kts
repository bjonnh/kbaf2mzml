plugins {
    kotlin("jvm") version "1.4.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.xerial:sqlite-jdbc:3.32.3.2")

    implementation("io.github.msdk:msdk-io-mzml:0.0.27")

    implementation("io.github.microutils:kotlin-logging-jvm:2.0.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.9.1")
}