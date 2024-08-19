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
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.Context
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

object UserHtmlRouter : BaseHtmlRouter<User>(entity = User, plural = "users") {
    @JvmStatic
    private val LOGGER = KotlinLogging.logger { }

    override fun create(ctx: Context) = Utils.query {
        render(ctx, "create", optionMap(), redirect = false)
    }

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
            "stats" to mapOf<String, Long>(
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
        renderResource(ctx, "view", model, redirect = false)
    }

    fun wishlist(ctx: Context) = Utils.query {
        val resource = ctx.getResource()
        ctx.render(
            "templates/book/list.kte",
            mapOf(
                "session" to ctx.getSession(),
                "resources" to filterWishlist(ctx),
                "filters" to wishlistFilters(ctx),
                "extras" to mapOf(
                    "collected" to true,
                    "read" to true,
                    "resetUrl" to "/users/${resource.id.value}/wishlist",
                    "title" to "${resource.username}'s Wishlist",
                ),
            ),
        )
    }

    fun readlist(ctx: Context) = Utils.query {
        val resource = ctx.getResource()
        ctx.render(
            "templates/book/list.kte",
            mapOf(
                "session" to ctx.getSession(),
                "resources" to filterReadlist(ctx),
                "filters" to readlistFilters(ctx),
                "extras" to mapOf(
                    "collected" to true,
                    "read" to true,
                    "resetUrl" to "/users/${resource.id.value}/readlist",
                    "title" to "${resource.username}'s Readlist",
                    "wished" to true,
                ),
            ),
        )
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
        val query = BookTable.selectAll().where { BookTable.isCollectedCol eq false }
        ctx.queryParam("creator-id")?.toLongOrNull()?.let {
            Creator.findById(it)?.let { creator -> query.andWhere { CreditTable.creatorCol eq creator.id } }
        }
        ctx.queryParam("format")?.asEnumOrNull<Format>()?.let { format ->
            query.andWhere { BookTable.formatCol eq format }
        }
        ctx.queryParam("has-wished")?.lowercase()?.toBooleanStrictOrNull()?.let { hasWished ->
            if (hasWished) {
                query.andWhere { BookTable.id inList resource.wishedBooks.map { it.id } }
            } else {
                query.andWhere { BookTable.id notInList resource.wishedBooks.map { it.id } }
            }
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

    private fun wishlistFilters(ctx: Context): Map<String, Any?> = mapOf(
        "creator" to ctx.queryParam("creator-id")?.toLongOrNull()?.let { Creator.findById(it) },
        "format" to ctx.queryParam("format")?.asEnumOrNull<Format>(),
        "has-read" to false,
        "has-wished" to ctx.queryParam("has-wished")?.lowercase()?.toBooleanStrictOrNull(),
        "is-collected" to false,
        "publisher" to ctx.queryParam("publisher-id")?.toLongOrNull()?.let { Publisher.findById(it) },
        "series" to ctx.queryParam("series-id")?.toLongOrNull()?.let { Series.findById(it) },
        "title" to ctx.queryParam("title"),
    )

    private fun filterReadlist(ctx: Context): List<Book> {
        val query = BookTable.selectAll().where { BookTable.id inList ctx.getResource().readBooks.map { it.book.id } }
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

    private fun readlistFilters(ctx: Context): Map<String, Any?> = mapOf(
        "creator" to ctx.queryParam("creator-id")?.toLongOrNull()?.let { Creator.findById(it) },
        "format" to ctx.queryParam("format")?.asEnumOrNull<Format>(),
        "has-read" to true,
        "has-wished" to null,
        "is-collected" to true,
        "publisher" to ctx.queryParam("publisher-id")?.toLongOrNull()?.let { Publisher.findById(it) },
        "series" to ctx.queryParam("series-id")?.toLongOrNull()?.let { Series.findById(it) },
        "title" to ctx.queryParam("title"),
    )
}
