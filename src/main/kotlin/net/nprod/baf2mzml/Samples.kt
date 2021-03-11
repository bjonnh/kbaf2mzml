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

/**
 * DSL class to construct samples.
 */
class SamplesBuilder {
    private val content = mutableListOf<Sample>()

    /**
     * List of samples
     */
    val list: List<Sample>
        get() = content

    private var counter = 0

    /**
     * Add a sample using the +Sample(…) syntax
     */
    operator fun Sample.unaryPlus() {
        val sample = if (this.id == null) {
            this.copy(id = "sample $counter").also { counter++ }
        } else {
            this
        }
        content.add(sample)
    }
}

/**
 * Describe a sample, we currently only handle a single sample by file.
 */
data class Sample(
    /**
     * Identifier for the sample, we allow it to be null at declaration so the SamplesBuilder can create an id
     * automatically. However, if you try to use it outside of the SamplesBuilder and it is null at export time
     * it will fail.
     */
    val id: String? = null,
    /**
     * Name of the sample
     */
    val name: String
) {
    /**
     * Convert this sample to a XML string.
     */
    fun toXML(): String =
        """<sample id="${XMLsafe(id)}" name="${XMLsafe(name)}"></sample>"""
}
