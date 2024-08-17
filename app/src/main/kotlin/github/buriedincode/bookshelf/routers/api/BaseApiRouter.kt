package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.IJson
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass

abstract class BaseApiRouter<T>(protected val entity: LongEntityClass<T>)
    where T : LongEntity, T : IJson {
    protected val name: String = entity::class.java.declaringClass.simpleName
        .lowercase()
    protected val paramName: String = "$name-id"
    protected val title: String = name.replaceFirstChar(Char::uppercaseChar)

    protected fun Context.getResource(): T = this.pathParam(paramName).toLongOrNull()?.let {
        entity.findById(it) ?: throw NotFoundResponse("$title not found")
    } ?: throw BadRequestResponse("Invalid $title Id")

    protected fun <I> Context.processInput(block: (I) -> Unit) = Utils.query { block(bodyAsClass()) }

    protected inline fun <reified I> manage(ctx: Context, block: (I, T) -> Unit) {
        ctx.processInput<I> { body ->
            val resource = ctx.getResource()
            block(body, resource)
            ctx.json(resource.toJson(showAll = true))
        }
    }

    abstract fun list(ctx: Context)

    abstract fun create(ctx: Context)

    open fun read(ctx: Context) = Utils.query {
        ctx.json(ctx.getResource().toJson(showAll = true))
    }

    abstract fun update(ctx: Context)

    open fun delete(ctx: Context) = Utils.query {
        ctx.getResource().delete()
        ctx.status(status = HttpStatus.NO_CONTENT)
    }
}
