package github.buriedincode.bookshelf.models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import github.buriedincode.bookshelf.Utils.toString
import github.buriedincode.bookshelf.tables.ReadBookTable
import github.buriedincode.bookshelf.tables.UserTable
import github.buriedincode.bookshelf.tables.WishedTable
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class User(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<User> {
    companion object : LongEntityClass<User>(UserTable) {
        val comparator = compareBy(User::username)

        fun find(username: String): User? {
            return User.find { UserTable.usernameCol eq username }.firstOrNull()
        }

        fun findOrCreate(username: String): User {
            return find(username) ?: User.new {
                this.username = username
            }
        }
    }

    var imageUrl: String? by UserTable.imageUrlCol
    val readBooks by ReadBook referrersOn ReadBookTable.userCol
    var username: String by UserTable.usernameCol
    var wishedBooks by Book via WishedTable

    override fun toJson(showAll: Boolean): Map<String, Any?> {
        return mutableMapOf<String, Any?>(
            "id" to id.value,
            "imageUrl" to imageUrl,
            "username" to username,
        ).apply {
            if (showAll) {
                put("read", readBooks.groupBy({ it.book.id.value }, { it.readDate?.toString("yyyy-MM-dd") }))
                put("wished", wishedBooks.sorted().map { it.id.value })
            }
        }.toSortedMap()
    }

    override fun compareTo(other: User): Int = comparator.compare(this, other)
}

data class UserInput(
    val readBooks: List<ReadBook> = emptyList(),
    val imageUrl: String? = null,
    val username: String,
    val wishedBooks: List<Long> = emptyList(),
) {
    data class ReadBook(
        val book: Long,
        @JsonDeserialize(using = LocalDateDeserializer::class)
        val readDate: LocalDate? = null,
    )
}
