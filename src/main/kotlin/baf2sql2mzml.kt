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

import com.google.common.collect.Range
import io.github.msdk.datamodel.*
import io.github.msdk.io.mzml.MzMLFileExportMethod
import io.github.msdk.io.mzml.data.MzMLCompressionType
import io.github.msdk.util.tolerances.MzTolerance
import net.nprod.baf2mzml.schema.Spectrum
import java.io.File
import java.util.*

class BAFMsScan(val spectrum: Spectrum) : MsScan {
    override fun getSpectrumType(): MsSpectrumType =
        MsSpectrumType.CENTROIDED

    override fun getNumberOfDataPoints(): Int = spectrum.profileData?.mz?.size ?: 0

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

    override fun getScanDefinition(): String? = null // TODO?

    override fun getMsFunction(): String? = null // TODO?

    override fun getMsLevel(): Int = spectrum.acquisitionKey.msLevel + 1

    override fun getRetentionTime(): Float = spectrum.rt.toFloat()

    override fun getScanningRange(): Range<Double>? = null

    override fun getSourceInducedFragmentation(): ActivationInfo? = null // TODO!!

    override fun getIsolations(): List<IsolationInfo> = listOf() // TODO!!
}

class BAFAsRawDataFile(val baf2SQL: BAF2SQL) : RawDataFile {
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
            listOfScans.add(BAFMsScan(it))
        }
        return listOfScans
    }

    override fun getChromatograms(): List<Chromatogram> = listOf()

    override fun dispose() {
        TODO("Dispose Not yet implemented")
    }
}


fun BAF2SQL.saveAsMzMl(filename: String) {
    val exporter = MzMLFileExportMethod(
        BAFAsRawDataFile(this),
        File(filename),
        MzMLCompressionType.NUMPRESS_POSINT,
        MzMLCompressionType.NO_COMPRESSION
    )

    exporter.execute()
}