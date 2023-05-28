package github.buriedincode.bookshelf.routers.api

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.SeriesEntry
import github.buriedincode.bookshelf.models.*
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.SeriesTable
import io.javalin.http.*
import io.javalin.openapi.*
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.and

object SeriesApiRouter : Logging {
    @OpenApi(
        description = "List all Series",
        methods = [HttpMethod.GET],
        operationId = "listSeries",
        path = "/series",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = []),
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(Array<SeriesEntry>::class)])],
        security = [],
        summary = "List all Series",
        tags = ["Series"]
    )
    fun listSeries(ctx: Context): Unit = Utils.query(description = "List Series") {
        val results = Series.all()
        ctx.json(results.map { it.toJson() })
    }

    @OpenApi(
        description = "Create Series",
        methods = [HttpMethod.POST],
        operationId = "createSeries",
        path = "/series",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(SeriesInput::class)], required = true),
        responses = [
            OpenApiResponse(
                status = "201",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Series::class)]
            ),
        ],
        security = [],
        summary = "Create Series",
        tags = ["Series"]
    )
    fun createSeries(ctx: Context): Unit = Utils.query(description = "Create Series") {
        val input: SeriesInput
        try {
            input = ctx.bodyAsClass<SeriesInput>()
        } catch (upe: UnrecognizedPropertyException) {
            throw BadRequestResponse(message = "Invalid Body: ${upe.message}")
        }
        val exists = Series.find {
            SeriesTable.titleCol eq input.title
        }.firstOrNull()
        if (exists != null)
            throw ConflictResponse(message = "Series already exists")
        val result = Series.new {
            title = input.title
        }
        input.books.forEach {
            val _book = Book.findById(id = it.bookId) ?: throw NotFoundResponse(message = "Book not found")
            val bookSeries = BookSeries.find {
                (BookSeriesTable.bookCol eq _book.id) and (BookSeriesTable.seriesCol eq result.id)
            }.firstOrNull()
            if (bookSeries == null)
                BookSeries.new {
                    book = _book
                    series = result
                    number = if(it.number == 0) null else it.number
                }
            else
                bookSeries.number = if(it.number == 0) null else it.number
        }

        ctx.status(HttpStatus.CREATED).json(result.toJson(showAll = true))
    }

    @OpenApi(
        description = "Get Series by id",
        methods = [HttpMethod.GET],
        operationId = "getSeries",
        path = "/series/{series-id}",
        pathParams = [OpenApiParam(name = "series-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Series::class)]
            ),
        ],
        security = [],
        summary = "Get Series by id",
        tags = ["Series"]
    )
    fun getSeries(ctx: Context): Unit = Utils.query(description = "Get Series") {
        val seriesId = ctx.pathParam("series-id")
        val result = seriesId.toLongOrNull()?.let {
            Series.findById(id = it) ?: throw NotFoundResponse(message = "Series not found")
        } ?: throw BadRequestResponse(message = "Invalid Series Id")
        ctx.json(result.toJson(showAll = true))
    }

    @OpenApi(
        description = "Update Series",
        methods = [HttpMethod.PUT],
        operationId = "updateSeries",
        path = "/series/{series-id}",
        pathParams = [OpenApiParam(name = "series-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(SeriesInput::class)], required = true),
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Series::class)]
            ),
        ],
        security = [],
        summary = "Update Series",
        tags = ["Series"]
    )
    fun updateSeries(ctx: Context): Unit = Utils.query(description = "Update Series") {
        val seriesId = ctx.pathParam("series-id")
        val input: SeriesInput
        try {
            input = ctx.bodyAsClass<SeriesInput>()
        } catch (upe: UnrecognizedPropertyException) {
            throw BadRequestResponse(message = "Invalid Body: ${upe.message}")
        }
        val exists = Series.find {
            SeriesTable.titleCol eq input.title
        }.firstOrNull()
        if (exists != null)
            throw ConflictResponse(message = "Series already exists")
        val result = seriesId.toLongOrNull()?.let {
            Series.findById(id = it) ?: throw NotFoundResponse(message = "Series not found")
        } ?: throw BadRequestResponse(message = "Invalid Series Id")
        result.title = input.title
        input.books.forEach {
            val _book = Book.findById(id = it.bookId) ?: throw NotFoundResponse(message = "Book not found")
            val bookSeries = BookSeries.find {
                (BookSeriesTable.bookCol eq _book.id) and (BookSeriesTable.seriesCol eq result.id)
            }.firstOrNull()
            if (bookSeries == null)
                BookSeries.new {
                    book = _book
                    series = result
                    number = if(it.number == 0) null else it.number
                }
            else
                bookSeries.number = if(it.number == 0) null else it.number
        }

        ctx.json(result.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete Series",
        methods = [HttpMethod.DELETE],
        operationId = "deleteSeries",
        path = "/series/{series-id}",
        pathParams = [OpenApiParam(name = "series-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "204")
        ],
        security = [],
        summary = "Delete Series",
        tags = ["Series"]
    )
    fun deleteSeries(ctx: Context): Unit = Utils.query(description = "Delete Series") {
        val seriesId = ctx.pathParam("series-id")
        val result = seriesId.toLongOrNull()?.let {
            Series.findById(id = it) ?: throw NotFoundResponse(message = "Series not found")
        } ?: throw BadRequestResponse(message = "Invalid Series Id")
        result.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }
    
    @OpenApi(
        description = "Add Book to Series",
        methods = [HttpMethod.POST],
        operationId = "addBookToSeries",
        path = "/series/{series-id}/books",
        pathParams = [OpenApiParam(name = "series-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(SeriesBookInput::class)], required = true),
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Series::class)]
            ),
        ],
        security = [],
        summary = "Add Book to Series",
        tags = ["Series"]
    )
    fun addBook(ctx: Context): Unit = Utils.query(description = "Add Book to Series") {
        val seriesId = ctx.pathParam("series-id")
        val result = seriesId.toLongOrNull()?.let {
            Series.findById(id = it) ?: throw NotFoundResponse(message = "Series not found")
        } ?: throw BadRequestResponse(message = "Invalid Series Id")
        val input: SeriesBookInput
        try {
            input = ctx.bodyAsClass<SeriesBookInput>()
        } catch (upe: UnrecognizedPropertyException) {
            throw BadRequestResponse(message = "Invalid Body: ${upe.message}")
        }
        val _book = Book.findById(id = input.bookId)
            ?: throw NotFoundResponse(message = "Book not found")
        val bookSeries = BookSeries.find {
            (BookSeriesTable.bookCol eq _book.id) and (BookSeriesTable.seriesCol eq result.id)
        }.firstOrNull()
        if (bookSeries != null)
            throw ConflictResponse(message = "Book already is linked to Series")
        BookSeries.new {
            book = _book
            series = result
            number = input.number
        }
        
        ctx.json(result.toJson(showAll = true))
    }
    
    @OpenApi(
        description = "Remove Book from Series",
        methods = [HttpMethod.DELETE],
        operationId = "removeBookFromSeries",
        path = "/series/{series-id}/books",
        pathParams = [OpenApiParam(name = "series-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)]
            ),
        ],
        security = [],
        summary = "Remove Book from Series",
        tags = ["Series"]
    )
    fun removeBook(ctx: Context): Unit = Utils.query(description = "Remove Book from Series") {
        val seriesId = ctx.pathParam("series-id")
        val result = seriesId.toLongOrNull()?.let {
            Series.findById(id = it) ?: throw NotFoundResponse(message = "Series not found")
        } ?: throw BadRequestResponse(message = "Invalid Series Id")
        val input: IdValue
        try {
            input = ctx.bodyAsClass<IdValue>()
        } catch (upe: UnrecognizedPropertyException) {
            throw BadRequestResponse(message = "Invalid Body: ${upe.message}")
        }
        val _book = Book.findById(id = input.id)
            ?: throw NotFoundResponse(message = "Book not found")
        val bookSeries = BookSeries.find {
            (BookSeriesTable.bookCol eq _book.id) and (BookSeriesTable.seriesCol eq result.id)
        }.firstOrNull() ?: throw NotFoundResponse(message = "Book isn't linked to Series")
        bookSeries.delete()
        
        ctx.json(result.toJson(showAll = true))
    }
}
