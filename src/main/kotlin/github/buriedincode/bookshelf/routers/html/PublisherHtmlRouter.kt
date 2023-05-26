package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.*
import io.javalin.http.*
import org.jetbrains.exposed.dao.load

object PublisherHtmlRouter {
    fun listEndpoint(ctx: Context) = Utils.query {
        val publishers = Publisher.all().toList()
        ctx.render(filePath = "templates/publisher/list_publishers.kte", mapOf("publishers" to publishers))
    }

    fun viewEndpoint(ctx: Context) = Utils.query {
        val publisherId = ctx.pathParam("publisher-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "Publisher not found")
        val publisher = Publisher.findById(id = publisherId)
            ?.load(Publisher::books)
            ?: throw NotFoundResponse(message = "Publisher not found")
        ctx.render(filePath = "templates/publisher/view_publisher.kte", mapOf("publisher" to publisher))
    }

    fun editEndpoint(ctx: Context) = Utils.query {
        val publisherId = ctx.pathParam("publisher-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "Publisher not found")
        val publisher = Publisher.findById(id = publisherId)
            ?.load(Publisher::books)
            ?: throw NotFoundResponse(message = "Publisher not found")
        val books = Book.all().toList().filterNot { it in publisher.books }
        ctx.render(filePath = "templates/publisher/edit_publisher.kte", mapOf(
            "publisher" to publisher,
            "books" to books
        ))
    }
}