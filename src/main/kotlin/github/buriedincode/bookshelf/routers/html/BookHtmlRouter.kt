package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.*
import io.javalin.http.*
import org.jetbrains.exposed.dao.load

object BookHtmlRouter {
    fun listEndpoint(ctx: Context) = Utils.query {
        val books = Book.all().toList()
        ctx.render(filePath = "templates/book/list.kte", mapOf("books" to books))
    }

    fun viewEndpoint(ctx: Context) = Utils.query {
        val bookId = ctx.pathParam("book-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "Book not found")
        val book = Book.findById(id = bookId)
            ?.load(Book::genres, Book::publisher, Book::readers, Book::series, Book::wishers)
            ?: throw NotFoundResponse(message = "Book not found")
        ctx.render(filePath = "templates/book/view.kte", mapOf("book" to book))
    }

    fun editEndpoint(ctx: Context) = Utils.query {
        val bookId = ctx.pathParam("book-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "Book not found")
        val book = Book.findById(id = bookId)
            ?.load(Book::genres, Book::publisher, Book::readers, Book::series, Book::wishers)
            ?: throw NotFoundResponse(message = "Book not found")
        val genres = Genre.all().toList().filterNot { it in book.genres }
        val publishers = Publisher.all().toList().filterNot { it == book.publisher }
        val series = Series.all().toList().filterNot { it in book.series.map { it.series } }
        ctx.render(filePath = "templates/book/edit.kte", mapOf(
            "book" to book,
            "genres" to genres,
            "publishers" to publishers,
            "series" to series
        ))
    }
}