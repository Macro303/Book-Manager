package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Genre
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.dao.load

object GenreHtmlRouter {
    fun listEndpoint(ctx: Context) = Utils.query {
        val genres = Genre.all().toList()
        ctx.render(filePath = "templates/genre/list.kte", mapOf("genres" to genres))
    }

    fun viewEndpoint(ctx: Context) = Utils.query {
        val genreId = ctx.pathParam("genre-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "Genre not found")
        val genre = Genre.findById(id = genreId)
            ?.load(Genre::books)
            ?: throw NotFoundResponse(message = "Genre not found")
        ctx.render(filePath = "templates/genre/view.kte", mapOf("genre" to genre))
    }

    fun editEndpoint(ctx: Context) = Utils.query {
        val genreId = ctx.pathParam("genre-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "Genre not found")
        val genre = Genre.findById(id = genreId)
            ?.load(Genre::books)
            ?: throw NotFoundResponse(message = "Genre not found")
        val books = Book.all().toList().filterNot { it in genre.books }
        ctx.render(
            filePath = "templates/genre/edit.kte", mapOf(
                "genre" to genre,
                "books" to books
            )
        )
    }
}