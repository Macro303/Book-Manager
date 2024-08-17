package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.asEnumOrNull
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Format
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.Series
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.tables.BookTable
import github.buriedincode.bookshelf.tables.UserTable
import github.buriedincode.bookshelf.tables.ReadBookTable
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.WishedTable
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.like
import org.jetbrains.exposed.sql.SizedCollection
import io.javalin.http.Context

object UserHtmlRouter : BaseHtmlRouter<User>(entity = User, plural = "users") {
    override fun view(ctx: Context) = Utils.query {
        val resource = ctx.getResource()
        val model = mapOf(
            "stats" to mapOf(
                "wishlist" to Book.find { (BookTable.isCollected eq false) and (resource.id inList BookTable.wishers) }.count(),
                "shared" to Book.find { (BookTable.isCollected eq false) and (BookTable.wishers.isEmpty()) }.count(),
                "unread" to Book.find { (BookTable.isCollected eq true) }.count() - resource.readBooks.count(),
                "read" to resource.readBooks.count(),
            ),
            "nextBooks" to Series
                .find {
                    SeriesTable.id inList resource.readBooks.mapNotNull { it.book.series.map { it.series.id }.flatten() }
                }.associateWith { series ->
                    series.books
                        .filter { it.number != null && it.number!! !in resource.readBooks.mapNotNull { it.book.number } }
                        .minByOrNull { it.number!! }
                }.filterValues { it != null }
                .mapValues { it.value!! }
                .toSortedMap(),
        )
        renderResource(ctx, "view", model)
    }

    fun wishlist(ctx: Context) = Utils.query {
        render(ctx, "wishlist", mapOf("resources" to filterBooks(ctx), "filters" to bookFilters(ctx)), redirect = false)
    }

    override fun filterResources(ctx: Context): List<User> {
        return User
            .find { UserTable.id neq -1 }
            .apply {
                ctx.queryParam("username")?.let { username -> andWhere { UserTable.usernameCol like "%$username%" } }
            }.toList()
    }

    override fun filters(ctx: Context): Map<String, Any?> = mapOf(
        "username" to ctx.queryParam("username"),
    )

    override fun optionMapExclusions(ctx: Context): Map<String, Any?> = mapOf(
        "readBooks" to Book
            .find {
                (BookTable.isCollectedCol eq true) and (ReadBookTable.bookCol notInList ctx.getResource().readBooks.map { it.book })
            }.distinct()
            .toList(),
        "wishedBooks" to Book
            .find {
                (BookTable.isCollectedCol eq true) and (BookTable.id notInList ctx.getResource().wishedBooks.map { it.id })
            }.distinct()
            .toList(),
    )
    
    private fun filterBooks(ctx: Context): List<Book> {
        return Book
            .find {
                (BookTable.isCollectedCol eq false) and
                    ((BookTable.id notInSubQuery WishedTable.slice(WishedTable.bookCol).select { WishedTable.userCol eq ctx.getResource().id }) or
                      (ctx.getResource().id inSubQuery WishedTable.slice(WishedTable.userCol).select { WishedTable.bookCol eq BookTable.id }))
            }.apply {
                ctx.queryParam("creator-id")?.toLongOrNull()?.let {
                    Creator.findById(it)?.let { andWhere { CreditTable.creatorCol eq it } }
                }
                ctx.queryParam("format")?.asEnumOrNull<Format>()?.let { andWhere { BookTable.formatCol eq it } }
                ctx.queryParam("publisher-id")?.toLongOrNull()?.let {
                    Publisher.findById(it)?.let { andWhere { BookTable.publisherCol eq it } }
                }
                ctx.queryParam("series-id")?.toLongOrNull()?.let { Series.findById(it)?.let { andWhere { BookSeriesTable.seriesCol eq it } } }
                ctx.queryParam("title")?.let { title ->
                    andWhere { (BookTable.titleCol like "%$title%") or (BookTable.subtitleCol like "%$title%") }
                }
            }.toList()
    }
    
    private fun bookFilters(ctx: Context): Map<String, Any?> = mapOf(
        "creator" to ctx.queryParam("creator-id")?.toLongOrNull()?.let { Creator.findById(it) },
        "format" to ctx.queryParam("format")?.asEnumOrNull<Format>(),
        "publisher" to ctx.queryParam("publisher-id")?.toLongOrNull()?.let { Publisher.findById(it) },
        "series" to ctx.queryParam("series-id")?.toLongOrNull()?.let { Series.findById(it) },
        "title" to ctx.queryParam("title"),
    )
}
