package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.dao.load
import org.jetbrains.kotlin.utils.addToStdlib.getOrPut
import org.jetbrains.kotlin.utils.keysToMap

object RoleHtmlRouter {
    private fun Context.getRole(): Role {
        return this.pathParam("role-id").toLongOrNull()?.let {
            Role.findById(id = it)
                ?.load(Role::bookCreators)
                ?: throw NotFoundResponse(message = "Role not found")
        } ?: throw BadRequestResponse(message = "Invalid Role Id")
    }

    fun listEndpoint(ctx: Context) = Utils.query {
        val roles = Role.all().toList()
        ctx.render(filePath = "templates/role/list.kte", mapOf("roles" to roles))
    }

    fun viewEndpoint(ctx: Context) = Utils.query {
        val role = ctx.getRole()
        val creators = HashMap<Creator, List<Book>>()
        for (entry in role.bookCreators) {
            var temp = creators.getOrDefault(entry.creator, ArrayList())
            temp = temp.plus(entry.book)
            creators[entry.creator] = temp
        }
        ctx.render(filePath = "templates/role/view.kte", mapOf("role" to role, "creators" to creators))
    }

    fun editEndpoint(ctx: Context) = Utils.query {
        val role = ctx.getRole()
        ctx.render(filePath = "templates/role/edit.kte", mapOf("role" to role))
    }
}