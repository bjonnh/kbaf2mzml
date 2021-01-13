plugins {
    kotlin("jvm") version "1.4.10"
    application
    distribution
}

group = "net.nprod"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

application {
    mainClass.set("net.nprod.baf2mzml.MainKt")
}


dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.xerial:sqlite-jdbc:3.32.3.2")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("com.github.ajalt.clikt:clikt:3.1.0")
}

distributions {
    main {
        contents {
            from("../lib/") {
                into("lib")
                include("*.so")
            }
        }
    }
}