package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookTable
import github.buriedincode.bookshelf.tables.PublisherTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Publisher(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<Publisher> {
    companion object : LongEntityClass<Publisher>(PublisherTable), Logging {
        val comparator = compareBy(Publisher::title)
    }

    var imageUrl: String? by PublisherTable.imageUrlCol
    var summary: String? by PublisherTable.summaryCol
    var title: String by PublisherTable.titleCol

    val books by Book optionalReferrersOn BookTable.publisherCol

    override fun toJson(showAll: Boolean): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "id" to id.value,
            "imageUrl" to imageUrl,
            "summary" to summary,
            "title" to title,
        )
        if (showAll) {
            output["books"] = books.sorted().map { it.toJson() }
        }
        return output.toSortedMap()
    }

    override fun compareTo(other: Publisher): Int = comparator.compare(this, other)
}

data class PublisherInput(
    val imageUrl: String? = null,
    val summary: String? = null,
    val title: String,
)
