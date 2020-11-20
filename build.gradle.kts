plugins {
    kotlin("jvm") version "1.4.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("http://www.ebi.ac.uk/Tools/maven/repos/content/groups/ebi-snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.xerial:sqlite-jdbc:3.32.3.2")

    //implementation("uk.ac.ebi.jmzml:jmzml:1.7.10-SNAPSHOT")
    implementation("io.github.msdk:msdk-io-mzml:0.0.27")
    implementation("org.apache.commons:commons-text:1.9")

    implementation("io.github.microutils:kotlin-logging-jvm:2.0.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.9.1")
}