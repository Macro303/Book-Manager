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

    var imageUrl: String? by UserTable.imageUrlCol
    var readBooks by Book via ReadTable
    var role: Short by UserTable.roleCol
    var username: String by UserTable.usernameCol
    var wishedBooks by Book via WishedTable

    fun toJson(showAll: Boolean = false): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "userId" to id.value,
            "imageUrl" to imageUrl,
            "role" to role,
            "username" to username
        )
        if (showAll) {
            output["read"] = readBooks.map { it.toJson() }
            output["wished"] = wishedBooks.map { it.toJson() }
        }
        return output.toSortedMap()
    }
}

class UserInput(
    val imageUrl: String? = null,
    val readBookIds: List<Long> = ArrayList(),
    val role: Short = 0,
    val username: String,
    val wishedBookIds: List<Long> = ArrayList()
)
