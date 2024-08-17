package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.asEnumOrNull
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Format
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.Series
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.BookTable
import github.buriedincode.bookshelf.tables.CreditTable
import github.buriedincode.bookshelf.tables.UserTable
import github.buriedincode.bookshelf.tables.WishedTable
import io.javalin.http.Context
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

object UserHtmlRouter : BaseHtmlRouter<User>(entity = User, plural = "users") {
    override fun view(ctx: Context) = Utils.query {
        val resource = ctx.getResource()
        var nextBooks = resource.readBooks
            .flatMap {
                it.book.series.map { it.series to it.number }
            }.groupBy({ it.first }, { it.second })
            .map { (series, readNumbers) ->
                series to series.books
                    .filter { it.number != null && it.number!! !in readNumbers }
                    .minByOrNull { it.number!! }
                    ?.book
            }.toMap()
            .filterValues { it != null }
            .mapValues { it.value!! }
            .toSortedMap()
        val model = mapOf(
            "stats" to mapOf(
                "wishlist" to resource.wishedBooks.count(),
                "shared" to
                    BookTable
                        .selectAll()
                        .where {
                            (BookTable.isCollectedCol eq false) and
                                (BookTable.id notInSubQuery WishedTable.select(WishedTable.bookCol))
                        }.count(),
                "unread" to Book.find { BookTable.isCollectedCol eq true }.count() - resource.readBooks.count(),
                "read" to resource.readBooks.count(),
            ),
            "nextBooks" to nextBooks,
        )
        renderResource(ctx, "view", model)
    }

    fun wishlist(ctx: Context) = Utils.query {
        render(ctx, "wishlist", mapOf("resources" to filterWishlist(ctx), "filters" to bookFilters(ctx)), redirect = false)
    }

    fun readlist(ctx: Context) = Utils.query {
        render(ctx, "readlist", mapOf("resources" to filterReadlist(ctx), "filters" to bookFilters(ctx)), redirect = false)
    }

    override fun filterResources(ctx: Context): List<User> {
        val query = UserTable.selectAll()
        ctx.queryParam("username")?.let { username ->
            query.andWhere { UserTable.usernameCol like "%$username%" }
        }
        return User.wrapRows(query.withDistinct()).toList()
    }

    override fun filters(ctx: Context): Map<String, Any?> = mapOf(
        "username" to ctx.queryParam("username"),
    )

    override fun optionMapExclusions(ctx: Context): Map<String, Any?> = mapOf(
        "readBooks" to Book
            .wrapRows(
                BookTable
                    .selectAll()
                    .where {
                        (BookTable.isCollectedCol eq true) and
                            (BookTable.id notInList ctx.getResource().readBooks.map { it.book.id })
                    }.withDistinct(),
            ).toList(),
        "wishedBooks" to Book
            .wrapRows(
                BookTable
                    .selectAll()
                    .where {
                        (BookTable.isCollectedCol eq false) and
                            (BookTable.id notInList ctx.getResource().wishedBooks.map { it.id })
                    }.withDistinct(),
            ).toList(),
    )

    private fun filterWishlist(ctx: Context): List<Book> {
        val resource = ctx.getResource()
        val query = BookTable.selectAll().where {
            (BookTable.isCollectedCol eq false) and (
                (BookTable.id inList resource.wishedBooks.map { it.id }) or (
                    BookTable.id notInSubQuery WishedTable.select(WishedTable.bookCol)
                )
            )
        }
        ctx.queryParam("creator-id")?.toLongOrNull()?.let {
            Creator.findById(it)?.let { creator -> query.andWhere { CreditTable.creatorCol eq creator.id } }
        }
        ctx.queryParam("format")?.asEnumOrNull<Format>()?.let { format ->
            query.andWhere { BookTable.formatCol eq format }
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

    private fun filterReadlist(ctx: Context): List<Book> {
        val query = BookTable.selectAll().where {
            (BookTable.isCollectedCol eq true) and
                (BookTable.id inList ctx.getResource().readBooks.map { it.book.id })
        }
        ctx.queryParam("creator-id")?.toLongOrNull()?.let {
            Creator.findById(it)?.let { creator -> query.andWhere { CreditTable.creatorCol eq creator.id } }
        }
        ctx.queryParam("format")?.asEnumOrNull<Format>()?.let { format ->
            query.andWhere { BookTable.formatCol eq format }
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

    private fun bookFilters(ctx: Context): Map<String, Any?> = mapOf(
        "creator" to ctx.queryParam("creator-id")?.toLongOrNull()?.let { Creator.findById(it) },
        "format" to ctx.queryParam("format")?.asEnumOrNull<Format>(),
        "publisher" to ctx.queryParam("publisher-id")?.toLongOrNull()?.let { Publisher.findById(it) },
        "series" to ctx.queryParam("series-id")?.toLongOrNull()?.let { Series.findById(it) },
        "title" to ctx.queryParam("title"),
    )
}
