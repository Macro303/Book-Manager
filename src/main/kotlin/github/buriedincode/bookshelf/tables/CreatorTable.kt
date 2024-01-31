package github.buriedincode.bookshelf.tables

import github.buriedincode.bookshelf.Utils
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils

object CreatorTable : LongIdTable(name = "creators"), Logging {
    val imageUrlCol: Column<String?> = text(name = "image").nullable()
    val nameCol: Column<String> = text(name = "name").uniqueIndex()
    val summaryCol: Column<String?> = text(name = "summary").nullable()

    init {
        Utils.query {
            SchemaUtils.create(this)
        }
    }
}
