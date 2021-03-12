import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm")
    application
    id("org.beryx.jlink")
    id("com.github.ben-manes.versions")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jmailen.kotlinter")
    id("org.jetbrains.changelog")
    // id("org.openjfx.javafxplugin")  // This doesn't work…
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
val compileJava: JavaCompile by tasks
compileJava.destinationDir = compileKotlin.destinationDir

val publicationName: String by project
group = "net.nprod"
version = "0.0.1" + if (System.getProperty("snapshot")?.isEmpty() != false) {
    ""
} else {
    "-SNAPSHOT"
}

repositories {
    mavenCentral()
    jcenter()
}

application {
    mainModule.set("baf2mzml")
    mainClass.set("net.nprod.baf2mzml.JavaFX")
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

java {
    modularity.inferModulePath.set(true)
}

dependencies {
    val coroutinesVersion: String by project
    val sqliteVersion: String by project
    val apacheCommonsTextVersion: String by project
    val cliktVersion: String by project
    val javafxVersion: String by project

    implementation(kotlin("stdlib"))
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    implementation("org.apache.commons:commons-text:$apacheCommonsTextVersion")
    implementation("com.github.ajalt.clikt:clikt:$cliktVersion") {
        exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
    }

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:$coroutinesVersion")

    val os: OperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()
    val platform =
        if (os.isLinux) "linux" else if (os.isWindows) "windows" else throw RuntimeException(
            "Your OS isn't managed yet. To port it, you will need to have the bruker library compiled\n" +
                " for your OS and your platform, so you will have to contact them. Good luck with that."
        )

    implementation("org.openjfx:javafx-base:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-controls:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-web:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-media:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-fxml:$javafxVersion:$platform")
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
    // addExtraDependencies("com.github.ajalt.clikt")
    imageZip.set(file("$buildDir/kbaf2mzml-$version.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    /*targetPlatform("windows-x64") {
        setJdkHome("stuff/jdk-15.0.2+7")
        extraModulePaths.add("stuff/javafx-jmods-15.0.1")
    }
    targetPlatform("linux-x64") {
	setJdkHome("stuff/jdk-15.0.2+7")
    	extraModulePaths.add("stuff/javafx-jmods-15.0.1")
    }*/
    addExtraDependencies("kotlinx.coroutines.core.jvm")
    mergedModule {

        forceMerge("kotlin", "sqlite-jdbc", "kotlinx.coroutines.core.jvm", "kotlinx.coroutines.core")
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
                    into("$buildDir/image/lib")
                }
                copy {
                    from("$projectDir/lib/libbaf2sql_adapter.so", "$projectDir/lib/libbaf2sql_c.so")
                    into("$buildDir/image/lib")
                }
            }
        }
    }
}

// Use the new compiler
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        useIR = true
    }
}
