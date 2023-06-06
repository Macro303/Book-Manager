package github.buriedincode.bookshelf.services.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class Contributor(
    val name: String,
    val role: String
)

data class Identifiers(
    val goodreads: List<String> = ArrayList(),
    val google: List<String> = ArrayList(),
    val librarything: List<String> = ArrayList()
)

data class Edition(
    val contributors: List<Contributor> = ArrayList(),
    val description: String? = null,
    @JsonProperty("physical_format")
    val format: String? = null,
    val genres: List<String> = ArrayList(),
    val identifiers: Identifiers = Identifiers(),
    val key: String,
    @JsonProperty("publish_date")
    val publishDate: LocalDate? = null,
    val publishers: List<String> = ArrayList(),
    val subtitle: String? = null,
    val title: String,
    val works: List<Resource> = ArrayList(),
) {
    val editionId: String
        get() = key.split("/").last()
}
