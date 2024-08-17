package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.ReadBookTable
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ReadBook(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ReadBook>(ReadBookTable) {
        fun find(book: Book, user: User): ReadBook? {
            return ReadBook.find { (ReadBookTable.bookCol eq book) and (ReadBookTable.userCol eq user) }.firstOrNull()
        }

        fun findOrCreate(book: Book, user: User): ReadBook {
            return find(book, user) ?: ReadBook.new {
                this.book = book
                this.user = user
            }
        }
    }

    var book: Book by Book referencedOn ReadBookTable.bookCol
    var user: User by User referencedOn ReadBookTable.userCol
    var readDate: LocalDate? by ReadBookTable.readDateCol
}
