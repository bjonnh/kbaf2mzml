package net.nprod.baf2mzml

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections

/**
 * Representation of the internal model of the application
 */
class ApplicationModel {
    /**
     * Where the files are going to be written, can be empty
     */
    val outputDirectory = SimpleStringProperty()

    /**
     * Is it currently processing
     */
    val processing = SimpleBooleanProperty(false)

    /**
     * List of input files (they are cleared as they are processed)
     */
    val inputFiles = FXCollections.observableArrayList<String>()
}
