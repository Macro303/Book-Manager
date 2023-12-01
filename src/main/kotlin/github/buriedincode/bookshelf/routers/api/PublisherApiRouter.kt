package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.PublisherInput
import github.buriedincode.bookshelf.tables.PublisherTable
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.bodyValidator
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

    private fun Context.getInput(): PublisherInput =
        this.bodyValidator<PublisherInput>()
            .check({ it.title.isNotBlank() }, error = "Title must not be empty")
            .get()

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val input = ctx.getInput()
            val exists = Publisher.find {
                PublisherTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse(message = "Publisher already exists")
            }
            val publisher = Publisher.new {
                image = input.image
                summary = input.summary
                title = input.title
            }

            ctx.status(HttpStatus.CREATED).json(publisher.toJson(showAll = true))
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getInput()
            val exists = Publisher.find {
                PublisherTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse(message = "Publisher already exists")
            }
            resource.image = input.image
            resource.summary = input.summary
            resource.title = input.title

            ctx.json(resource.toJson(showAll = true))
        }
    }
}
