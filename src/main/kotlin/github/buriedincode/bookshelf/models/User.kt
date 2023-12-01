package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.Utils.toString
import github.buriedincode.bookshelf.tables.ReadBookTable
import github.buriedincode.bookshelf.tables.UserTable
import github.buriedincode.bookshelf.tables.WishedTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDate

class User(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<User> {
    companion object : LongEntityClass<User>(UserTable), Logging {
        val comparator = compareBy(User::username)
    }

    var image: String? by UserTable.imageCol
    val readBooks by ReadBook referrersOn ReadBookTable.userCol
    var role: UserRole by UserTable.roleCol
    var username: String by UserTable.usernameCol
    var wishedBooks by Book via WishedTable

    override fun toJson(showAll: Boolean): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "id" to id.value,
            "role" to role.name,
            "username" to username,
        )
        output["image"] = image?.let {
            if (it.startsWith("http")) it else "/uploads/users/$it"
        }
        if (showAll) {
            output["read"] = readBooks.sortedWith(
                compareBy<ReadBook> { it.readDate ?: LocalDate.of(2000, 1, 1) }.thenBy { it.book },
            ).map {
                mapOf(
                    "book" to it.book.toJson(),
                    "readDate" to it.readDate?.toString("yyyy-MM-dd"),
                )
            }
            output["wished"] = wishedBooks.sorted().map { it.toJson() }
        }
        return output.toSortedMap()
    }

    override fun compareTo(other: User): Int = comparator.compare(this, other)
}
