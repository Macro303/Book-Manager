package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookCreatorRoleTable
import github.buriedincode.bookshelf.tables.RoleTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Role(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Role>(RoleTable), Logging

    val bookCreators by BookCreatorRole referrersOn BookCreatorRoleTable.roleCol
    var title: String by RoleTable.titleCol

    fun toJson(showAll: Boolean = false): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "roleId" to id.value,
            "title" to title
        )
        if (showAll)
            output["bookCreators"] = bookCreators.map {
                mapOf(
                    "bookId" to it.book.id.value,
                    "creatorId" to it.creator.id.value
                )
            }
        return output.toSortedMap()
    }
}

class RoleInput(
    val title: String
)
