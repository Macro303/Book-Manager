package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.CreditTable
import github.buriedincode.bookshelf.tables.RoleTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Role(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<Role> {
    companion object : LongEntityClass<Role>(RoleTable), Logging {
        val comparator = compareBy(Role::title)
    }

    val credits by Credit referrersOn CreditTable.roleCol
    var summary: String? by RoleTable.summaryCol
    var title: String by RoleTable.titleCol

    override fun toJson(showAll: Boolean): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "id" to id.value,
            "summary" to summary,
            "title" to title,
        )
        if (showAll) {
            output["credits"] = credits.sortedWith(
                compareBy<Credit> { it.book }.thenBy { it.creator },
            ).map {
                mapOf(
                    "book" to it.book.toJson(),
                    "creator" to it.creator.toJson(),
                )
            }
        }
        return output.toSortedMap()
    }

    override fun compareTo(other: Role): Int = comparator.compare(this, other)
}

data class RoleInput(
    val credits: List<Credit> = ArrayList(),
    val summary: String? = null,
    val title: String,
) {
    data class Credit(
        val bookId: Long,
        val creatorId: Long,
    )
}
