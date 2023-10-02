package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.PublisherInput
import github.buriedincode.bookshelf.tables.PublisherTable
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyValidator
import org.apache.logging.log4j.kotlin.Logging

object PublisherApiRouter : Logging {
    fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            var publishers = Publisher.all().toList()
            val title = ctx.queryParam("title")
            if (title != null) {
                publishers = publishers.filter {
                    it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
                }
            }
            ctx.json(publishers.sorted().map { it.toJson() })
        }

    private fun Context.getInput(): PublisherInput =
        this.bodyValidator<PublisherInput>()
            .check({ it.title.isNotBlank() }, error = "Title must not be empty")
            .get()

    fun createEndpoint(ctx: Context): Unit =
        Utils.query {
            val input = ctx.getInput()
            val exists = Publisher.find {
                PublisherTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse(message = "Publisher already exists")
            }
            val publisher = Publisher.new {
                title = input.title
            }

            ctx.status(HttpStatus.CREATED).json(publisher.toJson(showAll = true))
        }

    private fun Context.getResource(): Publisher {
        return this.pathParam("publisher-id").toLongOrNull()?.let {
            Publisher.findById(id = it) ?: throw NotFoundResponse(message = "Unable to find Publisher: `$it`")
        } ?: throw BadRequestResponse(message = "Unable to find Publisher: `${this.pathParam("publisher-id")}`")
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
            val exists = Publisher.find {
                PublisherTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse(message = "Publisher already exists")
            }
            resource.title = input.title

            ctx.json(resource.toJson(showAll = true))
        }

    fun deleteEndpoint(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            resource.delete()
            ctx.status(HttpStatus.NO_CONTENT)
        }
}
