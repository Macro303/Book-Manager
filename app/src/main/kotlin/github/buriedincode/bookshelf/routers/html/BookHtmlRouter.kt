package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.asEnumOrNull
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Format
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.models.Series
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.BookTable
import github.buriedincode.bookshelf.tables.CreditTable
import github.buriedincode.bookshelf.tables.SeriesTable
import io.javalin.http.Context
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll

object BookHtmlRouter : BaseHtmlRouter<Book>(entity = Book, plural = "books") {
    override fun list(ctx: Context) = Utils.query {
        val extras = if (ctx.getSession() == null) mapOf("read" to true, "wished" to true) else emptyMap()
        render(ctx, "list", mapOf("resources" to filterResources(ctx), "filters" to filters(ctx), "extras" to extras), redirect = false)
    }

    override fun view(ctx: Context) = Utils.query {
        renderResource(ctx, "view", mapOf("credits" to ctx.getResource().credits.groupBy({ it.role }, { it.creator })), redirect = false)
    }

    fun import(ctx: Context) = Utils.query { render(ctx, "import") }

    fun search(ctx: Context) = Utils.query { render(ctx, "search") }

    override fun filterResources(ctx: Context): List<Book> {
        val query = BookTable.selectAll()
        val isCollected: Boolean? = ctx.queryParam("is-collected")?.lowercase()?.toBooleanStrictOrNull()
        ctx.queryParam("creator-id")?.toLongOrNull()?.let {
            Creator.findById(it)?.let { creator -> query.andWhere { CreditTable.creatorCol eq creator.id } }
        }
        ctx.queryParam("format")?.asEnumOrNull<Format>()?.let { format ->
            query.andWhere { BookTable.formatCol eq format }
        }
        ctx.getSession()?.let { session ->
            if (isCollected == true || isCollected == null) {
                ctx.queryParam("has-read")?.lowercase()?.toBooleanStrictOrNull()?.let { hasRead ->
                    if (hasRead) {
                        query.andWhere { BookTable.id inList session.readBooks.map { it.book.id } }
                    } else {
                        query.andWhere { BookTable.id notInList session.readBooks.map { it.book.id } }
                    }
                }
            }
            if (isCollected == false || isCollected == null) {
                ctx.queryParam("has-wished")?.lowercase()?.toBooleanStrictOrNull()?.let { hasWished ->
                    if (hasWished) {
                        query.andWhere { BookTable.id inList session.wishedBooks.map { it.id } }
                    } else {
                        query.andWhere { BookTable.id notInList session.wishedBooks.map { it.id } }
                    }
                }
            }
        }
        isCollected?.let {
            query.andWhere { BookTable.isCollectedCol eq it }
        }
        ctx.queryParam("publisher-id")?.toLongOrNull()?.let {
            Publisher.findById(it)?.let { publisher -> query.andWhere { BookTable.publisherCol eq publisher.id } }
        }
        ctx.queryParam("series-id")?.toLongOrNull()?.let {
            Series.findById(it)?.let { series -> query.andWhere { BookSeriesTable.seriesCol eq series.id } }
        }
        ctx.queryParam("title")?.let { title ->
            query.andWhere { (BookTable.titleCol like "%$title%") or (BookTable.subtitleCol like "%$title%") }
        }
        return Book.wrapRows(query).toList()
    }

    override fun filters(ctx: Context): Map<String, Any?> = mapOf(
        "creator" to ctx.queryParam("creator-id")?.toLongOrNull()?.let { Creator.findById(it) },
        "format" to ctx.queryParam("format")?.asEnumOrNull<Format>(),
        "has-read" to ctx.queryParam("has-read")?.lowercase()?.toBooleanStrictOrNull(),
        "has-wished" to ctx.queryParam("has-wished")?.lowercase()?.toBooleanStrictOrNull(),
        "is-collected" to ctx.queryParam("is-collected")?.lowercase()?.toBooleanStrictOrNull(),
        "publisher" to ctx.queryParam("publisher-id")?.toLongOrNull()?.let { Publisher.findById(it) },
        "series" to ctx.queryParam("series-id")?.toLongOrNull()?.let { Series.findById(it) },
        "title" to ctx.queryParam("title"),
    )

    override fun optionMap(): Map<String, Any?> = mapOf(
        "creators" to Creator.all().toList(),
        "formats" to Format.entries.toList(),
        "has-read" to listOf(true, false),
        "has-wished" to listOf(true, false),
        "is-collected" to listOf(true, false),
        "publishers" to Publisher.all().toList(),
        "roles" to Role.all().toList(),
        "series" to Series.all().toList(),
    )

    override fun optionMapExclusions(ctx: Context): Map<String, Any?> = mapOf(
        "series" to Series
            .wrapRows(
                SeriesTable
                    .selectAll()
                    .where { BookSeriesTable.seriesCol notInList ctx.getResource().series.map { it.series.id } }
                    .withDistinct(),
            ).toList(),
    )
}
