package github.buriedincode.bookshelf.controllers

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.GenreEntry
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.GenreInput
import io.javalin.http.*
import io.javalin.openapi.*
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection

object GenreController : Logging {
    @OpenApi(
        description = "List all Genres",
        methods = [HttpMethod.GET],
        operationId = "listGenres",
        path = "/genres",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = []),
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(Array<GenreEntry>::class)])],
        security = [],
        summary = "List all Genres",
        tags = ["Genre"]
    )
    fun listGenres(ctx: Context): Unit = Utils.query(description = "List Genres") {
        val results = Genre.all()
        ctx.json(results.map { it.toJson() })
    }

    @OpenApi(
        description = "Create Genre",
        methods = [HttpMethod.POST],
        operationId = "createGenre",
        path = "/genres",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(GenreInput::class)], required = true),
        responses = [
            OpenApiResponse(
                status = "201",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)]
            ),
        ],
        security = [],
        summary = "Create Genre",
        tags = ["Genre"]
    )
    fun createGenre(ctx: Context): Unit = Utils.query(description = "Create Genre") {
        val input: GenreInput
        try {
            input = ctx.bodyAsClass<GenreInput>()
        } catch (upe: UnrecognizedPropertyException) {
            throw BadRequestResponse(message = "Invalid Body: ${upe.message}")
        }
        val result = Genre.new {
            books = SizedCollection(input.bookIds.map {
                Book.findById(id = it) ?: throw NotFoundResponse(message = "Book not found")
            })
            title = input.title
        }

        ctx.status(HttpStatus.CREATED).json(result.toJson(showAll = true))
    }

    @OpenApi(
        description = "Get Genre by id",
        methods = [HttpMethod.GET],
        operationId = "getGenre",
        path = "/genres/{genre-id}",
        pathParams = [OpenApiParam(name = "genre-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)]
            ),
        ],
        security = [],
        summary = "Get Genre by id",
        tags = ["Genre"]
    )
    fun getGenre(ctx: Context): Unit = Utils.query(description = "Get Genre") {
        val genreId = ctx.pathParam("genre-id")
        val result = genreId.toLongOrNull()?.let {
            Genre.findById(id = it) ?: throw NotFoundResponse(message = "Genre not found")
        } ?: throw BadRequestResponse(message = "Invalid Genre Id")
        ctx.json(result.toJson(showAll = true))
    }

    @OpenApi(
        description = "Update Genre",
        methods = [HttpMethod.PUT],
        operationId = "updateGenre",
        path = "/genres/{genre-id}",
        pathParams = [OpenApiParam(name = "genre-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(GenreInput::class)], required = true),
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Genre::class)]
            ),
        ],
        security = [],
        summary = "Update Genre",
        tags = ["Genre"]
    )
    fun updateGenre(ctx: Context): Unit = Utils.query(description = "Update Genre") {
        val genreId = ctx.pathParam("genre-id")
        val input: GenreInput
        try {
            input = ctx.bodyAsClass<GenreInput>()
        } catch (upe: UnrecognizedPropertyException) {
            throw BadRequestResponse(message = "Invalid Body: ${upe.message}")
        }
        val result = genreId.toLongOrNull()?.let {
            Genre.findById(id = it) ?: throw NotFoundResponse(message = "Genre not found")
        } ?: throw BadRequestResponse(message = "Invalid Genre Id")
        result.books = SizedCollection(input.bookIds.map {
            Book.findById(id = it) ?: throw NotFoundResponse(message = "Book not found")
        })
        result.title = input.title

        ctx.json(result.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete Genre",
        methods = [HttpMethod.DELETE],
        operationId = "deleteGenre",
        path = "/genres/{genre-id}",
        pathParams = [OpenApiParam(name = "genre-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "204")
        ],
        security = [],
        summary = "Delete Genre",
        tags = ["Genre"]
    )
    fun deleteGenre(ctx: Context): Unit = Utils.query(description = "Delete Genre") {
        val genreId = ctx.pathParam("genre-id")
        val result = genreId.toLongOrNull()?.let {
            Genre.findById(id = it) ?: throw NotFoundResponse(message = "Genre not found")
        } ?: throw BadRequestResponse(message = "Invalid Genre Id")
        result.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }
}
