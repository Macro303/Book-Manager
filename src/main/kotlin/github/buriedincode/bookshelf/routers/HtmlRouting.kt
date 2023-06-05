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
    private val name: String = entity::class.java.declaringClass.simpleName!!.lowercase()
    protected val paramName: String = "$name-id"
    protected val title: String = name.replaceFirstChar(Char::uppercaseChar)
    
    companion object: Logging

    protected fun getResource(resourceId: String): T {
        return resourceId.toLongOrNull()?.let {
            entity.findById(id = it) ?: throw NotFoundResponse(message = "$title not found")
        } ?: throw BadRequestResponse(message = "Invalid $title Id")
    }
    
    protected fun Context.getSession(): User? {
        val cookie = this.cookie("session-id")?.toLongOrNull() ?: -1L
        return User.findById(id = cookie)
    }

    open fun listEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resources = entity.all().toList()
            ctx.render(filePath = "templates/$name/list.kte", mapOf("resources" to resources, "session" to session))
        }
    }

    open fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = getResource(resourceId = ctx.pathParam(paramName))
            ctx.render(filePath = "templates/$name/view.kte", mapOf("resource" to resource, "session" to session))
       }
    }

    open fun editEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = getResource(resourceId = ctx.pathParam(paramName))
            ctx.render(filePath = "templates/$name/edit.kte", mapOf("resource" to resource, "session" to session))
        }
    }
}

object BookHtmlRouter : BaseHtmlRouter<Book>(entity = Book), Logging {
    override fun listEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resources = entity.all().toList().filter { it.isCollected }
            ctx.render(filePath = "templates/book/list.kte", mapOf("resources" to resources, "session" to session))
        }
    }

    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = getResource(resourceId = ctx.pathParam(paramName))
            val credits = HashMap<Role, List<Creator>>()
            for (entry in resource.credits) {
                var temp = credits.getOrDefault(entry.role, ArrayList())
                temp = temp.plus(entry.creator)
                credits[entry.role] = temp
            }
            ctx.render(
                filePath = "templates/book/view.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "credits" to credits
                )
            )
        }
    }

    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = getResource(resourceId = ctx.pathParam(paramName))
            val creators = Creator.all().toList()
            val genres = Genre.all().toList().filterNot { it in resource.genres }
            val publishers = Publisher.all().toList().filterNot { it == resource.publisher }
            val readers = User.all().toList().filterNot { it in resource.readers }
            val roles = Role.all().toList()
            val series = Series.all().toList().filterNot { it in resource.series.map { it.series } }
            val wishers = User.all().toList().filterNot { it in resource.wishers }
            ctx.render(
                filePath = "templates/book/edit.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
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
}

object CreatorHtmlRouter : BaseHtmlRouter<Creator>(entity = Creator), Logging {
    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = getResource(resourceId = ctx.pathParam(paramName))
            val credits = HashMap<Role, List<Book>>()
            for (entry in resource.credits) {
                var temp = credits.getOrDefault(entry.role, ArrayList())
                temp = temp.plus(entry.book)
                credits[entry.role] = temp
            }
            ctx.render(
                filePath = "templates/creator/view.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "credits" to credits
                )
            )
        }
    }

    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = getResource(resourceId = ctx.pathParam(paramName))
            val books = Book.all().toList()
            val roles = Role.all().toList()
            ctx.render(
                filePath = "templates/creator/edit.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "books" to books,
                    "roles" to roles
                )
            )
        }
    }
}

object GenreHtmlRouter : BaseHtmlRouter<Genre>(entity = Genre), Logging {
    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = getResource(resourceId = ctx.pathParam(paramName))
            val books = Book.all().toList().filterNot { it in resource.books }
            ctx.render(
                filePath = "templates/genre/edit.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "books" to books
                )
            )
        }
    }
}

object PublisherHtmlRouter : BaseHtmlRouter<Publisher>(entity = Publisher), Logging

object RoleHtmlRouter : BaseHtmlRouter<Role>(entity = Role), Logging {
    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = getResource(resourceId = ctx.pathParam(paramName))
            val credits = HashMap<Creator, List<Book>>()
            for (entry in resource.credits) {
                var temp = credits.getOrDefault(entry.creator, ArrayList())
                temp = temp.plus(entry.book)
                credits[entry.creator] = temp
            }
            ctx.render(
                filePath = "templates/role/view.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "credits" to credits
                )
            )
        }
    }

    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = getResource(resourceId = ctx.pathParam(paramName))
            val books = Book.all().toList()
            val creators = Creator.all().toList()
            ctx.render(
                filePath = "templates/role/edit.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "books" to books,
                    "creators" to creators
                )
            )
        }
    }
}

object SeriesHtmlRouter : BaseHtmlRouter<Series>(entity = Series), Logging {
    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = getResource(resourceId = ctx.pathParam(paramName))
            val books = Book.all().toList().filterNot { it in resource.books.map { it.book } }
            ctx.render(
                filePath = "templates/series/edit.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "books" to books
                )
            )
        }
    }
}

object UserHtmlRouter : BaseHtmlRouter<User>(entity = User), Logging {
    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = getResource(resourceId = ctx.pathParam(paramName))
            val readListSize = resource.readBooks.count()
            val stats = mapOf(
                "wishlist" to Book.all().filter { !it.isCollected && resource in it.wishers }.count(),
                "shared" to Book.all().filter { !it.isCollected && it.wishers.empty() }.count(),
                "unread" to Book.all().filter { it.isCollected }.count() - readListSize,
                "read" to readListSize
            )
            ctx.render(filePath = "templates/user/view.kte", mapOf("resource" to resource, "session" to session, "stats" to stats))
       }
    }
    
    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = getResource(resourceId = ctx.pathParam(paramName))
            val readBooks = Book.all().toList().filter { it.isCollected }.filterNot { it in resource.readBooks }
            val wishedBooks = Book.all().toList().filterNot { it.isCollected }.filterNot { it in resource.wishedBooks }
            ctx.render(
                filePath = "templates/user/edit.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "readBooks" to readBooks,
                    "wishedBooks" to wishedBooks
                )
            )
        }
    }

    fun wishlistEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = getResource(resourceId = ctx.pathParam(paramName))
            val books = Book.all().toList().filter { !it.isCollected && (it.wishers.empty() || resource in it.wishers) }
            ctx.render(
                filePath = "templates/user/wishlist.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "books" to books,
                )
            )
        }
    }
}
