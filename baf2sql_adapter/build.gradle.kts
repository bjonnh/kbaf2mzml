import org.gradle.internal.jvm.Jvm

plugins {
    `cpp-library`
}

library {
    linkage.set(listOf(Linkage.SHARED))
    binaries.configureEach {
        val task = compileTask.get()
        task.source.setFrom(fileTree("dir" to "src/main/c", "include" to listOf("**/*.c")).toSet())
        task.includes.from("${Jvm.current().javaHome}/include")
        task.includes.from("${Jvm.current().javaHome}/include/linux")

        task.compilerArgs.addAll(compileTask.get().toolChain.map {
            listOf("-x","c", "-O3", "-march=native")
        })

        if (this is ComponentWithSharedLibrary) {
            linkTask.get().apply {
                linkerArgs.addAll("-L../lib", "-lbaf2sql_c")
            }
        }
    }
}

tasks {
    "assemble" {
        doLast {

            // copy the binary to the the resources dir
            copy {
                from("build/lib/main/debug/libbaf2sql_adapter.so")
                into("../lib/")
            }
        }
    }
}
