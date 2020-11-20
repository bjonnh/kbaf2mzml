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

import net.nprod.baf2mzml.schema.*
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

typealias BinaryStorage = Long

class BAFDoubleArray(var ret: Int, var length: Long, var array: DoubleArray)

class NumElements(var ret: Int, var value: Long) {

    override fun toString(): String {
        return ("Return: ${this.ret} Value: ${this.value}")
    }
}

/**
 * Handle BAF files
 * this assumes you have enough memory, we are not going to stream from disk
 */
class BAF2SQL(val filename: String) {
    private var levelFilter: Double? = null
    private var sqliteDb: String? = null
    private var storage: BinaryStorage? = null
    private var connection: Connection? = null

    /**
     * Show the last error from the C library
     */
    val lasterror: String
        get() = c_baf2sql_get_last_error_string()

    init {
        System.load(File("lib/libbaf2sql_c.so").absolutePath)
        System.load(File("lib/libbaf2sql_adapter.so").absolutePath)
        c_baf2sql_set_num_threads(4) // try to keep that at n_cores/2 or even n_cores/4
        sqliteDb = c_baf2sql_get_sqlite_cache_filename(filename)
        storage = c_baf2sql_array_open_storage_calibrated(filename)
        connection = DriverManager.getConnection("jdbc:sqlite:$sqliteDb")
    }

    /**
     * Close
     */

    fun close() {
        storage?.let { c_baf2sql_array_close_storage(it) }
        connection?.let { it.close() }
    }

    fun supportedVariables(): Map<Int, SupportedVariable> {
        require(connection != null && (connection?.isClosed == false)) { "Connection has to be open" }

        val statement = connection?.createStatement()
            ?: throw RuntimeException("Connection is not open, something failed with the database and this was not expected.")

        statement.queryTimeout = 30

        val supportedVariables = mutableMapOf<Int, SupportedVariable>()

        val rs =
            statement.executeQuery(
                "SELECT Variable, PermanentName, Type, DisplayGroupName," +
                        " DisplayValueText, DisplayFormat, DisplayDimension FROM SupportedVariables"
            )

        while (rs.next()) {
            supportedVariables[rs.getInt(1)] = SupportedVariable(
                id = rs.getInt(1),
                permanentName = rs.getString(2),
                displayGroupName = rs.getString(3),
                displayName = rs.getString(4),
                displayValueText = rs.getString(5),
                displayFormat = rs.getString(6),
                displayDimension = rs.getString(7)
            )
        }

        return supportedVariables
    }

    fun spectraAcquisitionData(): Map<Int, SpectrumAcquisitionData> {
        require(connection != null && (connection?.isClosed == false)) { "Connection has to be open" }
        val statement = connection?.createStatement()
            ?: throw RuntimeException("Connection is not open, something failed with the database and this was not expected.")

        statement.queryTimeout = 30

        /* For now we only handle doubles of 5,7,8 */
        val spectraAcquisitionData = mutableMapOf<Int, MutableMap<Int, Double>>()

        val rs =
            statement.executeQuery(
                "SELECT Spectrum, Variable, Value FROM Variables ORDER BY Spectrum"
            )
        while (rs.next()) {
            val id = rs.getInt(1)
            if (!spectraAcquisitionData.containsKey(id)) spectraAcquisitionData[id] =
                mutableMapOf() // Allows for the !! later

            when (rs.getInt(2)) {
                5, 7, 8 -> spectraAcquisitionData[id]!![rs.getInt(2)] = rs.getDouble(3) // Safe we validated that before
            }
        }

        return spectraAcquisitionData.map { (id, values) ->
            id to SpectrumAcquisitionData(
                id = id,
                collisionEnergyAct = values[5],
                msmsIsolationmassAct = values[7],
                quadrupoleIsolationresolutionAct = values[8]
            )
        }.toMap()
    }

    fun acquisitionKeys(): Map<Int, AcquisitionKey> {
        require(connection != null && (connection?.isClosed == false)) { "Connection has to be open" }

        val statement = connection?.createStatement()
            ?: throw RuntimeException("Connection is not open, something failed with the database and this was not expected.")

        statement.queryTimeout = 30

        val acquisitionKeys = mutableMapOf<Int, AcquisitionKey>()

        val rs =
            statement.executeQuery(
                "SELECT Id, Polarity, ScanMode, AcquisitionMode, MsLevel FROM AcquisitionKeys"
            )

        while (rs.next()) {
            acquisitionKeys[rs.getInt(1)] = AcquisitionKey(
                id = rs.getInt(1),
                polarity = rs.getInt(2),
                scanMode = rs.getInt(3),
                acquisitionMode = rs.getInt(4),
                msLevel = rs.getInt(5)
            )
        }

        return acquisitionKeys
    }

    fun spectraDataAct(id: Int? = null, lineOnly: Boolean = true, func: (Spectrum) -> Unit) {
        require(connection != null && (connection?.isClosed == false)) { "Connection has to be open" }
        val statement = connection?.createStatement()
            ?: throw RuntimeException("Connection is not open, something failed with the database and this was not expected.")

        statement.queryTimeout = 30

        val acquisitionKeys = acquisitionKeys()
        val spectrumAcquisitionData = spectraAcquisitionData()

        val rs =
            statement.executeQuery(
                "SELECT Id, Rt, Segment, AcquisitionKey, Parent, MzAcqRangeLower, MzAcqRangeUpper, " +
                        "SumIntensity, MaxIntensity, TransformatorId," +
                        "ProfileMzId, ProfileIntensityId, " +
                        "LineIndexId, LineMzId, LineIntensityId, LineIndexWidthId, LinePeakAreaId, LineSnrId " +
                        "FROM Spectra" + if (id != null) " WHERE Id=$id" else ""
            )
        while (rs.next()) {
            val id = rs.getInt(1)
            val parentId = rs.getInt(5)

            func(
                Spectrum(
                    id,
                    rt = rs.getDouble(2),
                    segment = rs.getInt(3),
                    acquisitionKey = acquisitionKeys[rs.getInt(4)]
                        ?: throw RuntimeException("Invalid acquisition key for spectrum $id: ${rs.getInt(4)}"),
                    acquisitionData = spectrumAcquisitionData[id]
                        ?: throw RuntimeException("No acquisition data for spectrum $id"),
                    parent = parentId,
                    mzAcqRangeLower = rs.getInt(6),
                    mzAcqRangeUpper = rs.getInt(7),
                    sumIntensity = rs.getDouble(8),
                    maxIntensity = rs.getDouble(9),
                    transformatorId = rs.getInt(10), // We don't handle that yet
                    profileData = if (!lineOnly) generateProfileData(  // We get the profile data
                        rs.getLong(11),
                        rs.getLong(12)
                    ) else null,
                    lineData = generateLineData(
                        rs.getLong(13),
                        rs.getLong(14),
                        rs.getLong(15),
                        rs.getLong(16),
                        rs.getLong(17),
                        rs.getLong(18)
                    )
                )
            )
        }

    }

    private fun generateProfileData(mzId: Long?, intensityId: Long?): ProfileData? {
        if (mzId == null || intensityId == null) return null
        val localStorage =
            storage ?: throw RuntimeException("Storage is not accessible, something is wrong, did you open the file?")

        return ProfileData(
            c_baf2sql_read_double_array(localStorage, mzId)?.array
                ?: throw RuntimeException("Cannot read the MZ array. File is probably broken."),
            c_baf2sql_read_double_array(localStorage, intensityId)?.array
                ?: throw RuntimeException("Cannot read the Intensity Array. File is probably broken."),
        )
    }

    // for now we only handle mz, intensity and snr
    private fun generateLineData(
        indexId: Long?,
        mzId: Long?,
        intensityId: Long?,
        indexWidthId: Long?,
        peakAreaId: Long?,
        snrId: Long?
    ): LineData? {
        if (mzId == null || intensityId == null || snrId == null) return null
        val localStorage =
            storage ?: throw RuntimeException("Storage is not accessible, something is wrong, did you open the file?")
        val mzArray = c_baf2sql_read_double_array(localStorage, mzId)?.array
            ?: throw RuntimeException("Cannot read the MZ array. File is probably broken.")
        val intensityArray = c_baf2sql_read_double_array(localStorage, intensityId)?.array
            ?: throw RuntimeException("Cannot read the Intensity Array. File is probably broken.")

        val level = levelFilter
        val mzArrayProcessed: DoubleArray
        val intensityArrayProcessed: DoubleArray
        if (level == null) {
            mzArrayProcessed = mzArray
            intensityArrayProcessed = intensityArray
        } else {
            val indices = intensityArray.withIndex().filter { it.value > level }.map { it.index }
            mzArrayProcessed = indices.map { mzArray[it] }.toDoubleArray()
            intensityArrayProcessed = indices.map { intensityArray[it] }.toDoubleArray()
        }

        return LineData(
            mzArrayProcessed,
            intensityArrayProcessed,
        )
    }

    external fun c_baf2sql_get_sqlite_cache_filename(fileName: String): String
    external fun c_baf2sql_array_open_storage_calibrated(fileName: String): BinaryStorage
    external fun c_baf2sql_array_close_storage(storage: BinaryStorage): Int
    external fun c_baf2sql_get_last_error_string(): String
    external fun c_baf2sql_set_num_threads(threads: Int)
    external fun c_baf2sql_array_get_num_elements(handle: BinaryStorage, id: Long): NumElements
    external fun c_baf2sql_read_double_array(handle: BinaryStorage, id: Long): BAFDoubleArray?

    fun addLevelFilter(d: Double) {
        levelFilter = d
    }
}
