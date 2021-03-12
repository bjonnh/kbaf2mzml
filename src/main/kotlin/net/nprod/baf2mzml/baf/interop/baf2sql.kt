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

import java.io.File

typealias BinaryStorage = Long

class BAFDoubleArray(var ret: Int, var length: Long, var array: DoubleArray)

class NumElements(var ret: Int, var value: Long) {

    override fun toString(): String {
        return ("Return: ${this.ret} Value: ${this.value}")
    }
}

/**
 * The object that handles the interop with the BAF2SQL C library
 */
@Suppress("FunctionNaming")
object BAF2SQL {
    /**
     * For each EXTRA_CORE_FACTOR cores, add one thread
     */
    private const val EXTRA_CORE_FACTOR: Int = 4

    init {
        val map = listOf(
            File(System.getProperty("sun.boot.library.path")).parent,
            System.getProperty("user.dir"),
            System.getenv("APP_HOME")
        ).map { homeFile ->
            val libFile = File(homeFile, "lib")
            try {
                if (System.getProperty("os.name") == "Linux") {

                    System.load(File(libFile, "libbaf2sql_c.so").absolutePath)
                    System.load(File(libFile, "libbaf2sql_adapter.so").absolutePath)
                } else {
                    System.load(File(libFile, "baf2sql_c.dll").absolutePath)
                    System.load(File(libFile, "baf2sql_adapter.dll").absolutePath)
                }
            } catch (e: UnsatisfiedLinkError) {
                println("Couldn't find library in $libFile: ${e.localizedMessage}")
            }
        }

        c_baf2sql_set_num_threads(1 + Runtime.getRuntime().availableProcessors() / EXTRA_CORE_FACTOR)
    }

    external fun c_baf2sql_get_sqlite_cache_filename(fileName: String): String
    external fun c_baf2sql_array_open_storage_calibrated(fileName: String): BinaryStorage
    external fun c_baf2sql_array_close_storage(storage: BinaryStorage): Int
    external fun c_baf2sql_get_last_error_string(): String
    external fun c_baf2sql_set_num_threads(threads: Int)
    external fun c_baf2sql_array_get_num_elements(handle: BinaryStorage, id: Long): NumElements
    external fun c_baf2sql_read_double_array(handle: BinaryStorage, id: Long): BAFDoubleArray?
}
