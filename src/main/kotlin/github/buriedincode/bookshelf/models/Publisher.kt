package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookTable
import github.buriedincode.bookshelf.tables.PublisherTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Publisher(id: EntityID<Long>) : LongEntity(id), Comparable<Publisher> {
    companion object : LongEntityClass<Publisher>(PublisherTable), Logging {
        val comparator = compareBy(Publisher::title)
    }

    var title: String by PublisherTable.titleCol

    val books by Book optionalReferrersOn BookTable.publisherCol

    fun toJson(showAll: Boolean = false): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "publisherId" to id.value,
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
    val title: String,
)
