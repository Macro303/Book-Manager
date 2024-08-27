package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Role
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.Context
import io.javalin.http.NotImplementedResponse

object RoleHtmlRouter : BaseHtmlRouter<Role>(entity = Role, plural = "roles") {
    @JvmStatic
    private val LOGGER = KotlinLogging.logger { }

    override fun list(ctx: Context) = throw NotImplementedResponse()

    override fun create(ctx: Context) = throw NotImplementedResponse()

    override fun view(ctx: Context) = Utils.query {
        renderResource(ctx, "view", mapOf("credits" to ctx.getResource().credits.groupBy({ it.creator }, { it.book })), redirect = false)
    }
}
