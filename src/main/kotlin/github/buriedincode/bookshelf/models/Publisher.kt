package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookTable
import github.buriedincode.bookshelf.tables.PublisherTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Publisher(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Publisher>(PublisherTable), Logging

    var title: String by PublisherTable.titleCol

    val books by Book optionalReferrersOn BookTable.publisherCol

    fun toJson(showAll: Boolean = false): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "publisherId" to id.value,
            "title" to title
        )
        if (showAll)
            output["books"] = books.map { it.toJson() }
        return output.toSortedMap()
    }
}

class PublisherInput(
    val title: String
)
