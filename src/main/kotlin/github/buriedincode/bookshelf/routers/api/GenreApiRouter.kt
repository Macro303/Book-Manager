package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.GenreInput
import github.buriedincode.bookshelf.tables.GenreTable
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyValidator
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection

object GenreApiRouter : BaseApiRouter<Genre>(entity = Genre), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = Genre.all().toList()
            ctx.queryParam("book-id")?.toLongOrNull()?.let {
                Book.findById(it)?.let { book ->
                    resources = resources.filter { book in it.books }
                }
            }
            ctx.queryParam("title")?.let { title ->
                resources = resources.filter { it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true) }
            }
            ctx.json(resources.sorted().map { it.toJson() })
        }
    }

    private fun Context.getInput(): GenreInput =
        this.bodyValidator<GenreInput>()
            .check({ it.title.isNotBlank() }, error = "Title must not be empty")
            .get()

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val input = ctx.getInput()
            val exists = Genre.find {
                GenreTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse(message = "Genre already exists")
            }
            val genre = Genre.new {
                books = SizedCollection(
                    input.bookIds.map {
                        Book.findById(id = it)
                            ?: throw NotFoundResponse(message = "Unable to find Book: `$it`")
                    },
                )
                summary = input.summary
                title = input.title
            }

            ctx.status(HttpStatus.CREATED).json(genre.toJson(showAll = true))
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getInput()
            val exists = Genre.find {
                GenreTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse(message = "Genre already exists")
            }
            resource.books = SizedCollection(
                input.bookIds.map {
                    Book.findById(id = it)
                        ?: throw NotFoundResponse(message = "Unable to find Book: `$it`")
                },
            )
            resource.summary = input.summary
            resource.title = input.title

            ctx.json(resource.toJson(showAll = true))
        }
    }
}
