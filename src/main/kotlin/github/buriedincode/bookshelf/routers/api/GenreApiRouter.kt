package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.GenreInput
import github.buriedincode.bookshelf.models.IdInput
import github.buriedincode.bookshelf.tables.GenreTable
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyAsClass
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

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val body = ctx.bodyAsClass<GenreInput>()
            val exists = Genre.find {
                GenreTable.titleCol eq body.title
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse("Genre already exists")
            }
            val resource = Genre.new {
                summary = body.summary
                title = body.title
            }

            ctx.status(HttpStatus.CREATED).json(resource.toJson(showAll = true))
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<GenreInput>()
            val exists = Genre.find {
                GenreTable.titleCol eq body.title
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse("Genre already exists")
            }
            resource.summary = body.summary
            resource.title = body.title

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun addBook(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<IdInput>()

            val book = Book.findById(body.id) ?: throw NotFoundResponse("Book not found.")
            val temp = resource.books.toMutableSet()
            temp.add(book)
            resource.books = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun removeBook(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<IdInput>()

            val book = Book.findById(body.id) ?: throw NotFoundResponse("Book not found.")
            val temp = resource.books.toMutableSet()
            temp.remove(book)
            resource.books = SizedCollection(temp)

            ctx.status(HttpStatus.NO_CONTENT)
        }
    }
}
