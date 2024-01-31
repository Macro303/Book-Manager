package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.asEnumOrNull
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Format
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.Series
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.routers.html.BookHtmlRouter.getSession
import io.javalin.http.Context
import org.apache.logging.log4j.kotlin.Logging

object UserHtmlRouter : BaseHtmlRouter<User>(entity = User, plural = "users"), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = User.all().toList()
            val username = ctx.queryParam(key = "username")
            if (username != null) {
                resources = resources.filter {
                    it.username.contains(username, ignoreCase = true) || username.contains(it.username, ignoreCase = true)
                }
            }
            ctx.render(
                filePath = "templates/$name/list.kte",
                model = mapOf(
                    "session" to ctx.getSession(),
                    "resources" to resources,
                    "selected" to mapOf(
                        "username" to username,
                    ),
                ),
            )
        }
    }

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            ctx.render(
                filePath = "templates/$name/create.kte",
                model = mapOf(
                    "session" to ctx.getSession(),
                ),
            )
        }
    }

    override fun viewEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            ctx.render(
                filePath = "templates/$name/view.kte",
                model = mapOf(
                    "session" to ctx.getSession(),
                    "resource" to resource,
                    "stats" to mapOf(
                        "wishlist" to Book.all().count { !it.isCollected && resource in it.wishers },
                        "shared" to Book.all().count { !it.isCollected && it.wishers.empty() },
                        "unread" to Book.all().count { it.isCollected } - resource.readBooks.count(),
                        "read" to resource.readBooks.count(),
                    ),
                ),
            )
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val session = ctx.getSession()
            val resource = ctx.getResource()
            if (session == null) {
                ctx.redirect("/$plural/${resource.id.value}")
            } else {
                val readBooks = Book.all().toList()
                    .filter { it.isCollected }
                    .filterNot { it in resource.readBooks.map { it.book } }
                val wishedBooks = Book.all().toList()
                    .filterNot { it.isCollected }
                    .filterNot { it in resource.wishedBooks }
                ctx.render(
                    filePath = "templates/$name/update.kte",
                    model = mapOf(
                        "session" to session,
                        "resource" to resource,
                        "readBooks" to readBooks,
                        "wishedBooks" to wishedBooks,
                    ),
                )
            }
        }
    }

    fun wishlist(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            var books = Book.all().toList().filter {
                !it.isCollected && (it.wishers.empty() || resource in it.wishers)
            }
            val creator = ctx.queryParam("creator-id")?.toLongOrNull()?.let { Creator.findById(it) }
            creator?.let {
                books = books.filter { creator in it.credits.map { it.creator } }
            }
            val format = ctx.queryParam("format")?.asEnumOrNull<Format>()
            format?.let {
                books = books.filter { format == it.format }
            }
            val genre = ctx.queryParam("genre-id")?.toLongOrNull()?.let { Genre.findById(it) }
            genre?.let {
                books = books.filter { genre in it.genres }
            }
            val publisher = ctx.queryParam("publisher-id")?.toLongOrNull()?.let { Publisher.findById(it) }
            publisher?.let {
                books = books.filter { publisher == it.publisher }
            }
            val series = ctx.queryParam("series-id")?.toLongOrNull()?.let { Series.findById(it) }
            series?.let {
                books = books.filter { series in it.series.map { it.series } }
            }
            val title = ctx.queryParam("title")
            title?.let {
                books = books.filter {
                    (
                        it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
                    ) || (
                        it.subtitle?.let {
                            it.contains(title, ignoreCase = true) || title.contains(it, ignoreCase = true)
                        } ?: false
                    )
                }
            }
            ctx.render(
                filePath = "templates/$name/wishlist.kte",
                model = mapOf(
                    "session" to ctx.getSession(),
                    "resource" to resource,
                    "books" to books,
                    "selected" to mapOf(
                        "creator" to creator,
                        "format" to format,
                        "genre" to genre,
                        "publisher" to publisher,
                        "series" to series,
                        "title" to title,
                    ),
                ),
            )
        }
    }

    fun createBook(ctx: Context) {
        Utils.query {
            val session = ctx.getSession()
            val resource = ctx.getResource()
            if (session == null) {
                ctx.redirect("/$plural/${resource.id.value}/wishlist")
            } else {
                ctx.render(
                    filePath = "templates/$name/create_book.kte",
                    model = mapOf(
                        "session" to session,
                        "resource" to resource,
                        "formats" to Format.entries.toList(),
                        "publishers" to Publisher.all().toList(),
                    ),
                )
            }
        }
    }

    fun importBook(ctx: Context) {
        Utils.query {
            val session = ctx.getSession()
            val resource = ctx.getResource()
            if (session == null) {
                ctx.redirect("/$plural/${resource.id.value}/wishlist")
            } else {
                ctx.render(
                    filePath = "templates/$name/import_book.kte",
                    model = mapOf(
                        "session" to session,
                        "resource" to resource,
                    ),
                )
            }
        }
    }
}
