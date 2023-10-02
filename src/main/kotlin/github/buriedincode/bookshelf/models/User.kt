package github.buriedincode.bookshelf.models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import github.buriedincode.bookshelf.Utils.DATE_FORMATTER
import github.buriedincode.bookshelf.tables.ReadBookTable
import github.buriedincode.bookshelf.tables.UserTable
import github.buriedincode.bookshelf.tables.WishedTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDate

class User(id: EntityID<Long>) : LongEntity(id), Comparable<User> {
    companion object : LongEntityClass<User>(UserTable), Logging {
        val comparator = compareBy(User::username)
    }

    var imageUrl: String? by UserTable.imageUrlCol
    val readBooks by ReadBook referrersOn ReadBookTable.userCol
    var role: Short by UserTable.roleCol
    var username: String by UserTable.usernameCol
    var wishedBooks by Book via WishedTable

    fun toJson(showAll: Boolean = false): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "userId" to id.value,
            "imageUrl" to imageUrl,
            "role" to role,
            "username" to username,
        )
        if (showAll) {
            output["read"] = readBooks.sortedWith(
                compareBy<ReadBook> { it.readDate ?: LocalDate.of(2000, 1, 1) }
                    .thenBy { it.book },
            ).map {
                mapOf(
                    "book" to it.book.toJson(),
                    "readDate" to it.readDate?.format(DATE_FORMATTER),
                )
            }
            output["wished"] = wishedBooks.sorted().map { it.toJson() }
        }
        return output.toSortedMap()
    }

    override fun compareTo(other: User): Int = comparator.compare(this, other)
}

data class UserInput(
    val imageUrl: String? = null,
    val readBooks: List<UserReadInput> = ArrayList(),
    val role: Short = 0,
    val username: String,
    val wishedBookIds: List<Long> = ArrayList(),
)

data class UserReadInput(
    val bookId: Long,
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val readDate: LocalDate? = LocalDate.now(),
)
