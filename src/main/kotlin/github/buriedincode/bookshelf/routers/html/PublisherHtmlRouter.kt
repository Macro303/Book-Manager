package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Publisher
import io.javalin.http.Context
import org.apache.logging.log4j.kotlin.Logging

object PublisherHtmlRouter : BaseHtmlRouter<Publisher>(entity = Publisher, plural = "publishers"), Logging {
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
                    "session" to ctx.getSession(),
                    "selected" to mapOf(
                        "title" to title,
                    ),
                ),
            )
        }
    }
}
