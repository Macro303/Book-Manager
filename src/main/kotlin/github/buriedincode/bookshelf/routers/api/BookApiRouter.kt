package github.buriedincode.bookshelf.routers.api

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.BookEntry
import github.buriedincode.bookshelf.models.*
import github.buriedincode.bookshelf.tables.BookSeriesTable.bookCol
import github.buriedincode.bookshelf.tables.BookSeriesTable.seriesCol
import io.javalin.http.*
import io.javalin.openapi.*
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and

object BookApiRouter : Logging {
    @OpenApi(
        description = "List all Books",
        methods = [HttpMethod.GET],
        operationId = "listBooks",
        path = "/books",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = []),
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(Array<BookEntry>::class)])],
        security = [],
        summary = "List all Books",
        tags = ["Book"]
    )
    fun listBooks(ctx: Context): Unit = Utils.query(description = "List Books") {
        val results = Book.all()
        ctx.json(results.map { it.toJson() })
    }

    @OpenApi(
        description = "Create Book",
        methods = [HttpMethod.POST],
        operationId = "createBook",
        path = "/books",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(BookInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "201", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
        ],
        security = [],
        summary = "Create Book",
        tags = ["Book"]
    )
    fun createBook(ctx: Context): Unit = Utils.query(description = "Create Book") {
        val input: BookInput
        try {
            input = ctx.bodyAsClass<BookInput>()
        } catch (upe: UnrecognizedPropertyException) {
            throw BadRequestResponse(message = "Invalid Body: ${upe.message}")
        }
        val result = Book.new {
            description = input.description
            format = input.format
            genres = SizedCollection(input.genreIds.map {
                Genre.findById(id = it) ?: throw NotFoundResponse(message = "Genre not found")
            })
            goodreadsId = input.goodreadsId
            googleBooksId = input.googleBooksId
            imageUrl = input.imageUrl
            isCollected = input.isCollected
            isbn = input.isbn
            libraryThingId = input.libraryThingId
            openLibraryId = input.openLibraryId
            publishDate = input.publishDate
            publisher = input.publisherId?.let {
                Publisher.findById(id = it) ?: throw NotFoundResponse(message = "Publisher not found")
            }
            readers = SizedCollection(input.readerIds.map {
                User.findById(id = it) ?: throw NotFoundResponse(message = "User not found")
            })
            subtitle = input.subtitle
            title = input.title
            wishers = SizedCollection(input.wisherIds.map {
                User.findById(id = it) ?: throw NotFoundResponse(message = "User not found")
            })
        }
        input.series.forEach {
            val _series = Series.findById(id = it.seriesId) ?: throw NotFoundResponse(message = "Series not found")
            val bookSeries = BookSeries.find {
                (bookCol eq result.id) and (seriesCol eq _series.id)
            }.firstOrNull()
            if (bookSeries == null)
                BookSeries.new {
                    book = result
                    series = _series
                    number = if(it.number == 0) null else it.number
                }
            else
                bookSeries.number = if(it.number == 0) null else it.number
        }

        ctx.status(HttpStatus.CREATED).json(result.toJson(showAll = true))
    }

    @OpenApi(
        description = "Get Book by id",
        methods = [HttpMethod.GET],
        operationId = "getBook",
        path = "/books/{book-id}",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
        ],
        security = [],
        summary = "Get Book by id",
        tags = ["Book"]
    )
    fun getBook(ctx: Context): Unit = Utils.query(description = "Get Book") {
        val bookId = ctx.pathParam("book-id")
        val result = bookId.toLongOrNull()?.let {
            Book.findById(id = it) ?: throw NotFoundResponse(message = "Book not found")
        } ?: throw BadRequestResponse(message = "Invalid Book Id")
        ctx.json(result.toJson(showAll = true))
    }

    @OpenApi(
        description = "Update Book",
        methods = [HttpMethod.PUT],
        operationId = "updateBook",
        path = "/books/{book-id}",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(BookInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
        ],
        security = [],
        summary = "Update Book",
        tags = ["Book"]
    )
    fun updateBook(ctx: Context): Unit = Utils.query(description = "Update Book") {
        val bookId = ctx.pathParam("book-id")
        val input: BookInput
        try {
            input = ctx.bodyAsClass<BookInput>()
        } catch (upe: UnrecognizedPropertyException) {
            throw BadRequestResponse(message = "Invalid Body: ${upe.message}")
        }
        val result = bookId.toLongOrNull()?.let {
            Book.findById(id = it) ?: throw NotFoundResponse(message = "Book not found")
        } ?: throw BadRequestResponse(message = "Invalid Book Id")
        result.description = input.description
        result.format = input.format
        result.genres = SizedCollection(input.genreIds.map {
            Genre.findById(id = it) ?: throw NotFoundResponse(message = "Genre not found")
        })
        result.goodreadsId = input.goodreadsId
        result.googleBooksId = input.googleBooksId
        result.imageUrl = input.imageUrl
        result.isCollected = input.isCollected
        result.isbn = input.isbn
        result.libraryThingId = input.libraryThingId
        result.openLibraryId = input.openLibraryId
        result.publishDate = input.publishDate
        result.publisher = input.publisherId?.let {
            Publisher.findById(id = it) ?: throw NotFoundResponse(message = "Publisher not found")
        }
        result.readers = SizedCollection(input.wisherIds.map {
            User.findById(id = it) ?: throw NotFoundResponse(message = "User not found")
        })
        result.subtitle = input.subtitle
        result.title = input.title
        result.wishers = SizedCollection(input.wisherIds.map {
            User.findById(id = it) ?: throw NotFoundResponse(message = "User not found")
        })
        input.series.forEach {
            val _series = Series.findById(id = it.seriesId) ?: throw NotFoundResponse(message = "Series not found")
            val bookSeries = BookSeries.find {
                (bookCol eq result.id) and (seriesCol eq _series.id)
            }.firstOrNull()
            if (bookSeries == null)
                BookSeries.new {
                    book = result
                    series = _series
                    number = if(it.number == 0) null else it.number
                }
            else
                bookSeries.number = if(it.number == 0) null else it.number
        }

        ctx.json(result.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete Book",
        methods = [HttpMethod.DELETE],
        operationId = "deleteBook",
        path = "/books/{book-id}",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "204")
        ],
        security = [],
        summary = "Delete Book",
        tags = ["Book"]
    )
    fun deleteBook(ctx: Context): Unit = Utils.query(description = "Delete Book") {
        val bookId = ctx.pathParam("book-id")
        val result = bookId.toLongOrNull()?.let {
            Book.findById(id = it) ?: throw NotFoundResponse(message = "Book not found")
        } ?: throw BadRequestResponse(message = "Invalid Book Id")
        result.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }
    
    @OpenApi(
        description = "Import Book",
        methods = [HttpMethod.POST],
        operationId = "importBook",
        path = "/books/import",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(BookImport::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
        ],
        security = [],
        summary = "Import Book",
        tags = ["Book"]
    )
    fun importBook(ctx: Context): Unit = Utils.query(description = "Import Book") {
        val input: BookImport
        try {
            input = ctx.bodyAsClass<BookImport>()
        } catch (upe: UnrecognizedPropertyException) {
            throw BadRequestResponse(message = "Invalid Body: ${upe.message}")
        }
        if (input.goodreadsId != null)
            throw NotImplementedResponse(message = "Goodreads import not currently supported.")
        if (input.googleBooksId != null)
            throw NotImplementedResponse(message = "Google Books import not currently supported.")
        if (input.isbn != null)
            throw NotImplementedResponse(message = "Isbn import not currently supported.")
        if (input.libraryThingId != null)
            throw NotImplementedResponse(message = "LibraryThing import not currently supported.")
        if (input.openLibraryId != null)
            throw NotImplementedResponse(message = "OpenLibrary import not currently supported.")
        
        ctx.status(HttpStatus.NOT_IMPLEMENTED)
    }

}
