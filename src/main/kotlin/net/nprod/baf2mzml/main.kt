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

/*import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path*/
import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.input.TransferMode
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import java.io.File

/*
class Converter : CliktCommand() {
    val output: String? by option(help = "Output directory")
        .help("output directory, if not given, will build in the directory just up the requested converted file")
    val source: Set<Path> by argument().path(mustExist = true).multiple().unique()

    override fun run() {
        source.map {
            val outputDir = output?.let { File(output).also { it.mkdirs() } } ?: it.toFile().parentFile
            val analysis = File(it.toFile(), "analysis.baf")
            println("Converting ${analysis.absolutePath}")
            val converter = BAF2SQL(analysis.absolutePath)
            converter.addLevelFilter(100.0)
            val parent = analysis.parent
            converter.saveAsMzMl(
                File(
                    outputDir,
                    it.toFile().name.substring(0, it.toFile().name.length - 2) + ".mzML"
                ).absolutePath
            )
            println(converter.lasterror)
            converter.close()
        }
    }
}

fun main(args: Array<String>) = Converter().main(args)
*/

class Model {
    val outputDirectory = SimpleStringProperty()
}

class JavaFX : Application() {
    private val model = Model()
    private val defaultDropColor = Color.web("#9090FF")

    override fun start(stage: Stage) {
        val title = Text("BAF2MzML").apply {
            this.fill = Color.web("#FF0090")
            this.font = Font(32.0)
        }

        val dropLabel = Text("\n\nDrop input directories here\n\n")

        dropLabel.fill = defaultDropColor
        dropLabel.font = Font(64.0)
        val button = Button("Select Output Directory")

        val outputDirLabelHelp = Label("Output Directory: ")
        val outputDirLabel = Label("Not set").also {
            it.textProperty().bind(
                Bindings.`when`(model.outputDirectory.isNull()).then(
                    "Not set"
                ).otherwise(model.outputDirectory)
            )
        }

        val outDirBox = HBox(outputDirLabelHelp, outputDirLabel)

        val directoryChooser = DirectoryChooser()

        button.setOnAction {
            val file: File? = directoryChooser.showDialog(stage)
            if (file != null) model.outputDirectory.set(file.absolutePath)
        }
        val pane = VBox(title, dropLabel, button, outDirBox)


        with(pane) {
            this.onDragOver = EventHandler { event ->
                if ((event.gestureSource != this) && event.dragboard.hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY, TransferMode.LINK, TransferMode.MOVE)
                }

                dropLabel.fill = Color.web("#FF0000")
                event.consume()
            }

            this.onDragDropped = EventHandler { event ->
                var completed = false
                val db = event.dragboard
                if (db.hasFiles()) {
                    println(db.files.filter { it.isDirectory }) // TODO
                    completed = true
                }

                event.isDropCompleted = true
                dropLabel.fill = defaultDropColor
                event.consume()
            }

            this.onDragExited = EventHandler { event ->
                dropLabel.fill = defaultDropColor
                event.consume()
            }
        }
        pane.alignment = Pos.CENTER
        stage.scene = Scene(pane, 960.0, 600.0)

        stage.scene.stylesheets.add("theme.css")
        stage.show()
    }
}