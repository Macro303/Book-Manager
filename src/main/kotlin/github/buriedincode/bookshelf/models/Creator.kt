package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookCreatorRoleTable
import github.buriedincode.bookshelf.tables.CreatorTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Creator(id: EntityID<Long>) : LongEntity(id), Comparable<Creator> {
    companion object : LongEntityClass<Creator>(CreatorTable), Logging {
        val comparator = compareBy(Creator::name)
    }

    val credits by BookCreatorRole referrersOn BookCreatorRoleTable.creatorCol
    var imageUrl: String? by CreatorTable.imageUrlCol
    var name: String by CreatorTable.nameCol

    fun toJson(showAll: Boolean = false): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "creatorId" to id.value,
            "imageUrl" to imageUrl,
            "name" to name,
        )
        if (showAll) {
            output["credits"] = credits.sortedWith(compareBy<BookCreatorRole> { it.book }.thenBy { it.role }).map {
                mapOf(
                    "book" to it.book.toJson(),
                    "role" to it.role.toJson(),
                )
            }
        }
        return output.toSortedMap()
    }

    override fun compareTo(other: Creator): Int = comparator.compare(this, other)
}

data class CreatorInput(
    val credits: List<CreatorCreditInput> = ArrayList(),
    val imageUrl: String? = null,
    val name: String,
)

data class CreatorCreditInput(
    val bookId: Long,
    val roleId: Long,
)
