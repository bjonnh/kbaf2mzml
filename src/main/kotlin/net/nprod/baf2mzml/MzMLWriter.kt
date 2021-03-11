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

import net.nprod.baf2mzml.schema.Spectrum
import java.io.File
import java.io.FileOutputStream

class MzMLWriter(file: File, val sampleName: String, val baf2SQL: BAF2SQLFile) {
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
