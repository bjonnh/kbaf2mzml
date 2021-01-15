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
        name = "run"
    }
    //addExtraDependencies("com.github.ajalt.clikt")
    imageZip.set(file("$buildDir/kbaf2mzml-$version.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    targetPlatform("windows-x64") {
        setJdkHome("stuff/jdk-15.0.1+9")
        extraModulePaths.add("stuff/javafx-jmods-15.0.1")
    }
    targetPlatform("linux-x64", "/usr/lib/jvm/default")
    mergedModule {
        forceMerge("kotlin", "sqlite-jdbc")
        requires("java.sql")
        provides("java.sql.Driver").with("org.sqlite.JDBC")
        uses("org.sqlite.JDBC")
    }
}

tasks {
    "jlink" {
        if (this is org.beryx.jlink.JlinkTask) {
            doLast {
                file("$buildDir/image/run-windows-x64/lib").mkdirs()
                file("$buildDir/image/run-linux-x64/lib").mkdirs()
                copy {
                    from("$projectDir/lib/baf2sql_adapter.dll", "$projectDir/lib/baf2sql_c.dll")
                    into("$buildDir/image/run-windows-x64/lib")
                }
                copy {
                    from("$projectDir/lib/libbaf2sql_adapter.so", "$projectDir/lib/libbaf2sql_c.so")
                    into("$buildDir/image/run-linux-x64/lib")
                }
            }
        }
    }
}
