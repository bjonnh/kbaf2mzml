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
import net.nprod.baf2mzml.schema.ProfileData
import net.nprod.baf2mzml.schema.Spectrum
import org.apache.commons.text.StringEscapeUtils
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/*
class BAFActivationInfo(val spectrum: Spectrum) : ActivationInfo {
    override fun getActivationType(): ActivationType = ActivationType.CID

    override fun getActivationEnergy(): Double = spectrum.acquisitionData.collisionEnergyAct ?: 0.0
}

class BAFIsolationInfo(val spectrum: Spectrum) : IsolationInfo {
    val range: Double = spectrum.acquisitionData.quadrupoleIsolationresolutionAct ?: 10.0
    override fun getIsolationMzRange(): Range<Double> = Range.open(
        (spectrum.acquisitionData.msmsIsolationmassAct ?: 0.0) - range,
        (spectrum.acquisitionData.msmsIsolationmassAct ?: 0.0) + range
    )

    override fun getIonInjectTime(): Float? = null

    override fun getPrecursorMz(): Double? {
        println(spectrum.acquisitionData.msmsIsolationmassAct)
        return spectrum.acquisitionData.msmsIsolationmassAct
    }

    override fun getPrecursorCharge(): Int = 0

    override fun getPrecursorScanNumber(): Int = spectrum.parent

    override fun getActivationInfo(): ActivationInfo = BAFActivationInfo(spectrum)
}

class BAFMsScan(val spectrum: Spectrum) : MzMLScan() {
    override fun getSpectrumType(): MsSpectrumType =
        MsSpectrumType.CENTROIDED

    override fun getNumberOfDataPoints(): Int = spectrum.lineData?.mz?.size ?: 0

    override fun getMzValues(array: DoubleArray?): DoubleArray = spectrum.lineData?.mz
        ?: throw RuntimeException("Sorry no centroided data for scan ${spectrum.id}")

    override fun getIntensityValues(array: FloatArray?): FloatArray =
        spectrum.lineData?.intensity?.map { it.toFloat() }?.toFloatArray()
            ?: throw RuntimeException("Sorry no centroided data for scan ${spectrum.id}")

    override fun getTIC(): Float = spectrum.maxIntensity.toFloat()

    override fun getMzRange(): Range<Double> =
        Range.open(spectrum.mzAcqRangeLower.toDouble(), spectrum.mzAcqRangeUpper.toDouble())

    override fun getMzTolerance(): MzTolerance? {
        TODO("Not yet implemented")
    }

    override fun getRawDataFile(): RawDataFile? {
        TODO("Not yet implemented")
    }

    override fun getScanNumber(): Int = spectrum.id

    override fun getScanDefinition(): String = "DEF"

    override fun getMsFunction(): String = "FUN"

    override fun getMsLevel(): Int = spectrum.acquisitionKey.msLevel + 1

    override fun getRetentionTime(): Float = spectrum.rt.toFloat()

    override fun getScanningRange(): Range<Double> = mzRange

    override fun getSourceInducedFragmentation() = BAFActivationInfo(spectrum)

    override fun getIsolations(): List<IsolationInfo> = listOf(BAFIsolationInfo(spectrum)) // TODO!!
}
/*
class BAFAsRawDataFile(val baf2SQL: BAF2SQL) : MzMLRawDataFile(

) {
    override fun getName(): String =
        baf2SQL.filename

    override fun getOriginalFile(): Optional<File> =
        Optional.of(File(baf2SQL.filename))

    override fun getRawDataFileType(): FileType =
        FileType.UNKNOWN

    override fun getMsFunctions(): List<String> {
        TODO("Get MsFunctions Not yet implemented")
    }

    override fun getScans(): List<MsScan> {
        val listOfScans = mutableListOf<MsScan>()
        baf2SQL.spectraDataAct {
            // MzMine doesn't like empty scans so we remove them
            if ((it.lineData?.mz?.size ?: 0) > 0)
                listOfScans.add(BAFMsScan(it))
        }
        return listOfScans
    }

    override fun getChromatograms(): List<Chromatogram> = listOf()

    override fun dispose() {
        TODO("Dispose Not yet implemented")
    }
}*/
*/
fun BAF2SQL.saveAsMzMl(filename: String) {

    val writer = MzMLWriter(File(filename), "Test", this)
    writer.execute()
    /*val scans = mutableListOf<MsScan>()
    spectraDataAct {
        // MzMine doesn't like empty scans so we remove them
        if ((it.lineData?.mz?.size ?: 0) > 0)
            scans.add(BAFMsScan(it))
    }

    val dataFile = MzMLRawDataFile(
        File(this.filename), listOf(), scans,
        listOf() // chromatograms
    )


    val exporter = MzMLFileExportMethod(
        dataFile,
        File(filename),
        MzMLCompressionType.NO_COMPRESSION,
        MzMLCompressionType.NO_COMPRESSION
    )

    exporter.execute()*/
    // Impossible to use MSDK, files are not valid (doesn't take the precursor into account for example)


}

// TODO: MS2
class MzMLWriter(val file: File, val sampleName: String, val baf2SQL: BAF2SQL) {

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
                writeln("        <cvParam cvRef=\"MS\" accession=\"MS:1000744\" name=\"selected ion m/z\" value=\"${spectrum.acquisitionData.msmsIsolationmassAct }\" unitCvRef=\"MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\"/>")
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
        """
                        <isolationWindow>
                            <cvParam cvRef="MS" accession="MS:1000827" name="isolation window target m/z"
                                     value="445.30000000000001" unitCvRef="MS" unitAccession="MS:1000040"
                                     unitName="m/z"/>
                            <cvParam cvRef="MS" accession="MS:1000828" name="isolation window lower offset" value="0.5"
                                     unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/>
                            <cvParam cvRef="MS" accession="MS:1000829" name="isolation window upper offset" value="0.5"
                                     unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/>
                        </isolationWindow>
                        <selectedIonList count="1">
                            <selectedIon>
                                <cvParam cvRef="MS" accession="MS:1000744" name="selected ion m/z" value="445.33999999999997" unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/>
                                <cvParam cvRef="MS" accession="MS:1000042" name="peak intensity" value="120053"/>
                                <cvParam cvRef="MS" accession="MS:1000041" name="charge state" value="2"/>
                            </selectedIon>
                        </selectedIonList>
                   """
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
        binaryDataArray(lineData.intensity, mz = false)
        writeln("</binaryDataArrayList>")
    }

    private fun binaryDataArray(data: DoubleArray, mz: Boolean) {
        val encode = base64ArrayEncoder(data)
        writeln("<binaryDataArray encodedLength=\"${encode.size}\" dataProcessingRef=\"kbaf2mzml\">")
        writeln("  <cvParam cvRef=\"MS\" accession=\"MS:1000523\" name=\"64-bit float\" value=\"\"/>")
        writeln("  <cvParam cvRef=\"MS\" accession=\"MS:1000576\" name=\"no compression\" value=\"\"/>")
        if (mz) {
            writeln("<cvParam cvRef=\"MS\" accession=\"MS:1000514\" name=\"m/z array\" value=\"\" unitCvRef=\"MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\"/>")
        } else {
            writeln("<cvParam cvRef=\"MS\" accession=\"MS:1000515\" name=\"intensity array\" value=\"\" unitCvRef=\"MS\" unitAccession=\"MS:1000131\" unitName=\"number of counts\"/>")
        }
        write("<binary>")
        writeByteArray(encode)
        write("</binary>")
        writeln("</binaryDataArray>")
    }

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