package github.buriedincode.bookshelf.routers

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass

abstract class BaseHtmlRouter<T : LongEntity>(protected val entity: LongEntityClass<T>) {
    private val name: String = entity::class.simpleName!!.lowercase()
    protected val paramName: String = "$name-id"
    protected val title: String = name.replaceFirstChar(Char::uppercaseChar)

    protected fun Context.getParamObject(): T {
        return this.pathParam(paramName).toLongOrNull()?.let {
            entity.findById(id = it) ?: throw NotFoundResponse(message = "$title not found")
        } ?: throw BadRequestResponse(message = "Invalid $title Id")
    }

    open fun listEndpoint(ctx: Context): Unit = Utils.query {
        val results = entity.all().toList()
        ctx.render(filePath = "templates/$name/list.kte", mapOf("results" to results))
    }

    open fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val result = ctx.getParamObject()
        ctx.render(filePath = "templates/$name/view.kte", mapOf("result" to result))
    }

    open fun editEndpoint(ctx: Context): Unit = Utils.query {
        val result = ctx.getParamObject()
        ctx.render(filePath = "templates/$name/edit.kte", mapOf("result" to result))
    }
}

object BookHtmlRouter : BaseHtmlRouter<Book>(entity = Book), Logging {
    override fun listEndpoint(ctx: Context): Unit = Utils.query {
        val results = entity.all().toList().filter { it.isCollected }
        ctx.render(filePath = "templates/book/list.kte", mapOf("results" to results))
    }

    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val result = ctx.getParamObject()
        val credits = HashMap<Role, List<Creator>>()
        for (entry in result.credits) {
            var temp = credits.getOrDefault(entry.role, ArrayList())
            temp = temp.plus(entry.creator)
            credits[entry.role] = temp
        }
        ctx.render(
            filePath = "templates/book/view.kte", mapOf(
                "result" to result,
                "credits" to credits
            )
        )
    }

    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val result = ctx.getParamObject()
        val creators = Creator.all().toList()
        val genres = Genre.all().toList().filterNot { it in result.genres }
        val publishers = Publisher.all().toList().filterNot { it == result.publisher }
        val readers = User.all().toList().filterNot { it in result.readers }
        val roles = Role.all().toList()
        val series = Series.all().toList().filterNot { it in result.series.map { it.series } }
        val wishers = User.all().toList().filterNot { it in result.wishers }
        ctx.render(
            filePath = "templates/book/edit.kte", mapOf(
                "result" to result,
                "creators" to creators,
                "genres" to genres,
                "publishers" to publishers,
                "readers" to readers,
                "roles" to roles,
                "series" to series,
                "wishers" to wishers,
            )
        )
    }
}

object CreatorHtmlRouter : BaseHtmlRouter<Creator>(entity = Creator), Logging {
    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val result = ctx.getParamObject()
        val credits = HashMap<Role, List<Book>>()
        for (entry in result.credits) {
            var temp = credits.getOrDefault(entry.role, ArrayList())
            temp = temp.plus(entry.book)
            credits[entry.role] = temp
        }
        ctx.render(
            filePath = "templates/creator/view.kte", mapOf(
                "result" to result,
                "credits" to credits
            )
        )
    }

    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val result = ctx.getParamObject()
        val books = Book.all().toList()
        val roles = Role.all().toList()
        ctx.render(
            filePath = "templates/creator/edit.kte", mapOf(
                "result" to result,
                "books" to books,
                "roles" to roles
            )
        )
    }
}

object GenreHtmlRouter : BaseHtmlRouter<Genre>(entity = Genre), Logging {
    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val result = ctx.getParamObject()
        val books = Book.all().toList().filterNot { it in result.books }
        ctx.render(
            filePath = "templates/genre/edit.kte", mapOf(
                "result" to result,
                "books" to books
            )
        )
    }
}

object PublisherHtmlRouter : BaseHtmlRouter<Publisher>(entity = Publisher), Logging

object RoleHtmlRouter : BaseHtmlRouter<Role>(entity = Role), Logging {
    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val result = ctx.getParamObject()
        val credits = HashMap<Creator, List<Book>>()
        for (entry in result.credits) {
            var temp = credits.getOrDefault(entry.creator, ArrayList())
            temp = temp.plus(entry.book)
            credits[entry.creator] = temp
        }
        ctx.render(
            filePath = "templates/role/view.kte", mapOf(
                "result" to result,
                "credits" to credits
            )
        )
    }

    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val result = ctx.getParamObject()
        val books = Book.all().toList()
        val creators = Creator.all().toList()
        ctx.render(
            filePath = "templates/role/edit.kte", mapOf(
                "result" to result,
                "books" to books,
                "creators" to creators
            )
        )
    }
}

object SeriesHtmlRouter : BaseHtmlRouter<Series>(entity = Series), Logging {
    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val result = ctx.getParamObject()
        val books = Book.all().toList().filterNot { it in result.books.map { it.book } }
        ctx.render(
            filePath = "templates/series/edit.kte", mapOf(
                "result" to result,
                "books" to books
            )
        )
    }
}

object UserHtmlRouter : BaseHtmlRouter<User>(entity = User), Logging {
    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val result = ctx.getParamObject()
        val readBooks = Book.all().toList().filter { it.isCollected }.filterNot { it in result.readBooks }
        val wishedBooks = Book.all().toList().filterNot { it.isCollected }.filterNot { it in result.wishedBooks }
        ctx.render(
            filePath = "templates/user/edit.kte", mapOf(
                "result" to result,
                "readBooks" to readBooks,
                "wishedBooks" to wishedBooks
            )
        )
    }

    open fun wishlistEndpoint(ctx: Context): Unit = Utils.query {
        val result = ctx.getParamObject()
        val books = Book.all().toList().filter { !it.isCollected && (it.wishers.empty() || result in it.wishers) }
        ctx.render(
            filePath = "templates/user/wishlist.kte", mapOf(
                "result" to result,
                "books" to books,
            )
        )
    }
}