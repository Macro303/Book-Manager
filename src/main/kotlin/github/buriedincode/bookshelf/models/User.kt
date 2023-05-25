package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.ReadTable
import github.buriedincode.bookshelf.tables.UserTable
import github.buriedincode.bookshelf.tables.WishedTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class User(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<User>(UserTable), Logging

    var read_books by Book via ReadTable
    var wished_books by Book via WishedTable
    var username: String by UserTable.usernameCol
    var role: Short by UserTable.roleCol

    fun toJson(showAll: Boolean = false): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "userId" to id.value,
            "username" to username
        )
        if (showAll) {
            output["read"] = read_books.map { it.toJson() }
            output["wished"] = wished_books.map { it.toJson() }
        }
        return output.toSortedMap()
    }
}

class UserInput(
    val username: String,
    val readBookIds: List<Long> = ArrayList(),
    val wishedBookIds: List<Long> = ArrayList()
)
