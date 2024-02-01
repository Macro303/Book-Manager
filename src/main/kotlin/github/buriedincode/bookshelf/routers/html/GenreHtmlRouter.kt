package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Genre
import io.javalin.http.Context
import org.apache.logging.log4j.kotlin.Logging

object GenreHtmlRouter : BaseHtmlRouter<Genre>(entity = Genre, plural = "genres"), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = entity.all().toList()
            val title = ctx.queryParam(key = "title")
            if (title != null) {
                resources = resources.filter {
                    it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
                }
            }
            ctx.render(
                filePath = "templates/$name/list.kte",
                model = mapOf(
                    "session" to ctx.getSession(),
                    "resources" to resources,
                    "selected" to mapOf(
                        "title" to title,
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
                ctx.render(
                    filePath = "templates/$name/update.kte",
                    model = mapOf(
                        "session" to session,
                        "resource" to resource,
                        "books" to Book.all().toList().filterNot { it in resource.books },
                    ),
                )
            }
        }
    }
}
