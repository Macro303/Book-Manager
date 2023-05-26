package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.*
import io.javalin.http.*
import org.jetbrains.exposed.dao.load

object GenreHtmlRouter {
    fun listEndpoint(ctx: Context) = Utils.query {
        val genres = Genre.all().toList()
        ctx.render(filePath = "templates/genre/list_genres.kte", mapOf("genres" to genres))
    }

    fun viewEndpoint(ctx: Context) = Utils.query {
        val genreId = ctx.pathParam("genre-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "Genre not found")
        val genre = Genre.findById(id = genreId)
            ?.load(Genre::books)
            ?: throw NotFoundResponse(message = "Genre not found")
        ctx.render(filePath = "templates/genre/view_genre.kte", mapOf("genre" to genre))
    }

    fun editEndpoint(ctx: Context) = Utils.query {
        val genreId = ctx.pathParam("genre-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "Genre not found")
        val genre = Genre.findById(id = genreId)
            ?.load(Genre::books)
            ?: throw NotFoundResponse(message = "Genre not found")
        val books = Book.all().toList().filterNot { it in genre.books }
        ctx.render(filePath = "templates/genre/edit_genre.kte", mapOf(
            "genre" to genre,
            "books" to books
        ))
    }
}