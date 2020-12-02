package net.nprod.baf2mzml

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


fun base64ArrayEncoder(data: DoubleArray): ByteArray {
    var encodedData: ByteArray? = null
    val buffer = ByteBuffer.allocate(data.size * 8)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    for (d in data) {
        buffer.putDouble(d)
    }
    encodedData = buffer.array()
    return Base64.getEncoder().encode(encodedData)
}

fun base64ArrayEncoder(data: FloatArray): ByteArray {
    var encodedData: ByteArray? = null
    val buffer = ByteBuffer.allocate(data.size * 4)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    for (d in data) {
        buffer.putFloat(d)
    }
    encodedData = buffer.array()
    return Base64.getEncoder().encode(encodedData)
}