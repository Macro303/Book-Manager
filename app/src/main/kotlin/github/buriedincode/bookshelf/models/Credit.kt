package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.CreditTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Credit(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Credit>(CreditTable), Logging

    var book: Book by Book referencedOn CreditTable.bookCol
    var creator: Creator by Creator referencedOn CreditTable.creatorCol
    var role: Role by Role referencedOn CreditTable.roleCol
}
