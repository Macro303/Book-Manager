package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.BookSeries
import github.buriedincode.bookshelf.models.IdValue
import github.buriedincode.bookshelf.models.Series
import github.buriedincode.bookshelf.models.SeriesBookInput
import github.buriedincode.bookshelf.models.SeriesInput
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.SeriesTable
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyValidator
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.and

object SeriesApiRouter : Logging {
    fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            var series = Series.all().toList()
            val title = ctx.queryParam("title")
            if (title != null) {
                series = series.filter {
                    it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
                }
            }
            ctx.json(series.sorted().map { it.toJson() })
        }

    private fun Context.getInput(): SeriesInput =
        this.bodyValidator<SeriesInput>()
            .check({ it.title.isNotBlank() }, error = "Title must not be empty")
            .check({ it.books.all { it.bookId > 0 } }, error = "bookId must be greater than 0")
            .get()

    fun createEndpoint(ctx: Context): Unit =
        Utils.query {
            val input = ctx.getInput()
            val exists = Series.find {
                SeriesTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse(message = "Series already exists")
            }
            val series = Series.new {
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

    private fun Context.getResource(): Series {
        return this.pathParam("series-id").toLongOrNull()?.let {
            Series.findById(id = it) ?: throw NotFoundResponse(message = "Unable to find Series: `$it`")
        } ?: throw BadRequestResponse(message = "Unable to find Series: `${this.pathParam("series-id")}`")
    }

    fun getEndpoint(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            ctx.json(resource.toJson(showAll = true))
        }

    fun updateEndpoint(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getInput()
            val exists = Series.find {
                SeriesTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse(message = "Series already exists")
            }
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

    fun deleteEndpoint(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            resource.books.forEach {
                it.delete()
            }
            resource.delete()
            ctx.status(HttpStatus.NO_CONTENT)
        }

    private fun Context.getBookInput(): SeriesBookInput =
        this.bodyValidator<SeriesBookInput>()
            .check({ it.bookId > 0 }, error = "bookId must be greater than 0")
            .get()

    fun addBook(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getBookInput()
            val book = Book.findById(id = input.bookId)
                ?: throw NotFoundResponse(message = "Unable to find Book: `${input.bookId}`")
            val bookSeries = BookSeries.find {
                (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq resource.id)
            }.firstOrNull() ?: BookSeries.new {
                this.book = book
                this.series = resource
            }
            bookSeries.number = if (input.number == 0) null else input.number

            ctx.json(resource.toJson(showAll = true))
        }

    private fun Context.getIdValue(): IdValue =
        this.bodyValidator<IdValue>()
            .check({ it.id > 0 }, error = "Id must be greater than 0")
            .get()

    fun removeBook(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getIdValue()
            val book = Book.findById(id = input.id)
                ?: throw NotFoundResponse(message = "Unable to find Book: `${input.id}`")
            val bookSeries = BookSeries.find {
                (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq resource.id)
            }.firstOrNull() ?: throw BadRequestResponse(message = "Book is not part of the Series")
            bookSeries.delete()

            ctx.json(resource.toJson(showAll = true))
        }
}
