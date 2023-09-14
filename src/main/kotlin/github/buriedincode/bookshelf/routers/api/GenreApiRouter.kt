package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.ErrorResponse
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.GenreEntry
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.GenreInput
import github.buriedincode.bookshelf.models.IdValue
import github.buriedincode.bookshelf.tables.GenreTable
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyValidator
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiRequestBody
import io.javalin.openapi.OpenApiResponse
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection

object GenreApiRouter : CrudHandler, Logging {
    private fun getResource(resourceId: String): Genre {
        return resourceId.toLongOrNull()?.let {
            Genre.findById(id = it) ?: throw NotFoundResponse(message = "Genre not found")
        } ?: throw BadRequestResponse(message = "Invalid Genre Id")
    }

    private fun Context.getBody(): GenreInput = this.bodyValidator<GenreInput>()
        .check({ it.title.isNotBlank() }, error = "Title must not be empty")
        .get()

    @OpenApi(
        description = "List all Genres",
        methods = [HttpMethod.GET],
        operationId = "listGenres",
        path = "/genres",
        queryParams = [
            OpenApiParam(name = "title", type = String::class),
        ],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(Array<GenreEntry>::class)]),
        ],
        summary = "List all Genres",
        tags = ["Genre"],
    )
    override fun getAll(ctx: Context): Unit = Utils.query {
        var genres = Genre.all().toList()
        val title = ctx.queryParam("title")
        if (title != null) {
            genres = genres.filter {
                it.title.contains(title, ignoreCase = true) || title.contains(
                    it.title,
                    ignoreCase = true,
                )
            }
        }
        ctx.json(genres.sorted().map { it.toJson() })
    }

    @OpenApi(
        description = "Create Genre",
        methods = [HttpMethod.POST],
        operationId = "createGenre",
        path = "/genres",
        requestBody = OpenApiRequestBody(content = [OpenApiContent(GenreInput::class)], required = true),
        responses = [
            OpenApiResponse(
                status = "201",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)],
            ),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Create Genre",
        tags = ["Genre"],
    )
    override fun create(ctx: Context): Unit = Utils.query {
        val body = ctx.getBody()
        val exists = Genre.find {
            GenreTable.titleCol eq body.title
        }.firstOrNull()
        if (exists != null) {
            throw ConflictResponse(message = "Genre already exists")
        }
        val genre = Genre.new {
            books = SizedCollection(
                body.bookIds.map {
                    Book.findById(id = it)
                        ?: throw NotFoundResponse(message = "Book not found")
                },
            )
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
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)],
            ),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Get Genre by id",
        tags = ["Genre"],
    )
    override fun getOne(ctx: Context, resourceId: String): Unit = Utils.query {
        val genre = getResource(resourceId = resourceId)
        ctx.json(genre.toJson(showAll = true))
    }

    @OpenApi(
        description = "Update Genre",
        methods = [HttpMethod.PATCH],
        operationId = "updateGenre",
        path = "/genres/{genre-id}",
        pathParams = [OpenApiParam(name = "genre-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(GenreInput::class)], required = true),
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)],
            ),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Update Genre",
        tags = ["Genre"],
    )
    override fun update(ctx: Context, resourceId: String): Unit = Utils.query {
        val genre = getResource(resourceId = resourceId)
        val body = ctx.getBody()
        val exists = Genre.find {
            GenreTable.titleCol eq body.title
        }.firstOrNull()
        if (exists != null && exists != genre) {
            throw ConflictResponse(message = "Genre already exists")
        }
        genre.books = SizedCollection(
            body.bookIds.map {
                Book.findById(id = it)
                    ?: throw NotFoundResponse(message = "Book not found")
            },
        )
        genre.title = body.title

        ctx.json(genre.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete Genre",
        methods = [HttpMethod.DELETE],
        operationId = "deleteGenre",
        path = "/genres/{genre-id}",
        pathParams = [OpenApiParam(name = "genre-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "204"),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Delete Genre",
        tags = ["Genre"],
    )
    override fun delete(ctx: Context, resourceId: String): Unit = Utils.query {
        val genre = getResource(resourceId = resourceId)
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
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)],
            ),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Add Book to Genre",
        tags = ["Genre"],
    )
    fun addBook(ctx: Context): Unit = Utils.query {
        val genre = getResource(resourceId = ctx.pathParam("genre-id"))
        val body = ctx.getIdValue()
        val book = Book.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Book not found")
        if (book in genre.books) {
            throw ConflictResponse(message = "Book already is linked to Genre")
        }
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
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)],
            ),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Remove Book from Genre",
        tags = ["Genre"],
    )
    fun removeBook(ctx: Context): Unit = Utils.query {
        val genre = getResource(resourceId = ctx.pathParam("genre-id"))
        val body = ctx.getIdValue()
        val book = Book.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Book not found")
        if (!genre.books.contains(book)) {
            throw BadRequestResponse(message = "Book isn't linked to Genre")
        }
        val temp = genre.books.toMutableList()
        temp.remove(book)
        genre.books = SizedCollection(temp)

        ctx.json(genre.toJson(showAll = true))
    }
}
