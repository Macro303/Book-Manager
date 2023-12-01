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
import github.buriedincode.bookshelf.models.User
import io.javalin.http.Context
import org.apache.logging.log4j.kotlin.Logging

object BookHtmlRouter : BaseHtmlRouter<Book>(entity = Book), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = entity.all().toList().filter { it.isCollected }
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
                    "resources" to resources,
                    "session" to ctx.attribute<User>("session"),
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
            ctx.render(
                filePath = "templates/$name/create.kte",
                model = mapOf(
                    "session" to ctx.attribute<User>("session")!!,
                    "creators" to Creator.all().toList(),
                    "formats" to Format.entries.toList(),
                    "genres" to Genre.all().toList(),
                    "publishers" to Publisher.all().toList(),
                    "readers" to User.all().toList(),
                    "roles" to Role.all().toList(),
                    "series" to Series.all().toList(),
                    "wishers" to User.all().toList(),
                ),
            )
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
                    "resource" to resource,
                    "session" to ctx.attribute<User>("session"),
                    "credits" to credits,
                ),
            )
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            ctx.render(
                filePath = "templates/$name/update.kte",
                model = mapOf(
                    "resource" to resource,
                    "session" to ctx.attribute<User>("session"),
                    "creators" to Creator.all().toList(),
                    "formats" to Format.entries.toList(),
                    "genres" to Genre.all().toList().filterNot { it in resource.genres },
                    "publishers" to Publisher.all().toList().filterNot { it == resource.publisher },
                    "readers" to User.all().toList().filterNot { it in resource.readers.map { it.user } },
                    "roles" to Role.all().toList(),
                    "series" to Series.all().toList().filterNot { it in resource.series.map { it.series } },
                    "wishers" to User.all().toList().filterNot { it in resource.wishers },
                ),
            )
        }
    }
}
