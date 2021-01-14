package net.nprod.baf2mzml/*
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

import net.nprod.baf2mzml.XMLsafe

class ParamBuilder {
    val params = mutableListOf<Param>()
    operator fun Param.unaryPlus() {
        params.add(this)
    }
}

/**
 * This is a CV parameter.
 * It is pretty close to what MzML can handle, we have the most used ones as companion objects.
 */
data class Param(
    val cvRef: String,
    val accession: String,
    val name: String,
    val value: String,
    val unitCvRef: String? = null,
    val unitAccession: String? = null,
    val unitName: String? = null
) {
    fun toXML(): String {
        var out =
            """<cvParam cvRef="${XMLsafe(cvRef)}" accession="${XMLsafe(accession)}" name="${XMLsafe(name)}" value="${
                XMLsafe(
                    value
                )
            }""""
        unitCvRef?.let { out += """ unitCvRef="${XMLsafe(it)}" """ }
        unitAccession?.let { out += """ unitAccession="${XMLsafe(it)}" """ }
        unitName?.let { out += """ unitName="${XMLsafe(it)}" """ }
        out += "/>"
        return out
    }

    companion object {
        val MS1: Param = Param("MS", "MS:1000579", "MS1 spectrum", "")
        val MSn: Param = Param("MS", "MS:1000580", "MSn spectrum", "")
        val POSITIVE: Param = Param("MS", "MS:1000130", "positive scan", "")
        val noCombination: Param = Param("MS", "MS:1000795", "no combination", "")
        fun lowMass(number: Number) =
            Param(
                "MS",
                "MS:1000528",
                "lowest observed m/z",
                number.toString(),
                unitCvRef = "MS",
                unitAccession = "MS:1000040",
                unitName = "m/z"
            )

        fun highMass(number: Number) =
            Param(
                "MS",
                "MS:1000527",
                "highest observed m/z",
                number.toString(),
                unitCvRef = "MS",
                unitAccession = "MS:1000040",
                unitName = "m/z"
            )

        fun basePeak(number: Number) =
            Param(
                "MS",
                "MS:1000504",
                "base peak m/z",
                number.toString(), unitCvRef = "MS",
                unitAccession = "MS:1000040",
                unitName = "m/z"
            )

        fun basePeakIntensity(number: Number) =
            Param(
                "MS",
                "MS:1000505",
                "base peak intensity",
                number.toString(),
                unitCvRef = "MS",
                unitAccession = "MS:1000131",
                unitName = "number of counts"
            )

        fun tic(number: Number) =
            Param("MS", "MS:1000285", "total ion current", number.toInt().toString())

        fun msLevel(level: Int) =
            Param("MS", "MS:1000511", "ms level", level.toString())

        fun centroid() =
            Param("MS", "MS:1000127", "centroid spectrum", "")
    }
}
