package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.User
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass

abstract class BaseHtmlRouter<T : LongEntity>(protected val entity: LongEntityClass<T>, protected val plural: String) {
    protected val name: String = entity::class.java.declaringClass.simpleName.lowercase()
    protected val paramName: String = "$name-id"
    protected val title: String = name.replaceFirstChar(Char::uppercaseChar)

    companion object : Logging

    protected fun Context.getResource(): T {
        return this.pathParam(paramName).toLongOrNull()?.let {
            entity.findById(id = it) ?: throw NotFoundResponse(message = "$title not found")
        } ?: throw BadRequestResponse(message = "Invalid $title Id")
    }

    protected fun Context.getSession(): User? {
        val cookie = this.cookie(name = "bookshelf_session-id")?.toLongOrNull() ?: -1L
        return User.findById(id = cookie)
    }

    abstract fun listEndpoint(ctx: Context)

    open fun createEndpoint(ctx: Context) {
        Utils.query {
            val session = ctx.getSession()
            if (session == null || session.role < 2) {
                ctx.redirect(location = "/$plural")
            } else {
                ctx.render(
                    filePath = "templates/$name/create.kte",
                    model = mapOf(
                        "session" to session,
                    ),
                )
            }
        }
    }

    open fun viewEndpoint(ctx: Context) {
        Utils.query {
            ctx.render(
                filePath = "templates/$name/view.kte",
                model = mapOf(
                    "resource" to ctx.getResource(),
                    "session" to ctx.getSession(),
                ),
            )
        }
    }

    open fun updateEndpoint(ctx: Context) {
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
                    ),
                )
            }
        }
    }
}
