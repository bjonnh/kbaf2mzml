/*
An ultrafast Bruker BAF to MzML converter
Copyright (C) 2020 Jonathan Bisson

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package net.nprod.baf2mzml

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import java.io.File
import java.nio.file.Path

class Converter : CliktCommand() {
    val output: String by option(help = "Output directory").required()
    val source: Set<Path> by argument().path(mustExist = true).multiple().unique()

    override fun run() {
        val output = File(output).also { it.mkdirs() }
        source.map {
            val analysis = File(it.toFile(), "analysis.baf")
            println("Converting ${analysis.absolutePath}")
            val converter = BAF2SQL(analysis.absolutePath)
            converter.addLevelFilter(100.0)
            val parent = analysis.parent
            converter.saveAsMzMl(
                File(
                    output,
                    it.toFile().name.substring(0, it.toFile().name.length - 2) + ".mzML"
                ).absolutePath
            )
            println(converter.lasterror)
            converter.close()
        }
    }
}

fun main(args: Array<String>) = Converter().main(args)
