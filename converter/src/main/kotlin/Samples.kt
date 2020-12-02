import net.nprod.baf2mzml.XMLsafe

class SamplesBuilder {
    val content = mutableListOf<Sample>()
    private var counter = 0
    operator fun Sample.unaryPlus() {
        val sample = if (this.id == null) {
            this.copy(id = "sample $counter").also { counter++ }
        } else {
            this
        }
        content.add(sample)
    }
}

data class Sample(
    val id: String? = null,
    val name: String
) {
    fun toXML(): String =
        """<sample id="${XMLsafe(id)}" name="${XMLsafe(name)}"></sample>"""
}