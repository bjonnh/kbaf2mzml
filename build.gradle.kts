plugins {
    kotlin("jvm") version "1.4.30-M1"
    application
    distribution
    id("org.beryx.runtime") version "1.12.1"
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
    implementation("com.github.ajalt.clikt:clikt:3.1.0") {
        exclude("org.jetbrains.kotlin","kotlin-stdlib-jdk8")
    }
}

distributions {
    main {
        contents {
            from("lib/") {
                into("lib")
                include("*.so")
                include("*.dll")
            }
        }
    }
}

runtime {
    imageZip.set(file("$buildDir/kbaf2mzml-$version.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    modules.set(listOf("java.sql"))
    targetPlatform("windows-x64", "stuff/jdk-15.0.1+9")
    targetPlatform("linux-x64", "/usr/lib/jvm/default")
}

tasks {
    "runtime" {
        doLast {
            copy{
                from("lib/*.dll")
                into("$buildDir/image/lib")
            }
            copy{
                from("lib/*.so")
                into("$buildDir/image/lib")
            }
        }
    }
}
