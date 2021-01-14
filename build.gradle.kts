plugins {
    kotlin("jvm") version "1.4.21"
    application
    id("org.beryx.jlink") version "2.23.1"
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
val compileJava: JavaCompile by tasks
compileJava.destinationDir = compileKotlin.destinationDir


repositories {
    mavenCentral()
    jcenter()
}

application {
    mainModule.set("baf2mzml")
    mainClass.set("net.nprod.baf2mzml.JavaFX")
}

java {
    modularity.inferModulePath.set(true)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.xerial:sqlite-jdbc:3.32.3.2")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("com.github.ajalt.clikt:clikt:3.1.0") {
        exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
    }

    val platform = "linux"
    implementation("org.openjfx:javafx-base:15.0.1:${platform}")
    implementation("org.openjfx:javafx-controls:15.0.1:${platform}")
    implementation("org.openjfx:javafx-graphics:15.0.1:${platform}")
    implementation("org.openjfx:javafx-web:15.0.1:${platform}")
    implementation("org.openjfx:javafx-media:15.0.1:${platform}")
    implementation("org.openjfx:javafx-fxml:15.0.1:${platform}")
}
/*
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
*/
/*javafx {
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web")
}*/

jlink {
    launcher {
        name = "hello"
    }
    /*addExtraDependencies("com.github.ajalt.clikt")
    imageZip.set(file("$buildDir/kbaf2mzml-$version.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    //modules.set(listOf("java.sql"))
    targetPlatform("windows-x64", "stuff/jdk-15.0.1+9")
    targetPlatform("linux-x64", "/usr/lib/jvm/default")*/
}

tasks {
    "jlink" {
        doLast {
            copy {
                from("lib/*.dll")
                into("$buildDir/image/lib")
            }
            copy {
                from("lib/*.so")
                into("$buildDir/image/lib")
            }
        }
    }
}
