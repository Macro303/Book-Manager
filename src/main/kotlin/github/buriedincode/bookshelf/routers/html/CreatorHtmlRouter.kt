package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Role
import io.javalin.http.Context
import org.apache.logging.log4j.kotlin.Logging

object CreatorHtmlRouter : BaseHtmlRouter<Creator>(entity = Creator, plural = "creators"), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = entity.all().toList()
            val name = ctx.queryParam("name")
            name?.let {
                resources = resources.filter {
                    it.name.contains(name, ignoreCase = true) || name.contains(it.name, ignoreCase = true)
                }
            }
            ctx.render(
                filePath = "templates/${super.name}/list.kte",
                model = mapOf(
                    "resources" to resources,
                    "session" to ctx.getSession(),
                    "selected" to mapOf(
                        "name" to name,
                    ),
                ),
            )
        }
    }

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val session = ctx.getSession()
            if (session == null || session.role < 2) {
                ctx.redirect(location = "/$plural")
            } else {
                ctx.render(
                    filePath = "templates/$name/create.kte",
                    model = mapOf(
                        "session" to session,
                        "books" to Book.all().toList(),
                        "roles" to Role.all().toList(),
                    ),
                )
            }
        }
    }

    override fun viewEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val credits = HashMap<Role, List<Book>>()
            for (entry in resource.credits) {
                var temp = credits.getOrDefault(entry.role, ArrayList())
                temp = temp.plus(entry.book)
                credits[entry.role] = temp
            }
            ctx.render(
                filePath = "templates/$name/view.kte",
                model = mapOf(
                    "resource" to resource,
                    "session" to ctx.getSession(),
                    "credits" to credits,
                ),
            )
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val session = ctx.getSession()
            if (session == null || session.role < 2) {
                ctx.redirect(location = "/$plural/${ctx.pathParam(paramName)}")
            } else {
                ctx.render(
                    filePath = "templates/$name/update.kte",
                    model = mapOf(
                        "resource" to ctx.getResource(),
                        "session" to session,
                        "books" to Book.all().toList(),
                        "roles" to Role.all().toList(),
                    ),
                )
            }
        }
    }
}
