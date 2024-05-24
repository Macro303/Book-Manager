package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.asEnumOrNull
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Format
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.models.Series
import io.javalin.http.Context
import org.apache.logging.log4j.kotlin.Logging

object BookHtmlRouter : BaseHtmlRouter<Book>(entity = Book, plural = "books"), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = Book.all().toList().filter { it.isCollected }
            val creator = ctx.queryParam("creator-id")?.toLongOrNull()?.let { Creator.findById(it) }
            creator?.let {
                resources = resources.filter { creator in it.credits.map { it.creator } }
            }
            val format = ctx.queryParam("format")?.asEnumOrNull<Format>()
            format?.let {
                resources = resources.filter { format == it.format }
            }
            val genre = ctx.queryParam("genre-id")?.toLongOrNull()?.let { Genre.findById(it) }
            genre?.let {
                resources = resources.filter { genre in it.genres }
            }
            val publisher = ctx.queryParam("publisher-id")?.toLongOrNull()?.let { Publisher.findById(it) }
            publisher?.let {
                resources = resources.filter { publisher == it.publisher }
            }
            val series = ctx.queryParam("series-id")?.toLongOrNull()?.let { Series.findById(it) }
            series?.let {
                resources = resources.filter { series in it.series.map { it.series } }
            }
            val title = ctx.queryParam("title")
            title?.let {
                resources = resources.filter {
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
                filePath = "templates/$name/list.kte",
                model = mapOf(
                    "session" to ctx.getSession(),
                    "resources" to resources,
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

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect("/$plural")
            } else {
                ctx.render(
                    filePath = "templates/$name/create.kte",
                    model = mapOf(
                        "session" to session,
                        "creators" to Creator.all().toList(),
                        "formats" to Format.entries.toList(),
                        "genres" to Genre.all().toList(),
                        "publishers" to Publisher.all().toList(),
                        "roles" to Role.all().toList(),
                        "series" to Series.all().toList(),
                    ),
                )
            }
        }
    }

    override fun viewEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val credits = HashMap<Role, List<Creator>>()
            for (entry in resource.credits) {
                var temp = credits.getOrDefault(entry.role, ArrayList())
                temp = temp.plus(entry.creator)
                credits[entry.role] = temp
            }
            ctx.render(
                filePath = "templates/$name/view.kte",
                model = mapOf(
                    "session" to ctx.getSession(),
                    "resource" to resource,
                    "credits" to credits,
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
                ctx.render(
                    filePath = "templates/$name/update.kte",
                    model = mapOf(
                        "session" to session,
                        "resource" to resource,
                        "creators" to Creator.all().toList(),
                        "formats" to Format.entries.toList(),
                        "genres" to Genre.all().toList().filterNot { it in resource.genres },
                        "publishers" to Publisher.all().toList(),
                        "roles" to Role.all().toList(),
                        "series" to Series.all().toList().filterNot { it in resource.series.map { it.series } },
                    ),
                )
            }
        }
    }

    fun import(ctx: Context) {
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect("/$plural")
            } else {
                ctx.render(
                    filePath = "templates/$name/import.kte",
                    model = mapOf("session" to session),
                )
            }
        }
    }
}
