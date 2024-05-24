package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Role
import io.javalin.http.Context
import org.apache.logging.log4j.kotlin.Logging

object RoleHtmlRouter : BaseHtmlRouter<Role>(entity = Role, plural = "roles"), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = Role.all().toList()
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
                        "books" to Book.all().toList(),
                        "creators" to Creator.all().toList(),
                    ),
                )
            }
        }
    }

    override fun viewEndpoint(ctx: Context) {
        Utils.query {
            val session = ctx.getSession()
            val resource = ctx.getResource()
            val credits = HashMap<Creator, List<Book>>()
            for (entry in resource.credits) {
                var temp = credits.getOrDefault(entry.creator, ArrayList())
                temp = temp.plus(entry.book)
                credits[entry.creator] = temp
            }
            ctx.render(
                filePath = "templates/$name/view.kte",
                model = mapOf(
                    "session" to session,
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
                        "books" to Book.all().toList(),
                        "creators" to Creator.all().toList(),
                    ),
                )
            }
        }
    }
}
