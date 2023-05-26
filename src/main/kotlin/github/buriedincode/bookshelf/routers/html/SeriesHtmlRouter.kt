package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.*
import io.javalin.http.*
import org.jetbrains.exposed.dao.load

object SeriesHtmlRouter {
    fun listEndpoint(ctx: Context) = Utils.query {
        val series = Series.all().toList()
        ctx.render(filePath = "templates/series/list.kte", mapOf("series" to series))
    }

    fun viewEndpoint(ctx: Context) = Utils.query {
        val seriesId = ctx.pathParam("series-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "Series not found")
        val series = Series.findById(id = seriesId)
            ?.load(Series::books)
            ?: throw NotFoundResponse(message = "Series not found")
        ctx.render(filePath = "templates/series/view.kte", mapOf("series" to series))
    }

    fun editEndpoint(ctx: Context) = Utils.query {
        val seriesId = ctx.pathParam("series-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "Series not found")
        val series = Series.findById(id = seriesId)
            ?.load(Series::books)
            ?: throw NotFoundResponse(message = "Series not found")
        val books = Book.all().toList().filterNot { it in series.books.map { it.book } }
        ctx.render(filePath = "templates/series/edit.kte", mapOf(
            "series" to series,
            "books" to books
        ))
    }
}