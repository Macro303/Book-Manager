package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookCreatorRoleTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class BookCreatorRole(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BookCreatorRole>(BookCreatorRoleTable), Logging

    var book: Book by Book referencedOn BookCreatorRoleTable.bookCol
    var creator: Creator by Creator referencedOn BookCreatorRoleTable.creatorCol
    var role: Role by Role referencedOn BookCreatorRoleTable.roleCol
}
