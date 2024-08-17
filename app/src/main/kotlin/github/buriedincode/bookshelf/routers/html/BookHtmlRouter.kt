package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.asEnumOrNull
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Format
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.models.Series
import io.javalin.http.Context

object BookHtmlRouter : BaseHtmlRouter<Book>(entity = Book, plural = "books") {
    override fun view(ctx: Context) = Utils.query {
        renderResource(ctx, "view", mapOf("credits", ctx.getResource().credits.groupBy({ it.role }, { it.creator })), redirect = false)
    }

    fun import(ctx: Context) = render(ctx, "import")

    fun search(ctx: Context) = render(ctx, "search")

    protected override fun filterResources(ctx: Context): List<Book> {
        return Book
            .find { BooksTable.id neq -1 }
            .apply {
                ctx.queryParam("creator-id")?.toLongOrNull()?.let {
                    Creator.findById(it)?.let { andWhere { BookCreditsTable.creator eq it } }
                }
                ctx.queryParam("format")?.asEnumOrNull<Format>()?.let { andWhere { BooksTable.format eq it } }
                ctx.queryParam("publisher-id")?.toLongOrNull()?.let {
                    Publisher.findById(it)?.let { andWhere { BooksTable.publisher eq it } }
                }
                ctx.queryParam("series-id")?.toLongOrNull()?.let { Series.findById(it)?.let { andWhere { BookSeriesTable.series eq it } } }
                ctx.queryParam("title")?.let { title ->
                    andWhere { (BooksTable.title like "%$title%") or (BooksTable.subtitle like "%$title%") }
                }
            }.toList()
    }

    protected override fun filters(ctx: Context): Map<String, Any?> = mapOf(
        "creator" to ctx.queryParam("creator-id")?.toLongOrNull()?.let { Creator.findById(it) },
        "format" to ctx.queryParam("format")?.asEnumOrNull<Format>(),
        "publisher" to ctx.queryParam("publisher-id")?.toLongOrNull()?.let { Publisher.findById(it) },
        "series" to ctx.queryParam("series-id")?.toLongOrNull()?.let { Series.findById(it) },
        "title" to ctx.queryParam("title"),
    )

    protected override fun optionMap(): Map<String, Any?> = mapOf(
        "creators" to Creator.all().toList(),
        "formats" to Format.entries.toList(),
        "publishers" to Publisher.all().toList(),
        "roles" to Role.all().toList(),
        "series" to Series.all().toList(),
    )

    protected override fun optionMapExclusions(ctx: Context): Map<String, Any?> = mapOf(
        "series" to Series.find { BookSeriesTable.series notInList ctx.getResource().series.map { it.series } }.distinct().toList(),
    )
}
