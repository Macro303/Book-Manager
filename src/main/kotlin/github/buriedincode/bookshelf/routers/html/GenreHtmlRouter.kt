package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.User
import io.javalin.http.Context
import org.apache.logging.log4j.kotlin.Logging

object GenreHtmlRouter : BaseHtmlRouter<Genre>(entity = Genre), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = entity.all().toList()
            val title = ctx.queryParam(key = "title")
            title?.let {
                resources = resources.filter {
                    it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
                }
            }
            ctx.render(
                filePath = "templates/$name/list.kte",
                model = mapOf(
                    "resources" to resources,
                    "session" to ctx.attribute<User>("session"),
                    "selected" to mapOf(
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
                    "books" to Book.all().toList(),
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
                    "session" to ctx.attribute<User>("session")!!,
                    "books" to Book.all().toList().filterNot { it in resource.books },
                ),
            )
        }
    }
}
