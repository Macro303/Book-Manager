package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.IdInput
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.PublisherInput
import github.buriedincode.bookshelf.tables.PublisherTable
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyAsClass
import org.apache.logging.log4j.kotlin.Logging

object PublisherApiRouter : BaseApiRouter<Publisher>(entity = Publisher), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = Publisher.all().toList()
            ctx.queryParam("book-id")?.toLongOrNull()?.let {
                Book.findById(it)?.let { book ->
                    resources = resources.filter { book in it.books }
                }
            }
            ctx.queryParam("has-image")?.lowercase()?.toBooleanStrictOrNull()?.let { image ->
                resources = resources.filter { it.image != null == image }
            }
            ctx.queryParam("title")?.let { title ->
                resources = resources.filter { it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true) }
            }
            ctx.json(resources.sorted().map { it.toJson() })
        }
    }

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val body = ctx.bodyAsClass<PublisherInput>()
            val exists = Publisher.find {
                PublisherTable.titleCol eq body.title
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse("Publisher already exists")
            }
            val resource = Publisher.new {
                this.image = body.image
                this.summary = body.summary
                this.title = body.title
            }
            body.bookIds.map {
                Book.findById(it) ?: throw NotFoundResponse("No Book found.")
            }.forEach {
                it.publisher = resource
            }

            ctx.status(HttpStatus.CREATED).json(resource.toJson(showAll = true))
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<PublisherInput>()
            val exists = Publisher.find {
                PublisherTable.titleCol eq body.title
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse("Publisher already exists")
            }
            resource.image = body.image
            resource.summary = body.summary
            resource.title = body.title
            body.bookIds.map {
                Book.findById(it) ?: throw NotFoundResponse("No Book found.")
            }.forEach {
                it.publisher = resource
            }

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun addBook(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<IdInput>()
            val book = Book.findById(body.id)
                ?: throw NotFoundResponse("No Book found.")
            book.publisher = resource

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun removeBook(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<IdInput>()
            val book = Book.findById(body.id)
                ?: throw NotFoundResponse("No Book found.")
            book.publisher = null

            ctx.status(HttpStatus.NO_CONTENT)
        }
    }
}
