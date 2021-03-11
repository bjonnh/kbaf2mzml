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
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.Separator
import javafx.scene.input.TransferMode
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import java.io.File
import kotlin.concurrent.thread

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
    val processing = SimpleBooleanProperty(false)
    val inputFiles = FXCollections.observableArrayList<String>()
}

@Suppress("LongMethod")
class JavaFX : Application() {
    private val model = Model()
    private val defaultDropColor = Color.web("#9090FF")
    val notSetOutputLabel = "Not set, will write where the input files are"

    fun processAll() {
        model.processing.set(true)
        val inputs = model.inputFiles.map { it }

        val task = object : Task<Unit>() {
            override fun call() {
                inputs.map {
                    val outputDir = if (!model.outputDirectory.value.isNullOrEmpty()) {
                        File(model.outputDirectory.value).also { it.mkdirs() }
                    } else File(it).parentFile
                    val analysis = File(File(it), "analysis.baf")
                    println("Converting ${analysis.absolutePath}")
                    val converter = BAF2SQLFile(analysis.absolutePath)
                    converter.addLevelFilter(100.0)

                    converter.saveAsMzMl(
                        File(
                            outputDir,
                            File(it).name.substring(0, File(it).name.length - 2) + ".mzML"
                        ).absolutePath
                    )
                    println(converter.lasterror)
                    Platform.runLater {
                        model.inputFiles.removeAll(it)
                    }
                    converter.close()
                }
                Platform.runLater {
                    model.processing.set(false)
                }
            }
        }
        thread {
            task.run()
        }
        // processFiles.start(inputs, model.outputDirectory.value)
    }

    override fun start(stage: Stage) {

        BAF2SQL.initialize()

        val title = Text("BAF2MzML").apply {
            this.fill = Color.web("#FF0090")
            this.font = Font(32.0)
        }

        val dropLabel = Text("\n\nDrop input directories here\n\n")

        dropLabel.fill = defaultDropColor
        dropLabel.font = Font(64.0)
        val outputBtn = Button("Select Output Directory")
        outputBtn.font = Font(32.0)

        val outputDirLabelHelp = Label("Output Directory: ")
        outputDirLabelHelp.font = Font(32.0)
        val outputDirLabel = Label(notSetOutputLabel).also {
            it.textProperty().bind(
                Bindings.`when`(model.outputDirectory.isNull).then(
                    notSetOutputLabel
                ).otherwise(model.outputDirectory)
            )
        }

        val outDirBox = HBox(outputDirLabelHelp, outputDirLabel)
        outDirBox.alignment = Pos.CENTER_LEFT

        val directoryChooser = DirectoryChooser()

        outputBtn.setOnAction {
            val file: File? = directoryChooser.showDialog(stage)
            if (file != null) model.outputDirectory.set(file.absolutePath)
        }

        val list: ListView<String> = ListView<String>()
        Bindings.bindContent(list.items, model.inputFiles)
        list.setPrefSize(800.0, 400.0)
        val ruler = Separator()
        val inputName = Label("Input files")

        val processBtn = Button("Process").also {
            it.font = Font(32.0)
            it.disableProperty().bind(model.processing)
            it.onAction = EventHandler {
                processAll()
                it.consume()
            }
        }

        val pane = VBox(title, dropLabel, outputBtn, outDirBox, ruler, inputName, list, processBtn)

        with(pane) {
            this.onDragOver = EventHandler { event ->
                if ((event.gestureSource != this) && event.dragboard.hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY, TransferMode.LINK)
                }

                dropLabel.fill = Color.web("#FF0000")
                event.consume()
            }

            this.onDragDropped = EventHandler { event ->
                var completed = false
                val db = event.dragboard
                if (db.hasFiles() && model.processing.value == false) {
                    db.files.filter { it.isDirectory && it.exists() }.forEach {
                        if (!model.inputFiles.contains(it.absolutePath)) model.inputFiles.add(it.absolutePath)
                    }
                    completed = true
                }
                dropLabel.fill = defaultDropColor
                event.isDropCompleted = completed
                event.consume()
            }

            this.onDragExited = EventHandler { event ->
                dropLabel.fill = defaultDropColor
                event.consume()
            }
        }
        pane.alignment = Pos.CENTER
        stage.scene = Scene(pane, 960.0, 800.0)

        stage.scene.stylesheets.add("theme.css")
        stage.show()
    }
}
