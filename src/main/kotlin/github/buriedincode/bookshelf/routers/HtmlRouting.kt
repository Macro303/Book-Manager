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
        val cookie = this.cookie("session-id")?.toLongOrNull() ?: -1L
        return User.findById(id = cookie)
    }

    abstract fun listEndpoint(ctx: Context)
    abstract fun viewEndpoint(ctx: Context)
    abstract fun editEndpoint(ctx: Context)
}

object BookHtmlRouter : BaseHtmlRouter<Book>(entity = Book), Logging {
    override fun listEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            var resources = Book.all().toList().filter { it.isCollected }
            val title = ctx.queryParam("title")
            if (title != null)
                resources = resources.filter {
                    (it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)) ||
                            (it.subtitle?.let {
                                it.contains(title, ignoreCase = true) || title.contains(it, ignoreCase = true)
                            } ?: false)
                }
            ctx.render(
                filePath = "templates/$name/list.kte", mapOf(
                    "resources" to resources,
                    "session" to session,
                    "selected" to mapOf(
                        "title" to title
                    ),
                )
            )
        }
    }

    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = ctx.getResource()
            val credits = HashMap<Role, List<Creator>>()
            for (entry in resource.credits) {
                var temp = credits.getOrDefault(entry.role, ArrayList())
                temp = temp.plus(entry.creator)
                credits[entry.role] = temp
            }
            ctx.render(
                filePath = "templates/$name/view.kte", mapOf(
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
        else if (session.role < 2)
            ctx.redirect("/books/${ctx.pathParam(paramName)}")
        else {
            val resource = ctx.getResource()
            val creators = Creator.all().toList()
            val genres = Genre.all().toList().filterNot { it in resource.genres }
            val publishers = Publisher.all().toList().filterNot { it == resource.publisher }
            val readers = User.all().toList().filterNot { it in resource.readers.map { it.user } }
            val roles = Role.all().toList()
            val series = Series.all().toList().filterNot { it in resource.series.map { it.series } }
            val wishers = User.all().toList().filterNot { it in resource.wishers }
            ctx.render(
                filePath = "templates/$name/edit.kte", mapOf(
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
    override fun listEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            var resources = Creator.all().toList()
            val nameQuery = ctx.queryParam("name")
            if (nameQuery != null)
                resources = resources.filter { it.name.contains(nameQuery, ignoreCase = true) || nameQuery.contains(it.name, ignoreCase = true) }
            ctx.render(
                filePath = "templates/$name/list.kte", mapOf(
                    "resources" to resources,
                    "session" to session,
                    "selected" to mapOf(
                        "name" to nameQuery
                    ),
                )
            )
        }
    }

    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = ctx.getResource()
            val credits = HashMap<Role, List<Book>>()
            for (entry in resource.credits) {
                var temp = credits.getOrDefault(entry.role, ArrayList())
                temp = temp.plus(entry.book)
                credits[entry.role] = temp
            }
            ctx.render(
                filePath = "templates/$name/view.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "credits" to credits,
                )
            )
        }
    }

    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else if (session.role < 2)
            ctx.redirect("/creators/${ctx.pathParam(paramName)}")
        else {
            val resource = ctx.getResource()
            val books = Book.all().toList()
            val roles = Role.all().toList()
            ctx.render(
                filePath = "templates/$name/edit.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "books" to books,
                    "roles" to roles,
                )
            )
        }
    }
}

object GenreHtmlRouter : BaseHtmlRouter<Genre>(entity = Genre), Logging {
    override fun listEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            var resources = Genre.all().toList()
            val title = ctx.queryParam("title")
            if (title != null)
                resources = resources.filter { it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true) }
            ctx.render(
                filePath = "templates/$name/list.kte", mapOf(
                    "resources" to resources,
                    "session" to session,
                    "selected" to mapOf(
                        "title" to title
                    ),
                )
            )
        }
    }

    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = ctx.getResource()
            ctx.render(filePath = "templates/$name/view.kte", mapOf("resource" to resource, "session" to session))
        }
    }

    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else if (session.role < 2)
            ctx.redirect("/genres/${ctx.pathParam(paramName)}")
        else {
            val resource = ctx.getResource()
            val books = Book.all().toList().filterNot { it in resource.books }
            ctx.render(
                filePath = "templates/$name/edit.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "books" to books,
                )
            )
        }
    }
}

object PublisherHtmlRouter : BaseHtmlRouter<Publisher>(entity = Publisher), Logging {
    override fun listEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            var resources = Publisher.all().toList()
            val title = ctx.queryParam("title")
            if (title != null)
                resources = resources.filter { it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true) }
            ctx.render(
                filePath = "templates/$name/list.kte", mapOf(
                    "resources" to resources,
                    "session" to session,
                    "selected" to mapOf(
                        "title" to title
                    ),
                )
            )
        }
    }

    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = ctx.getResource()
            ctx.render(filePath = "templates/$name/view.kte", mapOf("resource" to resource, "session" to session))
        }
    }

    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else if (session.role < 2)
            ctx.redirect("/publishers/${ctx.pathParam(paramName)}")
        else {
            val resource = ctx.getResource()
            ctx.render(filePath = "templates/$name/edit.kte", mapOf("resource" to resource, "session" to session))
        }
    }
}

object RoleHtmlRouter : BaseHtmlRouter<Role>(entity = Role), Logging {
    override fun listEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            var resources = Role.all().toList()
            val title = ctx.queryParam("title")
            if (title != null)
                resources = resources.filter { it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true) }
            ctx.render(
                filePath = "templates/$name/list.kte", mapOf(
                    "resources" to resources,
                    "session" to session,
                    "selected" to mapOf(
                        "title" to title
                    ),
                )
            )
        }
    }

    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = ctx.getResource()
            val credits = HashMap<Creator, List<Book>>()
            for (entry in resource.credits) {
                var temp = credits.getOrDefault(entry.creator, ArrayList())
                temp = temp.plus(entry.book)
                credits[entry.creator] = temp
            }
            ctx.render(
                filePath = "templates/$name/view.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "credits" to credits,
                )
            )
        }
    }

    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else if (session.role < 2)
            ctx.redirect("/roles/${ctx.pathParam(paramName)}")
        else {
            val resource = ctx.getResource()
            val books = Book.all().toList()
            val creators = Creator.all().toList()
            ctx.render(
                filePath = "templates/$name/edit.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "books" to books,
                    "creators" to creators,
                )
            )
        }
    }
}

object SeriesHtmlRouter : BaseHtmlRouter<Series>(entity = Series), Logging {
    override fun listEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            var resources = Series.all().toList()
            val title = ctx.queryParam("title")
            if (title != null)
                resources = resources.filter { it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true) }
            ctx.render(
                filePath = "templates/$name/list.kte", mapOf(
                    "resources" to resources,
                    "session" to session,
                    "selected" to mapOf(
                        "title" to title
                    ),
                )
            )
        }
    }

    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = ctx.getResource()
            ctx.render(filePath = "templates/$name/view.kte", mapOf("resource" to resource, "session" to session))
        }
    }

    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else if (session.role < 2)
            ctx.redirect("/series/${ctx.pathParam(paramName)}")
        else {
            val resource = ctx.getResource()
            val books = Book.all().toList().filterNot { it in resource.books.map { it.book } }
            ctx.render(
                filePath = "templates/$name/edit.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "books" to books,
                )
            )
        }
    }
}

object UserHtmlRouter : BaseHtmlRouter<User>(entity = User), Logging {
    override fun listEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            var resources = User.all().toList()
            val username = ctx.queryParam("username")
            if (username != null)
                resources = resources.filter { it.username.contains(username, ignoreCase = true) || username.contains(it.username, ignoreCase = true) }
            ctx.render(
                filePath = "templates/$name/list.kte", mapOf(
                    "resources" to resources,
                    "session" to session,
                    "selected" to mapOf(
                        "username" to username
                    ),
                )
            )
        }
    }

    override fun viewEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        if (session == null)
            ctx.redirect("/")
        else {
            val resource = ctx.getResource()
            val readListSize = resource.readBooks.count()
            val stats = mapOf(
                "wishlist" to Book.all().count { !it.isCollected && resource in it.wishers },
                "shared" to Book.all().count { !it.isCollected && it.wishers.empty() },
                "unread" to Book.all().count { it.isCollected } - readListSize,
                "read" to readListSize
            )
            ctx.render(filePath = "templates/$name/view.kte", mapOf("resource" to resource, "session" to session, "stats" to stats))
        }
    }

    override fun editEndpoint(ctx: Context): Unit = Utils.query {
        val session = ctx.getSession()
        val resource = ctx.getResource()
        if (session == null)
            ctx.redirect("/")
        else if (session != resource && (session.role < 2 || session.role < resource.role))
            ctx.redirect("/users/${ctx.pathParam(paramName)}")
        else {
            val readBooks = Book.all().toList().filter { it.isCollected }.filterNot { it in resource.readBooks.map { it.book } }
            val wishedBooks = Book.all().toList().filterNot { it.isCollected }.filterNot { it in resource.wishedBooks }
            ctx.render(
                filePath = "templates/$name/edit.kte", mapOf(
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
            val resource = ctx.getResource()
            val books = Book.all().toList().filter { !it.isCollected && (it.wishers.empty() || resource in it.wishers) }
            ctx.render(
                filePath = "templates/$name/wishlist.kte", mapOf(
                    "resource" to resource,
                    "session" to session,
                    "books" to books,
                )
            )
        }
    }
}
