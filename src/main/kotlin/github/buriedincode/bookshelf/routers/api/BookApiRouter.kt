package github.buriedincode.bookshelf.routers.api

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import github.buriedincode.bookshelf.ErrorResponse
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
    private fun Context.getBook(): Book {
        return this.pathParam("book-id").toLongOrNull()?.let {
            Book.findById(id = it) ?: throw NotFoundResponse(message = "Book not found")
        } ?: throw BadRequestResponse(message = "Invalid Book Id")
    }

    private fun Context.getBookInput(): BookInput = this.bodyValidator<BookInput>()
        .check({ it.title.isNotBlank() }, error = "Title must not be empty")
        .check({ it.series.all { it.seriesId > 0 } }, error = "bookId must be greater than 0")
        .get()

    @OpenApi(
        description = "List all Books",
        methods = [HttpMethod.GET],
        operationId = "listBooks",
        path = "/books",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(Array<BookEntry>::class)])
        ],
        security = [],
        summary = "List all Books",
        tags = ["Book"]
    )
    fun listBooks(ctx: Context): Unit = Utils.query(description = "List Books") {
        val books = Book.all()
        ctx.json(books.map { it.toJson() })
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
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Create Book",
        tags = ["Book"]
    )
    fun createBook(ctx: Context): Unit = Utils.query(description = "Create Book") {
        val body = ctx.getBookInput()
        val book = Book.new {
            description = body.description
            format = body.format
            genres = SizedCollection(body.genreIds.map {
                Genre.findById(id = it)
                    ?: throw NotFoundResponse(message = "Genre not found")
            })
            goodreadsId = body.goodreadsId
            googleBooksId = body.googleBooksId
            imageUrl = body.imageUrl
            isCollected = body.isCollected
            isbn = body.isbn
            libraryThingId = body.libraryThingId
            openLibraryId = body.openLibraryId
            publishDate = body.publishDate
            publisher = body.publisherId?.let {
                Publisher.findById(id = it)
                    ?: throw NotFoundResponse(message = "Publisher not found")
            }
            readers = SizedCollection(body.readerIds.map {
                User.findById(id = it)
                    ?: throw NotFoundResponse(message = "User not found")
            })
            subtitle = body.subtitle
            title = body.title
            wishers = SizedCollection(body.wisherIds.map {
                User.findById(id = it)
                    ?: throw NotFoundResponse(message = "User not found")
            })
        }
        body.series.forEach {
            val series = Series.findById(id = it.seriesId)
                ?: throw NotFoundResponse(message = "Series not found")
            val bookSeries = BookSeries.find {
                (bookCol eq book.id) and (seriesCol eq series.id)
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

        ctx.status(HttpStatus.CREATED).json(book.toJson(showAll = true))
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
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Get Book by id",
        tags = ["Book"]
    )
    fun getBook(ctx: Context): Unit = Utils.query(description = "Get Book") {
        val book = ctx.getBook()
        ctx.json(book.toJson(showAll = true))
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
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Update Book",
        tags = ["Book"]
    )
    fun updateBook(ctx: Context): Unit = Utils.query(description = "Update Book") {
        val book = ctx.getBook()
        val body = ctx.getBookInput()
        book.description = body.description
        book.format = body.format
        book.genres = SizedCollection(body.genreIds.map {
            Genre.findById(id = it)
                ?: throw NotFoundResponse(message = "Genre not found")
        })
        book.goodreadsId = body.goodreadsId
        book.googleBooksId = body.googleBooksId
        book.imageUrl = body.imageUrl
        book.isCollected = body.isCollected
        book.isbn = body.isbn
        book.libraryThingId = body.libraryThingId
        book.openLibraryId = body.openLibraryId
        book.publishDate = body.publishDate
        book.publisher = body.publisherId?.let {
            Publisher.findById(id = it)
                ?: throw NotFoundResponse(message = "Publisher not found")
        }
        book.readers = SizedCollection(body.wisherIds.map {
            User.findById(id = it)
                ?: throw NotFoundResponse(message = "User not found")
        })
        book.subtitle = body.subtitle
        book.title = body.title
        book.wishers = SizedCollection(body.wisherIds.map {
            User.findById(id = it)
                ?: throw NotFoundResponse(message = "User not found")
        })
        body.series.forEach {
            val series = Series.findById(id = it.seriesId)
                ?: throw NotFoundResponse(message = "Series not found")
            val bookSeries = BookSeries.find {
                (bookCol eq book.id) and (seriesCol eq series.id)
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

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete Book",
        methods = [HttpMethod.DELETE],
        operationId = "deleteBook",
        path = "/books/{book-id}",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "204"),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Delete Book",
        tags = ["Book"]
    )
    fun deleteBook(ctx: Context): Unit = Utils.query(description = "Delete Book") {
        val book = ctx.getBook()
        book.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }

    private fun Context.getBookImport(): BookImport = this.bodyValidator<BookImport>()
        .check({ !it.goodreadsId.isNullOrBlank() || !it.googleBooksId.isNullOrBlank() || !it.isbn.isNullOrBlank() || !it.libraryThingId.isNullOrBlank() || !it.openLibraryId.isNullOrBlank() }, error = "At least 1 id to import must be specified")
        .get()

    @OpenApi(
        description = "Import Book",
        methods = [HttpMethod.POST],
        operationId = "importBook",
        path = "/books/import",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(BookImport::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Import Book",
        tags = ["Book"]
    )
    fun importBook(ctx: Context): Unit = Utils.query(description = "Import Book") {
        val body = ctx.getBookImport()
        if (body.goodreadsId != null)
            throw NotImplementedResponse(message = "Goodreads import not currently supported.")
        if (body.googleBooksId != null)
            throw NotImplementedResponse(message = "Google Books import not currently supported.")
        if (body.isbn != null)
            throw NotImplementedResponse(message = "Isbn import not currently supported.")
        if (body.libraryThingId != null)
            throw NotImplementedResponse(message = "LibraryThing import not currently supported.")
        if (body.openLibraryId != null)
            throw NotImplementedResponse(message = "OpenLibrary import not currently supported.")

        ctx.status(HttpStatus.NOT_IMPLEMENTED)
    }

    @OpenApi(
        description = "Discard Book",
        methods = [HttpMethod.PATCH],
        operationId = "discardBook",
        path = "/books/{book-id}/discard",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Discard Book",
        tags = ["Book"]
    )
    fun discardBook(ctx: Context): Unit = Utils.query(description = "Discard Book") {
        val book = ctx.getBook()
        book.isCollected = false
        book.readers = SizedCollection()
        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Collect Book",
        methods = [HttpMethod.PATCH],
        operationId = "collectBook",
        path = "/books/{book-id}/collect",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Collect Book",
        tags = ["Book"]
    )
    fun collectBook(ctx: Context): Unit = Utils.query(description = "Collect Book") {
        val book = ctx.getBook()
        book.isCollected = true
        book.wishers = SizedCollection()
        ctx.json(book.toJson(showAll = true))
    }

    private fun Context.getIdValue(): IdValue = this.bodyValidator<IdValue>()
        .check({ it.id > 0 }, error = "Id must be greater than 0")
        .get()

    @OpenApi(
        description = "Unread Book",
        methods = [HttpMethod.PATCH],
        operationId = "unreadBook",
        path = "/books/{book-id}/unread",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Unread Book",
        tags = ["Book"]
    )
    fun unreadBook(ctx: Context): Unit = Utils.query(description = "Unread Book") {
        val book = ctx.getBook()
        if (!book.isCollected)
            throw BadRequestResponse(message = "Book hasn't been collected to be able to unread")
        val body = ctx.getIdValue()
        val user = User.findById(id = body.id)
            ?: throw NotFoundResponse(message = "User not found")
        if (!book.readers.contains(user))
            throw BadRequestResponse(message = "Book hasn't been read by User")
        val temp = book.readers.toMutableList()
        temp.remove(user)
        book.readers = SizedCollection(temp)

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Read Book",
        methods = [HttpMethod.PATCH],
        operationId = "readBook",
        path = "/books/{book-id}/read",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Read Book",
        tags = ["Book"]
    )
    fun readBook(ctx: Context): Unit = Utils.query(description = "Read Book") {
        val book = ctx.getBook()
        if (!book.isCollected)
            throw BadRequestResponse(message = "Book hasn't been collected to be able to read")
        val body = ctx.getIdValue()
        val user = User.findById(id = body.id)
            ?: throw NotFoundResponse(message = "User not found")
        if (user in book.readers)
            throw BadRequestResponse(message = "Book has already been read by User")
        val temp = book.readers.toMutableList()
        temp.add(user)
        book.readers = SizedCollection(temp)

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Unwish Book",
        methods = [HttpMethod.PATCH],
        operationId = "unwishBook",
        path = "/books/{book-id}/unwish",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Unwish Book",
        tags = ["Book"]
    )
    fun unwishBook(ctx: Context): Unit = Utils.query(description = "Unwish Book") {
        val book = ctx.getBook()
        if (book.isCollected)
            throw BadRequestResponse(message = "Book has already been collected")
        val body = ctx.getIdValue()
        val user = User.findById(id = body.id)
            ?: throw NotFoundResponse(message = "User not found")
        if (!book.wishers.contains(user))
            throw BadRequestResponse(message = "Book hasn't been wished by User")
        val temp = book.wishers.toMutableList()
        temp.remove(user)
        book.wishers = SizedCollection(temp)

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Wish Book",
        methods = [HttpMethod.PATCH],
        operationId = "wishBook",
        path = "/books/{book-id}/wish",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Wish Book",
        tags = ["Book"]
    )
    fun wishBook(ctx: Context): Unit = Utils.query(description = "Wish Book") {
        val book = ctx.getBook()
        if (book.isCollected)
            throw BadRequestResponse(message = "Book has already been collected")
        val body = ctx.getIdValue()
        val user = User.findById(id = body.id)
            ?: throw NotFoundResponse(message = "User not found")
        if (user in book.wishers)
            throw BadRequestResponse(message = "Book has already been wished by User")
        val temp = book.wishers.toMutableList()
        temp.add(user)
        book.wishers = SizedCollection(temp)

        ctx.json(book.toJson(showAll = true))
    }

    fun addCreator(ctx: Context): Unit = Utils.query(description = "Add Creator to Book") {
        ctx.status(HttpStatus.NOT_IMPLEMENTED)
    }

    fun removeCreator(ctx: Context): Unit = Utils.query(description = "Remove Creator from Book") {
        ctx.status(HttpStatus.NOT_IMPLEMENTED)
    }

    fun addGenre(ctx: Context): Unit = Utils.query(description = "Add Genre to Book") {
        ctx.status(HttpStatus.NOT_IMPLEMENTED)
    }

    fun removeGenre(ctx: Context): Unit = Utils.query(description = "Remove Genre from Book") {
        ctx.status(HttpStatus.NOT_IMPLEMENTED)
    }

    fun addSeries(ctx: Context): Unit = Utils.query(description = "Add Series to Book") {
        ctx.status(HttpStatus.NOT_IMPLEMENTED)
    }

    fun removeSeries(ctx: Context): Unit = Utils.query(description = "Remove Series from Book") {
        ctx.status(HttpStatus.NOT_IMPLEMENTED)
    }
}