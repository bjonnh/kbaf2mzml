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
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.Separator
import javafx.scene.input.TransferMode
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import net.nprod.baf2mzml.baf.BAF2SQLFile
import java.io.File
import java.util.Properties
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

/**
 * Minimal peak height that will be converted (in each individual spectrum)
 */
const val DEFAULT_FILTER_LEVEL = 100.0

const val WINDOW_WIDTH = 1024.0
const val WINDOW_HEIGHT = 800.0

const val LIST_WIDTH = 800.0
const val LIST_HEIGHT = 400.0

const val FONT_SIZE = 16.0

const val SPACING_FACTOR = 2
const val SPACING_FACTOR_LARGE = 3

class PropertiesManager {
    val properties = Properties()
    init {
        val file = this::class.java.getResource("/version.properties").openStream()
        properties.load(file)
    }

    fun version(): String = properties["version"]?.toString() ?: "No version defined"
}

@Suppress("LongMethod")
class JavaFX : Application() {
    private val model = ApplicationModel()
    private val propertiesManager = PropertiesManager()
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
                    converter.setLevelFilter(DEFAULT_FILTER_LEVEL)

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

    @Suppress("ComplexMethod")
    override fun start(stage: Stage) {
        stage.title = "BAF2MZML v${propertiesManager.version()}"

        val title = Text("BAF2MzML v${propertiesManager.version()}").apply {
            this.fill = Color.web("#FF0090")
            this.font = Font(FONT_SIZE)
        }

        val dropLabel = Text("Drop one or more analysis here")
        dropLabel.fill = defaultDropColor
        dropLabel.font = Font(2 * FONT_SIZE)

        val orLabel = Text("Or").also {
            it.font = Font(FONT_SIZE)
            it.textAlignment = TextAlignment.CENTER
        }

        val inputBtn = Button("Select a single analysis")
        inputBtn.font = Font(FONT_SIZE)

        val thenLabel = Text("Then")
        thenLabel.font = Font(FONT_SIZE)

        val outputBtn = Button("Select Output Directory (optional)")
        outputBtn.font = Font(FONT_SIZE)

        val outputDirLabel = Label(notSetOutputLabel).also {
            it.textProperty().bind(
                Bindings.`when`(model.outputDirectory.isNull).then(
                    notSetOutputLabel
                ).otherwise(model.outputDirectory)
            )
        }

        val directoryChooser = DirectoryChooser()

        outputBtn.setOnAction {
            val file: File? = directoryChooser.showDialog(stage)
            if (file != null) model.outputDirectory.set(file.absolutePath)
        }

        inputBtn.setOnAction {
            val file: File? = directoryChooser.showDialog(stage)
            if (file != null) model.inputFiles.add(file.absolutePath)
        }

        val list: ListView<String> = ListView<String>()
        Bindings.bindContent(list.items, model.inputFiles)

        list.setPrefSize(LIST_WIDTH, LIST_HEIGHT)
        val ruler = Separator()
        val inputName = Label("Input files")

        val processBtn = Button("Process").also {
            it.font = Font(FONT_SIZE)
            it.disableProperty().bind(model.processing)
            it.onAction = EventHandler {
                processAll()
                it.consume()
            }
        }

        val exitBtn = Button("Exit")
        exitBtn.font = Font(FONT_SIZE)
        exitBtn.onAction = EventHandler {
            stage.close()
        }

        val pane = VBox(
            title,
            VBox(dropLabel, orLabel, inputBtn).also {
                it.alignment = Pos.CENTER
                it.spacing = FONT_SIZE * SPACING_FACTOR
                it.background = Background(BackgroundFill(Color.BEIGE, CornerRadii.EMPTY, Insets.EMPTY))
            },
            VBox(
                thenLabel,
                HBox(outputBtn, outputDirLabel).also {
                    it.alignment = Pos.CENTER
                    it.spacing = FONT_SIZE * SPACING_FACTOR
                }
            ).also {
                it.alignment = Pos.CENTER
                it.spacing = FONT_SIZE * SPACING_FACTOR
                it.padding = Insets(FONT_SIZE * SPACING_FACTOR)
            },
            ruler,
            inputName,
            list,
            HBox(processBtn, exitBtn).also {
                it.alignment = Pos.CENTER
                it.padding = Insets(FONT_SIZE)
                it.spacing = WINDOW_WIDTH / SPACING_FACTOR_LARGE
            }

        ).also {
            it.alignment = Pos.CENTER
        }

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

        stage.scene = Scene(pane, WINDOW_WIDTH, WINDOW_HEIGHT)

        stage.scene.stylesheets.add("theme.css")
        stage.show()
    }
}
