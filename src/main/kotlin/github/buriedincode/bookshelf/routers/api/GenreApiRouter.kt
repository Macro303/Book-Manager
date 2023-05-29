package github.buriedincode.bookshelf.routers.api

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import github.buriedincode.bookshelf.ErrorResponse
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.GenreEntry
import github.buriedincode.bookshelf.models.*
import github.buriedincode.bookshelf.tables.GenreTable
import io.javalin.http.*
import io.javalin.openapi.*
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection

object GenreApiRouter : Logging {
    private fun Context.getGenre(): Genre {
        return this.pathParam("genre-id").toLongOrNull()?.let {
            Genre.findById(id = it) ?: throw NotFoundResponse(message = "Genre not found")
        } ?: throw BadRequestResponse(message = "Invalid Genre Id")
    }

    private fun Context.getGenreInput(): GenreInput = this.bodyValidator<GenreInput>()
        .check({ it.title.isNotBlank() }, error = "Title must not be empty")
        .get()

    @OpenApi(
        description = "List all Genres",
        methods = [HttpMethod.GET],
        operationId = "listGenres",
        path = "/genres",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(Array<GenreEntry>::class)]),
        ],
        security = [],
        summary = "List all Genres",
        tags = ["Genre"]
    )
    fun listGenres(ctx: Context): Unit = Utils.query(description = "List Genres") {
        val genres = Genre.all()
        ctx.json(genres.map { it.toJson() })
    }

    @OpenApi(
        description = "Create Genre",
        methods = [HttpMethod.POST],
        operationId = "createGenre",
        path = "/genres",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(GenreInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "201", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Create Genre",
        tags = ["Genre"]
    )
    fun createGenre(ctx: Context): Unit = Utils.query(description = "Create Genre") {
        val body = ctx.getGenreInput()
        val exists = Genre.find {
            GenreTable.titleCol eq body.title
        }.firstOrNull()
        if (exists != null)
            throw ConflictResponse(message = "Genre already exists")
        val genre = Genre.new {
            books = SizedCollection(body.bookIds.map {
                Book.findById(id = it)
                    ?: throw NotFoundResponse(message = "Book not found")
            })
            title = body.title
        }

        ctx.status(HttpStatus.CREATED).json(genre.toJson(showAll = true))
    }

    @OpenApi(
        description = "Get Genre by id",
        methods = [HttpMethod.GET],
        operationId = "getGenre",
        path = "/genres/{genre-id}",
        pathParams = [OpenApiParam(name = "genre-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Get Genre by id",
        tags = ["Genre"]
    )
    fun getGenre(ctx: Context): Unit = Utils.query(description = "Get Genre") {
        val genre = ctx.getGenre()
        ctx.json(genre.toJson(showAll = true))
    }

    @OpenApi(
        description = "Update Genre",
        methods = [HttpMethod.PUT],
        operationId = "updateGenre",
        path = "/genres/{genre-id}",
        pathParams = [OpenApiParam(name = "genre-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(GenreInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Update Genre",
        tags = ["Genre"]
    )
    fun updateGenre(ctx: Context): Unit = Utils.query(description = "Update Genre") {
        val genre = ctx.getGenre()
        val body = ctx.getGenreInput()
        val exists = Genre.find {
            GenreTable.titleCol eq body.title
        }.firstOrNull()
        if (exists != null)
            throw ConflictResponse(message = "Genre already exists")
        genre.books = SizedCollection(body.bookIds.map {
            Book.findById(id = it)
                ?: throw NotFoundResponse(message = "Book not found")
        })
        genre.title = body.title

        ctx.json(genre.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete Genre",
        methods = [HttpMethod.DELETE],
        operationId = "deleteGenre",
        path = "/genres/{genre-id}",
        pathParams = [OpenApiParam(name = "genre-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "204"),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Delete Genre",
        tags = ["Genre"]
    )
    fun deleteGenre(ctx: Context): Unit = Utils.query(description = "Delete Genre") {
        val genre = ctx.getGenre()
        genre.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }

    private fun Context.getIdValue(): IdValue = this.bodyValidator<IdValue>()
        .check({ it.id > 0 }, error = "Id must be greater than 0")
        .get()

    @OpenApi(
        description = "Add Book to Genre",
        methods = [HttpMethod.PATCH],
        operationId = "addBookToGenre",
        path = "/genres/{genre-id}/books",
        pathParams = [OpenApiParam(name = "genre-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Add Book to Genre",
        tags = ["Genre"]
    )
    fun addBook(ctx: Context): Unit = Utils.query(description = "Add Book to Genre") {
        val genre = ctx.getGenre()
        val body = ctx.getIdValue()
        val book = Book.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Book not found")
        if (book in genre.books)
            throw ConflictResponse(message = "Book already is linked to Genre")
        val temp = genre.books.toMutableList()
        temp.add(book)
        genre.books = SizedCollection(temp)

        ctx.json(genre.toJson(showAll = true))
    }

    @OpenApi(
        description = "Remove Book from Genre",
        methods = [HttpMethod.DELETE],
        operationId = "removeBookFromGenre",
        path = "/genres/{genre-id}/books",
        pathParams = [OpenApiParam(name = "genre-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Remove Book from Genre",
        tags = ["Genre"]
    )
    fun removeBook(ctx: Context): Unit = Utils.query(description = "Remove Book from Genre") {
        val genre = ctx.getGenre()
        val body = ctx.getIdValue()
        val book = Book.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Book not found")
        if (!genre.books.contains(book))
            throw BadRequestResponse(message = "Book isn't linked to Genre")
        val temp = genre.books.toMutableList()
        temp.remove(book)
        genre.books = SizedCollection(temp)

        ctx.json(genre.toJson(showAll = true))
    }
}