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

package net.nprod.baf2mzml.helpers

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

/**
 * Convert a DoubleArray to a Base64 encoded byte array
 */
@Suppress("MagicNumber")
fun base64ArrayEncoder(data: DoubleArray): ByteArray {
    val encodedData: ByteArray
    val buffer = ByteBuffer.allocate(data.size * 8)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    for (d in data) {
        buffer.putDouble(d)
    }
    encodedData = buffer.array()
    return Base64.getEncoder().encode(encodedData)
}

/**
 * Convert a FloatArray to a Base64 encoded byte array
 */
@Suppress("MagicNumber")
fun base64ArrayEncoder(data: FloatArray): ByteArray {
    val encodedData: ByteArray
    val buffer = ByteBuffer.allocate(data.size * 4)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    for (d in data) {
        buffer.putFloat(d)
    }
    encodedData = buffer.array()
    return Base64.getEncoder().encode(encodedData)
}
