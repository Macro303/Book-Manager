package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.GenreInput
import github.buriedincode.bookshelf.models.IdValue
import github.buriedincode.bookshelf.tables.GenreTable
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyValidator
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection

object GenreApiRouter : Logging {
    fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            var genres = Genre.all().toList()
            val title = ctx.queryParam("title")
            if (title != null) {
                genres = genres.filter {
                    it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
                }
            }
            ctx.json(genres.sorted().map { it.toJson() })
        }

    private fun Context.getInput(): GenreInput =
        this.bodyValidator<GenreInput>()
            .check({ it.title.isNotBlank() }, error = "Title must not be empty")
            .get()

    fun createEndpoint(ctx: Context): Unit =
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
                title = input.title
            }

            ctx.status(HttpStatus.CREATED).json(genre.toJson(showAll = true))
        }

    private fun Context.getResource(): Genre {
        return this.pathParam("genre-id").toLongOrNull()?.let {
            Genre.findById(id = it) ?: throw NotFoundResponse(message = "Unable to find Genre: `$it`")
        } ?: throw BadRequestResponse(message = "Unable to find Genre: `${this.pathParam("genre-id")}`")
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
            resource.title = input.title

            ctx.json(resource.toJson(showAll = true))
        }

    fun deleteEndpoint(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            resource.delete()
            ctx.status(HttpStatus.NO_CONTENT)
        }

    private fun Context.getIdValue(): IdValue =
        this.bodyValidator<IdValue>()
            .check({ it.id > 0 }, error = "Id must be greater than 0")
            .get()

    fun addBook(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getIdValue()
            val book = Book.findById(id = input.id)
                ?: throw NotFoundResponse(message = "Unable to find Book: `${input.id}`")
            val temp = resource.books.toMutableSet()
            temp.add(book)
            resource.books = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }

    fun removeBook(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getIdValue()
            val book = Book.findById(id = input.id)
                ?: throw NotFoundResponse(message = "Unable to find Book: `${input.id}`")
            if (!resource.books.contains(book)) {
                throw BadRequestResponse(message = "Book isn't linked to Genre")
            }
            val temp = resource.books.toMutableList()
            temp.remove(book)
            resource.books = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }
}
