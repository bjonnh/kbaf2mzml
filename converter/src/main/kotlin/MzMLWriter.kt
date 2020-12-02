package net.nprod.baf2mzml

import Sample
import net.nprod.baf2mzml.schema.Spectrum
import java.io.File
import java.io.FileOutputStream

class MzMLWriter(file: File, val sampleName: String, val baf2SQL: BAF2SQL) {
    val outputStream: FileOutputStream = FileOutputStream(file)
    val offsetStore = mutableListOf<Offset>()
    var position = 0

    fun execute() {
        mzMLfile(outputStream) {
            addDeclaration()
            content {
                referenceableParamsGroupList {
                    group("CommonMS1SpectrumParams") {
                        +Param.MS1
                        +Param.POSITIVE
                    }
                    group("CommonMS2SpectrumParams") {
                        +Param.MSn
                        +Param.POSITIVE
                    }
                }

                samples {
                    +Sample(name = sampleName)
                }

                runs {
                    newRun(
                        defaultSourceFile = baf2SQL.filename,
                        defaultInstrumentConfiguration = "Something"
                    ) {
                        val listOfScans = mutableListOf<Spectrum>()
                        baf2SQL.spectraDataAct {
                            if ((it.lineData?.mz?.size ?: 0) > 0)
                                listOfScans.add(it)
                        }

                        addSpectrumSource(listOfScans)
                    }
                }
            }
        }
    }
}