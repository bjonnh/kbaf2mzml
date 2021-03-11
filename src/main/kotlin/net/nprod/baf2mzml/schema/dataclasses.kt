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

package net.nprod.baf2mzml.schema

/**
 * This is following the scheme of the database
 */
data class SupportedVariable(
    val id: Int,
    val permanentName: String,
    val displayGroupName: String,
    val displayName: String,
    val displayValueText: String,
    val displayFormat: String,
    val displayDimension: String
)

/**
 * These are the variables we are currently supporting
 */
typealias Collision_Energy_Act = Double // id 5, unit eV
typealias MSMS_IsolationMass_Act = Double // id 7, m/z
typealias Quadrupole_IsolationResolution_Act = Double // id 8, m/z

/**
 * This is a combined data class making it more convenient to work with the spectral acquisition parameters
 */
data class SpectrumAcquisitionData(
    val id: Int,
    val collisionEnergyAct: Collision_Energy_Act?,
    val msmsIsolationmassAct: MSMS_IsolationMass_Act?,
    val quadrupoleIsolationresolutionAct: Quadrupole_IsolationResolution_Act?
)

data class AcquisitionKey(
    val id: Int,
    val polarity: Int,
    val scanMode: Int,
    val acquisitionMode: Int,
    val msLevel: Int
)

data class ProfileData(
    val mz: DoubleArray,
    val intensity: DoubleArray
)

data class LineData(
    // val index: Array<Int>,
    val mz: DoubleArray,
    val intensity: DoubleArray,
    // val indexWidth: DoubleArray,
    // val peakArea: DoubleArray,
    // val snr: DoubleArray
)

/**
 * This is huge as we are directly loading data. If really it is a bottleneck in some applications, this will
 * have to be rewritten as a lazy loader.
 */
data class Spectrum(
    val id: Int,
    val rt: Double,
    val acquisitionKey: AcquisitionKey,
    val acquisitionData: SpectrumAcquisitionData,
    val segment: Int,
    val parent: Int,
    val mzAcqRangeLower: Int,
    val mzAcqRangeUpper: Int,
    val sumIntensity: Double,
    val maxIntensity: Double,
    val transformatorId: Int,
    val profileData: ProfileData?,
    val lineData: LineData?
)
