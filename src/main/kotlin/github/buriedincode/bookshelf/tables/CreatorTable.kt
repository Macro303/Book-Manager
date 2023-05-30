package github.buriedincode.bookshelf.tables

import github.buriedincode.bookshelf.Utils
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils

object CreatorTable : LongIdTable(name = "creators"), Logging {
    val imageUrlCol: Column<String?> = text(name = "image_url").nullable()
    val nameCol: Column<String> = text(name = "name").uniqueIndex()

    init {
        Utils.query(description = "Create Creator Table") {
            SchemaUtils.create(this)
        }
    }
}