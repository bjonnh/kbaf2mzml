package net.nprod.baf2mzml.baf

import net.nprod.baf2mzml.BAF2SQL
import net.nprod.baf2mzml.BinaryStorage
import net.nprod.baf2mzml.exceptions.FileFormatException
import net.nprod.baf2mzml.mzml.MzMLWriter
import net.nprod.baf2mzml.schema.AcquisitionKey
import net.nprod.baf2mzml.schema.LineData
import net.nprod.baf2mzml.schema.ProfileData
import net.nprod.baf2mzml.schema.Spectrum
import net.nprod.baf2mzml.schema.SpectrumAcquisitionData
import net.nprod.baf2mzml.schema.SupportedVariable
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * Handle BAF files
 * this assumes you have enough memory, we are not going to stream from disk
 *
 * @param filename Path of the file
 */
class BAF2SQLFile(val filename: String) {
    private var levelFilter: Double? = null
    private var sqliteDb: String? = null
    private var storage: BinaryStorage? = null
    private var connection: Connection? = null

    private val sqliteQueryTimeout: Duration = 30.seconds

    /**
     * Show the last error from the C library
     */
    val lasterror: String
        get() = BAF2SQL.c_baf2sql_get_last_error_string()

    init {
        sqliteDb = BAF2SQL.c_baf2sql_get_sqlite_cache_filename(filename)
        storage = BAF2SQL.c_baf2sql_array_open_storage_calibrated(filename)
        connection = DriverManager.getConnection("jdbc:sqlite:$sqliteDb")
    }

    /**
     * Close
     */
    fun close() {
        storage?.let { BAF2SQL.c_baf2sql_array_close_storage(it) }
        connection?.close()
    }

    /**
     * Extract the supported variables as a Map
     */
    fun supportedVariables(): Map<Int, SupportedVariable> {
        require(connection != null && (connection?.isClosed == false)) { "Connection has to be open" }

        val statement = connection?.createStatement()
            ?: throw connectionError()

        statement.queryTimeout = sqliteQueryTimeout.toInt(TimeUnit.SECONDS)

        val supportedVariables = mutableMapOf<Int, SupportedVariable>()

        val rs =
            statement.executeQuery(
                "SELECT Variable, PermanentName, Type, DisplayGroupName," +
                    " DisplayValueText, DisplayFormat, DisplayDimension FROM SupportedVariables"
            )

        @Suppress("MagicNumber")
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

    private fun connectionError() = IllegalStateException(
        "Connection is not open, something failed with the database " +
            "and this was not expected, this may be due to a faulty file."
    )

    fun spectraAcquisitionData(): Map<Int, SpectrumAcquisitionData> {
        require(connection != null && (connection?.isClosed == false)) { "Connection has to be open" }
        val statement = connection?.createStatement()
            ?: throw connectionError()

        statement.queryTimeout = sqliteQueryTimeout.toInt(TimeUnit.SECONDS)

        /* For now we only handle doubles of 5,7,8 */
        val spectraAcquisitionData = mutableMapOf<Int, MutableMap<Int, Double>>()

        val rs =
            statement.executeQuery(
                "SELECT Spectrum, Variable, Value FROM Variables ORDER BY Spectrum"
            )

        @Suppress("MagicNumber")
        while (rs.next()) {
            val id = rs.getInt(1)
            if (!spectraAcquisitionData.containsKey(id)) spectraAcquisitionData[id] =
                mutableMapOf() // Allows for the !! later

            when (rs.getInt(2)) {
                5, 7, 8 -> spectraAcquisitionData[id]!![rs.getInt(2)] = rs.getDouble(3) // Safe we validated that before
            }
        }

        @Suppress("MagicNumber")
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
            ?: throw connectionError()

        statement.queryTimeout = sqliteQueryTimeout.toInt(TimeUnit.SECONDS)

        val acquisitionKeys = mutableMapOf<Int, AcquisitionKey>()

        val rs =
            statement.executeQuery(
                "SELECT Id, Polarity, ScanMode, AcquisitionMode, MsLevel FROM AcquisitionKeys"
            )

        @Suppress("MagicNumber")
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

    @Suppress("ThrowsCount")
    fun spectraDataAct(id: Int? = null, lineOnly: Boolean = true, func: (Spectrum) -> Unit) {
        require(connection != null && (connection?.isClosed == false)) { "Connection has to be open" }
        val statement = connection?.createStatement()
            ?: throw connectionError()

        statement.queryTimeout = sqliteQueryTimeout.toInt(TimeUnit.SECONDS)

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

        @Suppress("MagicNumber")
        while (rs.next()) {
            val spectrumId = rs.getInt(1)
            val parentId = rs.getInt(5)

            func(
                Spectrum(
                    spectrumId,
                    rt = rs.getDouble(2),
                    segment = rs.getInt(3),
                    acquisitionKey = acquisitionKeys[rs.getInt(4)]
                        ?: throw IllegalArgumentException(
                            "Invalid acquisition key for spectrum $spectrumId: ${rs.getInt(4)}"
                        ),
                    acquisitionData = spectrumAcquisitionData[spectrumId]
                        ?: throw IllegalArgumentException("No acquisition data for spectrum $spectrumId"),
                    parent = parentId,
                    mzAcqRangeLower = rs.getInt(6),
                    mzAcqRangeUpper = rs.getInt(7),
                    sumIntensity = rs.getDouble(8),
                    maxIntensity = rs.getDouble(9),
                    transformatorId = rs.getInt(10), // We don't handle that yet
                    profileData = if (!lineOnly) generateProfileData( // We get the profile data
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

    @Suppress("ThrowsCount")
    private fun generateProfileData(mzId: Long?, intensityId: Long?): ProfileData? {
        if (mzId == null || intensityId == null) return null
        val localStorage =
            storage
                ?: throw FileFormatException("Storage is not accessible, something is wrong, did you open the file?")

        return ProfileData(
            BAF2SQL.c_baf2sql_read_double_array(localStorage, mzId)?.array
                ?: throw FileFormatException("Cannot read the MZ array. File is probably broken."),
            BAF2SQL.c_baf2sql_read_double_array(localStorage, intensityId)?.array
                ?: throw FileFormatException("Cannot read the Intensity Array. File is probably broken."),
        )
    }

    // for now we only handle mz, intensity and snr
    @Suppress("LongParameterList", "ThrowsCount", "UnusedPrivateMember")
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
            storage
                ?: throw FileFormatException("Storage is not accessible, something is wrong, did you open the file?")
        val mzArray = BAF2SQL.c_baf2sql_read_double_array(localStorage, mzId)?.array
            ?: throw FileFormatException("Cannot read the MZ array. File is probably broken.")
        val intensityArray = BAF2SQL.c_baf2sql_read_double_array(localStorage, intensityId)?.array
            ?: throw FileFormatException("Cannot read the Intensity Array. File is probably broken.")

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

    /**
     * Set the minimum value for any signal
     *
     * @param d Minimal value
     */
    fun setLevelFilter(d: Double) {
        levelFilter = d
    }

    fun saveAsMzMl(filename: String) {
        val writer = MzMLWriter(File(filename), "Test", this)
        writer.execute()
    }
}
