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

import net.nprod.baf2mzml.helpers.base64ArrayEncoder
import net.nprod.baf2mzml.schema.LineData
import net.nprod.baf2mzml.schema.Spectrum

/**
 * DSL class to construct a run.
 */
class RunsBuilder {
    private val content = mutableListOf<Run>()

    val list: List<Run>
        get() = content

    private var counter = 0
    operator fun Run.unaryPlus(): Run {
        val run = if (this.id == null) {
            this.copy(id = "run $counter").also { counter++ }
        } else {
            this
        }
        content.add(run)
        return run
    }

    fun newRun(defaultSourceFile: String, defaultInstrumentConfiguration: String, f: Run.() -> Unit) {
        val run =
            Run(defaultInstrumentConfiguration = defaultInstrumentConfiguration, defaultSourceFile = defaultSourceFile)
        (+run).apply(f)
    }
}

/**
 * Describe a run
 */
data class Run(
    val id: String? = null,
    val defaultInstrumentConfiguration: String,
    val defaultSourceFile: String
) {
    var spectrumSource: List<Spectrum>? = null

    /**
     * Write the content of that run to a file
     * As we need performance here, we are passing the mzMLFile along
     */
    fun writeToFile(mzMLFile: MzMLFile) {
        mzMLFile.writeln(
            """<run id="${XMLsafe(id)}" defaultInstrumentConfigurationRef="${
            XMLsafe(
                defaultInstrumentConfiguration
            )
            }" defaultSourceFileRef="${
            XMLsafe(
                defaultSourceFile
            )
            }">"""
        )

        // Generate Content
        spectrumSource?.let {
            writeSpectrumList(mzMLFile, it)
        }

        mzMLFile.writeln("</run>")
    }

    /**
     * For now we only handle a single source for spectra
     */
    fun addSpectrumSource(source: List<Spectrum>) {
        println("Adding it")
        this.spectrumSource = source
    }

    fun writeSpectrumList(mzMLFile: MzMLFile, source: List<Spectrum>) {
        with(mzMLFile) { writeln("""<spectrumList count="${source.size}" defaultDataProcessingRef="kbaf_processing">""") }

        source.mapIndexed { index, spectrum ->
            val id = "scan=${spectrum.id}"
            mzMLFile.run {
                addOffset(id)
                writeln("""<spectrum index="$index" id="$id" defaultArrayLength="${spectrum.lineData?.mz?.size}">""")

                when (spectrum.acquisitionKey.msLevel) {
                    0 -> mzMLFile.writeln("""<referenceableParamGroupRef ref="CommonMS1SpectrumParams"/>""")
                    1 -> mzMLFile.writeln("""<referenceableParamGroupRef ref="CommonMS2SpectrumParams"/>""")
                }

                write(Param.msLevel(spectrum.acquisitionKey.msLevel + 1))
                write(Param.centroid())
                write(Param.lowMass(spectrum.profileData?.mz?.minOrNull() ?: 0.0))
                write(Param.highMass(spectrum.profileData?.mz?.maxOrNull() ?: 0.0))
            }

            val basepeakIndex = spectrum.profileData?.intensity?.withIndex()?.maxByOrNull { it.value }?.index
            if (basepeakIndex != null) {
                mzMLFile.write(Param.basePeak(spectrum.profileData.mz[basepeakIndex]))
                mzMLFile.write(Param.basePeakIntensity(spectrum.profileData.intensity[basepeakIndex]))
            }

            mzMLFile.write(Param.tic(spectrum.sumIntensity))

            writeScanList(mzMLFile, spectrum)
            precursorList(mzMLFile, spectrum)
            binaryData(mzMLFile, spectrum.lineData)

            mzMLFile.writeln("""</spectrum>""")
        }

        mzMLFile.writeln("""</spectrumList>""")
    }

    fun writeScanList(mzMLFile: MzMLFile, spectrum: Spectrum) {
        mzMLFile.run {
            writeln("""  <scanList count="1">""")
            write(Param.noCombination)
            writeln("""    <scan instrumentConfigurationRef="ImpactII">""")
            writeln("""       <cvParam cvRef="MS" accession="MS:1000016" name="scan start time" value="${spectrum.rt / 60}" unitCvRef="UO" unitAccession="UO:0000031" unitName="minute"/>""")
            writeln("""       <cvParam cvRef="MS" accession="MS:1000512" name="filter string" value="+ c TODO!!!"/>""")
            writeln(
                """       <scanWindowList count="1"><scanWindow>"""
            )
            writeln(
                """<cvParam cvRef="MS" accession="MS:1000501" name="scan window lower limit" value="400" unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/>
"""
            )
            writeln(
                """<cvParam cvRef="MS" accession="MS:1000500" name="scan window upper limit" value="1800" unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/></scanWindow></scanWindowList>"""
            )
            writeln("    </scan>")
            writeln("  </scanList>")
        }
    }

    fun precursorList(mzMLFile: MzMLFile, spectrum: Spectrum) {
        if (spectrum.acquisitionKey.msLevel > 0) {
            if (spectrum.acquisitionData.msmsIsolationmassAct != null) {
                with(mzMLFile) {
                    writeln("""<precursorList count="1">""")
                    writeln("""  <precursor spectrumRef="scan=${spectrum.parent}">""")
                    writeln("""    <selectedIonList count="1">""")
                    writeln("""      <selectedIon>""")
                    writeln("""        <cvParam cvRef="MS" accession="MS:1000744" name="selected ion m/z" value="${spectrum.acquisitionData.msmsIsolationmassAct}" unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/>""")
                    writeln("""      </selectedIon>""")
                    writeln("""    </selectedIonList>""")
                    writeln("""    <activation>""")
                    writeln("""      <cvParam cvRef="MS" accession="MS:1000133" name="collision-induced dissociation" value=""/>""")
                    writeln(
                        """      <cvParam cvRef="MS" accession="MS:1000045" name="collision energy" value="${spectrum.acquisitionData.collisionEnergyAct ?: 0.0}" unitCvRef="UO"""" +
                            """ unitAccession="UO:0000266" unitName="electronvolt"/>"""
                    )
                    writeln("""    </activation>""")
                    writeln("""  </precursor>""")
                    writeln("""</precursorList>""")
                }
            }
        }
    }

    private fun binaryData(mzMLFile: MzMLFile, lineData: LineData?) {
        if (lineData == null) throw RuntimeException("Sorry only line data for now")
        mzMLFile.writeln("""<binaryDataArrayList count="2">""")
        binaryDataArrayWriter(
            mzMLFile,
            base64ArrayEncoder(lineData.mz),
            mz = true,
            double = true
        )
        binaryDataArrayWriter(
            mzMLFile,
            base64ArrayEncoder(lineData.intensity.map { it.toFloat() }.toFloatArray()),
            mz = false,
            double = false
        )
        mzMLFile.writeln("</binaryDataArrayList>")
    }

    private fun binaryDataArrayWriter(mzMLFile: MzMLFile, encoded: ByteArray, mz: Boolean, double: Boolean) {
        with(mzMLFile) {
            writeln("""<binaryDataArray encodedLength="${encoded.size}" dataProcessingRef="kbaf2mzml">""")
            if (double) {
                writeln("""  <cvParam cvRef="MS" accession="MS:1000523" name="64-bit float" value=""/>""")
            } else {
                writeln("""  <cvParam cvRef="MS" accession="MS:1000521" name="32-bit float" value=""/>""")
            }
            writeln("""  <cvParam cvRef="MS" accession="MS:1000576" name="no compression" value=""/>""")
            if (mz) {
                writeln("""<cvParam cvRef="MS" accession="MS:1000514" name="m/z array" value="" unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/>""")
            } else {
                writeln("""<cvParam cvRef="MS" accession="MS:1000515" name="intensity array" value="" unitCvRef="MS" unitAccession="MS:1000131" unitName="number of counts"/>""")
            }
            write("<binary>")
            write(encoded)
            write("</binary>")
            writeln("</binaryDataArray>")
        }
    }

/*
fun addSoftwareList() { // TODO!!!
    writeln(
        """
        <softwareList count="3">
  <software id="Bioworks" version="3.3.1 sp1">
    <cvParam cvRef="MS" accession="MS:1000533" name="Bioworks" value=""/>
  </software>
  <software id="pwiz" version="1.0">
    <cvParam cvRef="MS" accession="MS:1000615" name="ProteoWizard" value=""/>
  </software>
  <software id="CompassXtract" version="2.0.5">
    <cvParam cvRef="MS" accession="MS:1000718" name="CompassXtract" value=""/>
  </software>
</softwareList>
    """.trimIndent()
    )
}
*/
}
