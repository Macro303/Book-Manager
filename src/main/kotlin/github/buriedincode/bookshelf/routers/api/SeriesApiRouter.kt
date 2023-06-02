package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.ErrorResponse
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.SeriesEntry
import github.buriedincode.bookshelf.models.*
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.SeriesTable
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.*
import io.javalin.openapi.*
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.and

object SeriesApiRouter : CrudHandler, Logging {
    private fun getResource(resourceId: String): Series {
        return resourceId.toLongOrNull()?.let {
            Series.findById(id = it) ?: throw NotFoundResponse(message = "Series not found")
        } ?: throw BadRequestResponse(message = "Invalid Series Id")
    }

    private fun Context.getBody(): SeriesInput = this.bodyValidator<SeriesInput>()
        .check({ it.title.isNotBlank() }, error = "Title must not be empty")
        .check({ it.books.all { it.bookId > 0 } }, error = "bookId must be greater than 0")
        .get()

    @OpenApi(
        description = "List all Series",
        methods = [HttpMethod.GET],
        operationId = "listSeries",
        path = "/series",
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(Array<SeriesEntry>::class)]),
        ],
        summary = "List all Series",
        tags = ["Series"]
    )
    override fun getAll(ctx: Context): Unit = Utils.query {
        val series = Series.all()
        ctx.json(series.map { it.toJson() })
    }

    @OpenApi(
        description = "Create Series",
        methods = [HttpMethod.POST],
        operationId = "createSeries",
        path = "/series",
        requestBody = OpenApiRequestBody(content = [OpenApiContent(SeriesInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "201", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Series::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Create Series",
        tags = ["Series"]
    )
    override fun create(ctx: Context): Unit = Utils.query {
        val body = ctx.getBody()
        val exists = Series.find {
            SeriesTable.titleCol eq body.title
        }.firstOrNull()
        if (exists != null)
            throw ConflictResponse(message = "Series already exists")
        val series = Series.new {
            title = body.title
        }
        body.books.forEach {
            val book = Book.findById(id = it.bookId)
                ?: throw NotFoundResponse(message = "Book not found")
            BookSeries.new {
                this.book = book
                this.series = series
                number = if (it.number == 0) null else it.number
            }
        }

        ctx.status(HttpStatus.CREATED).json(series.toJson(showAll = true))
    }

    @OpenApi(
        description = "Get Series by id",
        methods = [HttpMethod.GET],
        operationId = "getSeries",
        path = "/series/{series-id}",
        pathParams = [OpenApiParam(name = "series-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Series::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Get Series by id",
        tags = ["Series"]
    )
    override fun getOne(ctx: Context, resourceId: String): Unit = Utils.query {
        val series = getResource(resourceId = resourceId)
        ctx.json(series.toJson(showAll = true))
    }

    @OpenApi(
        description = "Update Series",
        methods = [HttpMethod.PUT],
        operationId = "updateSeries",
        path = "/series/{series-id}",
        pathParams = [OpenApiParam(name = "series-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(SeriesInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Series::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Update Series",
        tags = ["Series"]
    )
    override fun update(ctx: Context, resourceId: String): Unit = Utils.query {
        val series = getResource(resourceId = resourceId)
        val body = ctx.getBody()
        val exists = Series.find {
            SeriesTable.titleCol eq body.title
        }.firstOrNull()
        if (exists != null && exists != series)
            throw ConflictResponse(message = "Series already exists")
        series.title = body.title
        body.books.forEach {
            val book = Book.findById(id = it.bookId)
                ?: throw NotFoundResponse(message = "Book not found")
            val bookSeries = BookSeries.find {
                (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq series.id)
            }.firstOrNull()
            if (bookSeries == null)
                BookSeries.new {
                    this.book = book
                    this.series = series
                    number = if (it.number == 0) null else it.number
                }
            else
                bookSeries.number = if (it.number == 0) null else it.number
        }

        ctx.json(series.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete Series",
        methods = [HttpMethod.DELETE],
        operationId = "deleteSeries",
        path = "/series/{series-id}",
        pathParams = [OpenApiParam(name = "series-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "204"),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Delete Series",
        tags = ["Series"]
    )
    override fun delete(ctx: Context, resourceId: String): Unit = Utils.query {
        val series = getResource(resourceId = resourceId)
        series.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }

    private fun Context.getBookBody(): SeriesBookInput = this.bodyValidator<SeriesBookInput>()
        .check({ it.bookId > 0 }, error = "bookId must be greater than 0")
        .get()

    private fun Context.getIdValue(): IdValue = this.bodyValidator<IdValue>()
        .check({ it.id > 0 }, error = "Id must be greater than 0")
        .get()

    @OpenApi(
        description = "Add Book to Series",
        methods = [HttpMethod.PATCH],
        operationId = "addBookToSeries",
        path = "/series/{series-id}/books",
        pathParams = [OpenApiParam(name = "series-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(SeriesBookInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Series::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Add Book to Series",
        tags = ["Series"]
    )
    fun addBook(ctx: Context): Unit = Utils.query {
        val series = getResource(resourceId = ctx.pathParam("series-id"))
        val body = ctx.getBookBody()
        val book = Book.findById(id = body.bookId)
            ?: throw NotFoundResponse(message = "Book not found")
        val bookSeries = BookSeries.find {
            (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq series.id)
        }.firstOrNull()
        if (bookSeries != null)
            throw ConflictResponse(message = "Book already is linked to Series")
        BookSeries.new {
            this.book = book
            this.series = series
            number = body.number
        }

        ctx.json(series.toJson(showAll = true))
    }

    @OpenApi(
        description = "Remove Book from Series",
        methods = [HttpMethod.DELETE],
        operationId = "removeBookFromSeries",
        path = "/series/{series-id}/books",
        pathParams = [OpenApiParam(name = "series-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Series::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Remove Book from Series",
        tags = ["Series"]
    )
    fun removeBook(ctx: Context): Unit = Utils.query {
        val series = getResource(resourceId = ctx.pathParam("series-id"))
        val body = ctx.getIdValue()
        val book = Book.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Book not found")
        val bookSeries = BookSeries.find {
            (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq series.id)
        }.firstOrNull() ?: throw NotFoundResponse(message = "Book isn't linked to Series")
        bookSeries.delete()

        ctx.json(series.toJson(showAll = true))
    }
}
