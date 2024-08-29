package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.IdInput
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.PublisherInput
import github.buriedincode.bookshelf.tables.BookTable
import github.buriedincode.bookshelf.tables.PublisherTable
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll

object PublisherApiRouter : BaseApiRouter<Publisher>(entity = Publisher) {
    override fun list(ctx: Context): Unit = Utils.query {
        val query = PublisherTable.selectAll()
        ctx.queryParam("book-id")?.toLongOrNull()?.let {
            Book.findById(it)?.let { book -> query.andWhere { BookTable.id eq book.id } }
        }
        ctx.queryParam("title")?.let { title ->
            query.andWhere { PublisherTable.titleCol like "%$title%" }
        }
        ctx.json(Publisher.wrapRows(query.withDistinct()).toList().sorted().map { it.toJson() })
    }

    override fun create(ctx: Context) = ctx.processInput<PublisherInput> { body ->
        Utils.query {
            Publisher.find(body.title)?.let {
                throw ConflictResponse("Publisher already exists")
            }
            val resource = Publisher.findOrCreate(body.title)
            ctx.status(HttpStatus.CREATED).json(resource.toJson(showAll = true))
        }
    }

    override fun update(ctx: Context) = manage<PublisherInput>(ctx) { body, publisher ->
        Publisher.find(body.title)?.takeIf { it != publisher }?.let { throw ConflictResponse("Publisher already exists") }
        publisher.apply {
            title = body.title
        }
    }

    fun addBook(ctx: Context) = manage<IdInput>(ctx) { body, publisher ->
        val book = Book.findById(body.id) ?: throw NotFoundResponse("Book not found.")
        book.publisher = publisher
    }

    fun removeBook(ctx: Context) = manage<IdInput>(ctx) { body, publisher ->
        val book = Book.findById(body.id) ?: throw NotFoundResponse("Book not found.")
        book.publisher = null
    }
}
