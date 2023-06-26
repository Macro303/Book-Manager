package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookCreatorRoleTable
import github.buriedincode.bookshelf.tables.RoleTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Role(id: EntityID<Long>) : LongEntity(id), Comparable<Role> {
    companion object : LongEntityClass<Role>(RoleTable), Logging {
        val comparator = compareBy(Role::title)
    }

    val credits by BookCreatorRole referrersOn BookCreatorRoleTable.roleCol
    var title: String by RoleTable.titleCol

    fun toJson(showAll: Boolean = false): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "roleId" to id.value,
            "title" to title
        )
        if (showAll)
            output["credits"] = credits.sortedWith(compareBy<BookCreatorRole> { it.book }.thenBy { it.creator }).map {
                mapOf(
                    "book" to it.book.toJson(),
                    "creator" to it.creator.toJson()
                )
            }
        return output.toSortedMap()
    }

    override fun compareTo(other: Role): Int = comparator.compare(this, other)
}

data class RoleInput(
    val credits: List<RoleCreditInput> = ArrayList(),
    val title: String
)

data class RoleCreditInput(
    val bookId: Long,
    val creatorId: Long
)
