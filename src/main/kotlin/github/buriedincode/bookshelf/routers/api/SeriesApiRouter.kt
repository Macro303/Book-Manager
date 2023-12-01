package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.BookSeries
import github.buriedincode.bookshelf.models.Series
import github.buriedincode.bookshelf.models.SeriesInput
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.SeriesTable
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyValidator
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.and

object SeriesApiRouter : BaseApiRouter<Series>(entity = Series), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = entity.all().toList()
            ctx.queryParam("book-id")?.toLongOrNull()?.let {
                Book.findById(it)?.let { book ->
                    resources = resources.filter { book in it.books.map { it.book } }
                }
            }
            ctx.queryParam("title")?.let { title ->
                resources = resources.filter { it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true) }
            }
            ctx.json(resources.sorted().map { it.toJson() })
        }
    }

    private fun Context.getInput(): SeriesInput =
        this.bodyValidator<SeriesInput>()
            .check({ it.title.isNotBlank() }, error = "Title must not be empty")
            .check({ it.books.all { it.bookId > 0 } }, error = "bookId must be greater than 0")
            .get()

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val input = ctx.getInput()
            val exists = Series.find {
                SeriesTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse(message = "Series already exists")
            }
            val series = Series.new {
                summary = input.summary
                title = input.title
            }
            input.books.forEach {
                val book = Book.findById(id = it.bookId)
                    ?: throw NotFoundResponse(message = "Unable to find Book: `${it.bookId}`")
                val bookSeries = BookSeries.find {
                    (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq series.id)
                }.firstOrNull() ?: BookSeries.new {
                    this.book = book
                    this.series = series
                }
                bookSeries.number = if (it.number == 0) null else it.number
            }

            ctx.status(HttpStatus.CREATED).json(series.toJson(showAll = true))
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getInput()
            val exists = Series.find {
                SeriesTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse(message = "Series already exists")
            }
            resource.summary = input.summary
            resource.title = input.title
            input.books.forEach {
                val book = Book.findById(id = it.bookId)
                    ?: throw NotFoundResponse(message = "Unable to find Book: `${it.bookId}`")
                val bookSeries = BookSeries.find {
                    (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq resource.id)
                }.firstOrNull() ?: BookSeries.new {
                    this.book = book
                    this.series = resource
                }
                bookSeries.number = if (it.number == 0) null else it.number
            }

            ctx.json(resource.toJson(showAll = true))
        }
    }

    override fun deleteEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            resource.books.forEach {
                it.delete()
            }
            resource.delete()
            ctx.status(HttpStatus.NO_CONTENT)
        }
    }
}
