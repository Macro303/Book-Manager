package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookCreatorRoleTable
import github.buriedincode.bookshelf.tables.CreatorTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Creator(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Creator>(CreatorTable), Logging

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
            output["credits"] = credits.map {
                mapOf(
                    "bookId" to it.book.id.value,
                    "roleId" to it.role.id.value
                )
            }
        }
        return output.toSortedMap()
    }
}

class CreatorInput(
    val imageUrl: String? = null,
    val name: String
)