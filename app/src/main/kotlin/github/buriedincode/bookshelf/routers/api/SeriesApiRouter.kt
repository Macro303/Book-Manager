package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.BookSeries
import github.buriedincode.bookshelf.models.IdInput
import github.buriedincode.bookshelf.models.Series
import github.buriedincode.bookshelf.models.SeriesInput
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.SeriesTable
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll

object SeriesApiRouter : BaseApiRouter<Series>(entity = Series) {
    override fun list(ctx: Context): Unit = Utils.queryTransaction {
        val query = SeriesTable.selectAll()
        ctx.queryParam("book-id")?.toLongOrNull()?.let {
            Book.findById(it)?.let { book -> query.andWhere { BookSeriesTable.bookCol eq book.id } }
        }
        ctx.queryParam("title")?.let { title ->
            query.andWhere { SeriesTable.titleCol like "%$title%" }
        }
        ctx.json(Series.wrapRows(query.withDistinct()).toList().sorted().map { it.toJson() })
    }

    override fun create(ctx: Context) = ctx.processInput<SeriesInput> { body ->
        Series.find(body.title)?.let {
            throw ConflictResponse("Series already exists")
        }
        val resource = Series.findOrCreate(body.title).apply {
            body.books.forEach {
                BookSeries.new {
                    this.book = Book.findById(it.book) ?: throw NotFoundResponse("Book not found.")
                    this.series = this@apply
                    this.number = if (it.number == 0) null else it.number
                }
            }
            summary = body.summary
        }
        ctx.status(HttpStatus.CREATED).json(resource.toJson(showAll = true))
    }

    override fun update(ctx: Context) = manage<SeriesInput>(ctx) { body, series ->
        Series.find(body.title)?.takeIf { it != series }?.let { throw ConflictResponse("Series already exists") }
        series.apply {
            books.forEach { it.delete() }
            body.books.forEach {
                BookSeries
                    .findOrCreate(
                        Book.findById(it.book) ?: throw NotFoundResponse("Book not found."),
                        series,
                    ).apply {
                        number = if (it.number == 0) null else it.number
                    }
            }
            summary = body.summary
            title = body.title
        }
    }

    override fun delete(ctx: Context) = Utils.queryTransaction {
        ctx.getResource().apply {
            books.forEach { it.delete() }
            delete()
        }
        ctx.status(HttpStatus.NO_CONTENT)
    }

    fun addBook(ctx: Context) = manage<SeriesInput.Book>(ctx) { body, series ->
        BookSeries
            .findOrCreate(
                Book.findById(body.book) ?: throw NotFoundResponse("Book not found."),
                series,
            ).apply {
                number = if (body.number == 0) null else body.number
            }
    }

    fun removeBook(ctx: Context) = manage<IdInput>(ctx) { body, series ->
        BookSeries
            .findOrCreate(
                Book.findById(body.id) ?: throw NotFoundResponse("Book not found."),
                series,
            )?.delete()
    }
}
