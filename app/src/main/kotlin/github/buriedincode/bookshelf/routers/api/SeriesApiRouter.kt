package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.BookSeries
import github.buriedincode.bookshelf.models.IdInput
import github.buriedincode.bookshelf.models.Series
import github.buriedincode.bookshelf.models.SeriesInput
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.SeriesTable
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyAsClass
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.and

object SeriesApiRouter : BaseApiRouter<Series>(entity = Series), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = Series.all().toList()
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

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val body = ctx.bodyAsClass<SeriesInput>()
            val exists = Series
                .find {
                    SeriesTable.titleCol eq body.title
                }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse("Series already exists")
            }
            val resource = Series.new {
                this.summary = body.summary
                this.title = body.title
            }

            ctx.status(HttpStatus.CREATED).json(resource.toJson(showAll = true))
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<SeriesInput>()
            val exists = Series
                .find {
                    SeriesTable.titleCol eq body.title
                }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse("Series already exists")
            }
            resource.summary = body.summary
            resource.title = body.title

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

    fun addBook(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<SeriesInput.Book>()
            val book = Book.findById(body.bookId)
                ?: throw NotFoundResponse("No Book found.")
            val bookSeries = BookSeries
                .find {
                    (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq resource.id)
                }.firstOrNull() ?: BookSeries.new {
                this.book = book
                this.series = resource
            }
            bookSeries.number = if (body.number == 0) null else body.number

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun removeBook(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<IdInput>()
            val book = Book.findById(body.id)
                ?: throw NotFoundResponse("No Book found.")
            val bookSeries = BookSeries
                .find {
                    (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq resource.id)
                }.firstOrNull() ?: throw BadRequestResponse("Book Series not found.")
            bookSeries.delete()

            ctx.status(HttpStatus.NO_CONTENT)
        }
    }
}
