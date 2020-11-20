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

import net.nprod.baf2mzml.schema.LineData
import net.nprod.baf2mzml.schema.Spectrum
import org.apache.commons.text.StringEscapeUtils
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

fun BAF2SQL.saveAsMzMl(filename: String) {
    val writer = MzMLWriter(File(filename), "Test", this)
    writer.execute()
}

class MzMLWriter(file: File, val sampleName: String, val baf2SQL: BAF2SQL) {

    val writer = file.bufferedWriter()
    var position = 0

    fun execute() {
        addDeclaration()
        addHeader()
        addReferenceableParamsGroupList()
        addSampleList()

        addRun()
        addSpectrumList()
        closeRun()

        //writeIndex()
        closeHeader()
        writer.close()
    }

    fun writeln(content: String) {
        write(content + "\n")
    }

    fun write(content: String) {
        position += content.length
        writer.write(content)
    }

    fun writeByteArray(content: ByteArray) =
        write(content.decodeToString())

    fun addDeclaration() {
        writeln("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>")
    }

    fun addHeader() {
        //writeln("<indexedmzML xmlns=\"http://psi.hupo.org/ms/mzml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://psi.hupo.org/ms/mzml http://psidev.info/files/ms/mzML/xsd/mzML1.1.0_idx.xsd\">")
        writeln("<mzML xmlns=\"http://psi.hupo.org/ms/mzml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://psi.hupo.org/ms/mzml http://psidev.info/files/ms/mzML/xsd/mzML1.1.0.xsd\" id=\"urn:lsid:net.nprod:mzML.instanceDocuments.iwillnotcomplainthatmyfileisbroken\" version=\"1.1.0\">")
        writeln(
            """
            <cvList count="2">
                <cv id="MS" fullName="Proteomics Standards Initiative Mass Spectrometry Ontology" version="2.26.0" URI="http://psidev.cvs.sourceforge.net/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo"/>
                <cv id="UO" fullName="Unit Ontology" version="14:07:2009" URI="http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/phenotype/unit.obo"/>
            </cvList>
        """.trimIndent()
        )
        writeln("<fileDescription></fileDescription>")
    }

    fun addReferenceableParamsGroupList() { // TODO!!!
        writeln(
            """<referenceableParamGroupList count="2">
      <referenceableParamGroup id="CommonMS1SpectrumParams">
        <cvParam cvRef="MS" accession="MS:1000579" name="MS1 spectrum" value=""/>
        <cvParam cvRef="MS" accession="MS:1000130" name="positive scan" value=""/>
      </referenceableParamGroup>
      <referenceableParamGroup id="CommonMS2SpectrumParams">
        <cvParam cvRef="MS" accession="MS:1000580" name="MSn spectrum" value=""/>
        <cvParam cvRef="MS" accession="MS:1000130" name="positive scan" value=""/>
      </referenceableParamGroup>
    </referenceableParamGroupList>"""
        )
    }

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

    fun addSampleList() { // TODO!!!
        writeln(
            """<sampleList count="1">
      <sample id="${safe(sampleName)}" name="Sample 1">
      </sample>
    </sampleList>"""
        )
    }

    // TODO!
    fun addRun() =
        writeln(
            "<run id=\"Experiment_x0020_1\" defaultInstrumentConfigurationRef=\"LCQ_x0020_Deca\" sampleRef=\"_x0032_0090101_x0020_-_x0020_Sample_x0020_1\" startTimeStamp=\"2007-06-27T15:23:45.00035\" defaultSourceFileRef=\"${
                safe(
                    baf2SQL.filename
                )
            }\">"
        )

    fun closeRun() = writeln("</run>")

    fun addSpectrumList() {
        val listOfScans = mutableListOf<Spectrum>()
        val scans = baf2SQL.spectraDataAct {
            if ((it.lineData?.mz?.size ?: 0) > 0)
                listOfScans.add(it)
        }

        writeln("<spectrumList count=\"${listOfScans.size}\" defaultDataProcessingRef=\"pwiz_processing\">")

        listOfScans.mapIndexed { index, spectrum ->
            writeln("<spectrum index=\"$index\" id=\"scan=${spectrum.id}\" defaultArrayLength=\"${spectrum.lineData?.mz?.size}\">")

            when (spectrum.acquisitionKey.msLevel) {
                0 -> writeln("<referenceableParamGroupRef ref=\"CommonMS1SpectrumParams\"/>")
                1 -> writeln("<referenceableParamGroupRef ref=\"CommonMS2SpectrumParams\"/>")
            }

            msLevel(spectrum.acquisitionKey.msLevel + 1)
            centroid()
            lowMass(spectrum.profileData?.mz?.minOrNull() ?: 0.0)
            highMass(spectrum.profileData?.mz?.maxOrNull() ?: 0.0)

            val basepeakIndex = spectrum.profileData?.intensity?.withIndex()?.maxByOrNull { it.value }?.index
            if (basepeakIndex != null) {
                basePeak(spectrum.profileData!!.mz[basepeakIndex])
                basePeakIntensity(spectrum.profileData!!.intensity[basepeakIndex])
            }

            tic(spectrum.sumIntensity)

            scanList(spectrum)
            precursorList(spectrum)
            binaryData(spectrum.lineData)

            writeln("</spectrum>")
        }

        writeln("</spectrumList>")

    }

    private fun precursorList(spectrum: Spectrum) {
        if (spectrum.acquisitionKey.msLevel > 0) {
            if (spectrum.acquisitionData.msmsIsolationmassAct != null) {
                writeln("<precursorList count=\"1\">")
                writeln("  <precursor spectrumRef=\"scan=${spectrum.parent}\">")
                writeln("    <selectedIonList count=\"1\">")
                writeln("      <selectedIon>")
                writeln("        <cvParam cvRef=\"MS\" accession=\"MS:1000744\" name=\"selected ion m/z\" value=\"${spectrum.acquisitionData.msmsIsolationmassAct}\" unitCvRef=\"MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\"/>")
                writeln("      </selectedIon>")
                writeln("    </selectedIonList>")
                writeln("    <activation>")
                writeln("      <cvParam cvRef=\"MS\" accession=\"MS:1000133\" name=\"collision-induced dissociation\" value=\"\"/>")
                writeln(
                    "      <cvParam cvRef=\"MS\" accession=\"MS:1000045\" name=\"collision energy\" value=\"${spectrum.acquisitionData.collisionEnergyAct ?: 0.0}\" unitCvRef=\"UO\"" +
                            " unitAccession=\"UO:0000266\" unitName=\"electronvolt\"/>"
                )
                writeln("    </activation>")
                writeln("  </precursor>")
                writeln("</precursorList>")
            }
        }
    }

    private fun scanList(spectrum: Spectrum) {
        writeln("  <scanList count=\"1\">")
        writeln("    <cvParam cvRef=\"MS\" accession=\"MS:1000795\" name=\"no combination\" value=\"\"/>")
        writeln("    <scan instrumentConfigurationRef=\"ImpactII\">")
        writeln("       <cvParam cvRef=\"MS\" accession=\"MS:1000016\" name=\"scan start time\" value=\"${spectrum.rt / 60}\" unitCvRef=\"UO\" unitAccession=\"UO:0000031\" unitName=\"minute\"/>")
        writeln("       <cvParam cvRef=\"MS\" accession=\"MS:1000512\" name=\"filter string\" value=\"+ c TODO!!!\"/>")
        writeln(
            "       <scanWindowList count=\"1\"><scanWindow> <cvParam cvRef=\"MS\" accession=\"MS:1000501\" name=\"scan window lower limit\" value=\"400\" unitCvRef=\"MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\"/>\n" +
                    "                  <cvParam cvRef=\"MS\" accession=\"MS:1000500\" name=\"scan window upper limit\" value=\"1800\" unitCvRef=\"MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\"/></scanWindow></scanWindowList>"
        )
        writeln("    </scan>")
        writeln("  </scanList>")
    }

    private fun binaryData(lineData: LineData?) {
        if (lineData == null) throw RuntimeException("Sorry only line data for now")
        writeln("<binaryDataArrayList count=\"2\">")
        binaryDataArray(lineData.mz, mz = true)
        binaryDataArray(lineData.intensity.map { it.toFloat() }.toFloatArray(), mz = false)
        writeln("</binaryDataArrayList>")
    }

    private fun binaryDataArrayWriter(encoded: ByteArray, mz: Boolean, double: Boolean) {
        writeln("<binaryDataArray encodedLength=\"${encoded.size}\" dataProcessingRef=\"kbaf2mzml\">")
        if (double) {
            writeln("  <cvParam cvRef=\"MS\" accession=\"MS:1000523\" name=\"64-bit float\" value=\"\"/>")
        } else {
            writeln("  <cvParam cvRef=\"MS\" accession=\"MS:1000521\" name=\"32-bit float\" value=\"\"/>")
        }
        writeln("  <cvParam cvRef=\"MS\" accession=\"MS:1000576\" name=\"no compression\" value=\"\"/>")
        if (mz) {
            writeln("<cvParam cvRef=\"MS\" accession=\"MS:1000514\" name=\"m/z array\" value=\"\" unitCvRef=\"MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\"/>")
        } else {
            writeln("<cvParam cvRef=\"MS\" accession=\"MS:1000515\" name=\"intensity array\" value=\"\" unitCvRef=\"MS\" unitAccession=\"MS:1000131\" unitName=\"number of counts\"/>")
        }
        write("<binary>")
        writeByteArray(encoded)
        write("</binary>")
        writeln("</binaryDataArray>")
    }

    private fun binaryDataArray(data: DoubleArray, mz: Boolean) =
        binaryDataArrayWriter(base64ArrayEncoder(data), mz, true)

    private fun binaryDataArray(data: FloatArray, mz: Boolean) =
        binaryDataArrayWriter(base64ArrayEncoder(data), mz, false)

    private fun base64ArrayEncoder(data: DoubleArray): ByteArray {
        var encodedData: ByteArray? = null
        val buffer = ByteBuffer.allocate(data.size * 8)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        for (d in data) {
            buffer.putDouble(d)
        }
        encodedData = buffer.array()
        return Base64.getEncoder().encode(encodedData)
    }

    private fun base64ArrayEncoder(data: FloatArray): ByteArray {
        var encodedData: ByteArray? = null
        val buffer = ByteBuffer.allocate(data.size * 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        for (d in data) {
            buffer.putFloat(d)
        }
        encodedData = buffer.array()
        return Base64.getEncoder().encode(encodedData)
    }

    private fun lowMass(number: Number) =
        writeln("<cvParam cvRef=\"MS\" accession=\"MS:1000528\" name=\"lowest observed m/z\" value=\"$number\" unitCvRef=\"MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\"/>")

    private fun highMass(number: Number) =
        writeln("<cvParam cvRef=\"MS\" accession=\"MS:1000527\" name=\"highest observed m/z\" value=\"$number\" unitCvRef=\"MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\"/>")

    private fun basePeak(number: Number) =
        writeln("<cvParam cvRef=\"MS\" accession=\"MS:1000504\" name=\"base peak m/z\" value=\"$number\" unitCvRef=\"MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\"/>")

    private fun basePeakIntensity(number: Number) =
        writeln("<cvParam cvRef=\"MS\" accession=\"MS:1000505\" name=\"base peak intensity\" value=\"$number\" unitCvRef=\"MS\" unitAccession=\"MS:1000131\" unitName=\"number of counts\"/>")

    private fun tic(number: Number) =
        writeln("<cvParam cvRef=\"MS\" accession=\"MS:1000285\" name=\"total ion current\" value=\"${number.toInt()}\"/>")

    private fun writeIndex() {
        println("Writing Index is not supported yet (ever?)")
    }

    private fun closeHeader() {
        writeln("</mzML>")
        //writeln("</indexedmzML>")
    }

    private fun safe(s: String) = StringEscapeUtils.ESCAPE_XML11.translate(s)

    private fun msLevel(level: Int) =
        writeln("<cvParam cvRef=\"MS\" accession=\"MS:1000511\" name=\"ms level\" value=\"$level\"/>")

    private fun centroid() =
        writeln("<cvParam cvRef=\"MS\" accession=\"MS:1000127\" name=\"centroid spectrum\" value=\"\"/>")
}