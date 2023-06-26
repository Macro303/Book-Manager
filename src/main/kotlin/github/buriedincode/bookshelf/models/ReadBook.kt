package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.ReadBookTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDate

class ReadBook(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ReadBook>(ReadBookTable), Logging

    var book: Book by Book referencedOn ReadBookTable.bookCol
    var user: User by User referencedOn ReadBookTable.userCol
    var readDate: LocalDate? by ReadBookTable.readDateCol
}
