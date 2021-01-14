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

import org.apache.commons.text.StringEscapeUtils
import java.io.File
import java.io.OutputStream

fun BAF2SQL.saveAsMzMl(filename: String) {
    val writer = MzMLWriter(File(filename), "Test", this)
    writer.execute()
}

fun XMLsafe(s: String?): String {
    if (s == null) throw RuntimeException("Impossible to make a null string safe.")
    return StringEscapeUtils.ESCAPE_XML11.translate(s)
}

data class Offset(
    val id: String,
    val offset: Int
)

class OffsetableOutput(
    val bytes: ByteArray,
    val offsets: List<Offset>
)

class MzMLFile(val stream: OutputStream) {
    var position = 0
    val offsetStore = mutableListOf<Offset>()

    fun writeln(content: String) {
        write(content + "\n")
    }

    fun write(content: String) {
        val byteArray = content.encodeToByteArray()
        write(byteArray)
    }

    fun write(content: ByteArray) {
        position += content.size
        stream.write(content)
    }

    fun addDeclaration() {
        writeln("""<?xml version="1.0" encoding="ISO-8859-1"?>""")
    }

    fun content(f: MzMLFile.() -> Unit) {
        writeln("""<indexedmzML xmlns="http://psi.hupo.org/ms/mzml" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://psi.hupo.org/ms/mzml http://psidev.info/files/ms/mzML/xsd/mzML1.1.0_idx.xsd">""")
        writeln("""<mzML xmlns="http://psi.hupo.org/ms/mzml" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://psi.hupo.org/ms/mzml http://psidev.info/files/ms/mzML/xsd/mzML1.1.0.xsd" id="urn:lsid:net.nprod:mzML.instanceDocuments.iwillnotcomplainthatmyfileisbroken" version="1.1.0">""")
        writeln(
            """
            <cvList count="2">
                <cv id="MS" fullName="Proteomics Standards Initiative Mass Spectrometry Ontology" version="2.26.0" URI="http://psidev.cvs.sourceforge.net/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo"/>
                <cv id="UO" fullName="Unit Ontology" version="14:07:2009" URI="http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/phenotype/unit.obo"/>
            </cvList>
        """.trimIndent()
        )
        writeln("<fileDescription></fileDescription>")
        this.apply(f)

        writeIndex()
        writeln("</mzML>")
        writeln("</indexedmzML>")
    }

    fun referenceableParamsGroupList(f: ReferenceableParamGroupList.() -> Unit) { // TODO!!!
        val groups = ReferenceableParamGroupList()
        groups.apply(f)
        writeln("""<referenceableParamGroupList count="${groups.referenceableParamGroups.size}">""")
        groups.referenceableParamGroups.forEach { (name, entries) ->
            writeln(
                """<referenceableParamGroup id="$name">"""
            )
            entries.forEach {
                writeln(it.toXML())
            }
            writeln("""</referenceableParamGroup>""")
        }
        writeln("""</referenceableParamGroupList>""")
    }

    fun samples(f: SamplesBuilder.() -> Unit) {
        val samples = SamplesBuilder()
        samples.apply(f)
        writeln("""<sampleList count="${samples.list.size}">""")
        samples.list.forEach { sample ->
            writeln(sample.toXML())
        }
        writeln("""</sampleList>""")
    }

    fun runs(f: RunsBuilder.() -> Unit) {
        val runs = RunsBuilder()
        runs.apply(f)
        runs.list.forEach {
            it.writeToFile(this)
        }
    }

    fun writeIndex() {
        writeln("""<indexList count="1">""") // Only spectra for now
        writeln("""  <index name="spectrum">""")
        offsetStore.map {
            writeln("""    <offset idRef="${it.id}">${it.offset}</offset>""")
        }
        writeln("""  </index>""")
        writeln("""</indexList>""")
    }

    fun addOffset(id: String) {
        offsetStore.add(Offset(id, position + 1)) // Why is there a off by one here?
    }

    fun write(param: Param) {
        writeln(param.toXML())
    }
}


class ReferenceableParamGroupList {
    val referenceableParamGroups = mutableMapOf<String, List<Param>>()
    fun group(groupName: String, f: ParamBuilder.() -> Unit) {
        val paramBuilder = ParamBuilder()
        paramBuilder.apply(f)

        referenceableParamGroups[XMLsafe(groupName)] = paramBuilder.params
    }
}

/**
 * Start the DSL for creating that file
 */
fun mzMLfile(stream: OutputStream, f: MzMLFile.() -> Unit) {
    val file = MzMLFile(stream)
    file.apply(f)
    stream.close()
}


