package github.buriedincode.bookshelf.routers

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.asEnumOrNull
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Format
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.models.Series
import github.buriedincode.bookshelf.models.User
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
        val cookie = this.cookie(name = "bookshelf_session-id")?.toLongOrNull() ?: -1L
        return User.findById(id = cookie)
    }

    abstract fun listEndpoint(ctx: Context)

    abstract fun viewEndpoint(ctx: Context)

    abstract fun editEndpoint(ctx: Context)
}

object BookHtmlRouter : BaseHtmlRouter<Book>(entity = Book), Logging {
    override fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
                var resources = Book.all().toList().filter { it.isCollected }
                val books = resources
                val creator = ctx.queryParam(key = "creator-id")?.toLongOrNull()?.let {
                    Creator.findById(id = it)
                }
                if (creator != null) {
                    resources = resources.filter {
                        creator in it.credits.map { it.creator }
                    }
                }
                val format: Format? = ctx.queryParam(key = "format")?.asEnumOrNull<Format>()
                if (format != null) {
                    resources = resources.filter {
                        it.format == format
                    }
                }
                val genre = ctx.queryParam(key = "genre-id")?.toLongOrNull()?.let {
                    Genre.findById(id = it)
                }
                if (genre != null) {
                    resources = resources.filter {
                        genre in it.genres
                    }
                }
                val publisher = ctx.queryParam(key = "publisher-id")?.toLongOrNull()?.let {
                    Publisher.findById(id = it)
                }
                if (publisher != null) {
                    resources = resources.filter {
                        it.publisher == publisher
                    }
                }
                val series = ctx.queryParam(key = "series-id")?.toLongOrNull()?.let {
                    Series.findById(id = it)
                }
                if (series != null) {
                    resources = resources.filter {
                        series in it.series.map { it.series }
                    }
                }
                val title = ctx.queryParam(key = "title")
                if (title != null) {
                    resources = resources.filter {
                        (
                            it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
                        ) || (
                            it.subtitle?.let {
                                it.contains(title, ignoreCase = true) || title.contains(it, ignoreCase = true)
                            } ?: false
                        )
                    }
                }
                ctx.render(
                    filePath = "templates/$name/list.kte",
                    model = mapOf(
                        "resources" to resources,
                        "session" to session,
                        "creators" to books.flatMap { it.credits.map { it.creator } }.toSet().toList(),
                        "selectedCreator" to creator,
                        "formats" to books.map { it.format }.toSet().toList(),
                        "selectedFormat" to format,
                        "genres" to books.flatMap { it.genres }.toSet().toList(),
                        "selectedGenre" to genre,
                        "publishers" to books.map { it.publisher }.toSet().toList(),
                        "selectedPublisher" to publisher,
                        "series" to books.flatMap { it.series.map { it.series } }.toSet().toList(),
                        "selectedSeries" to series,
                        "selectedTitle" to title,
                    ),
                )
            }
        }

    override fun viewEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
                val resource = ctx.getResource()
                val credits = HashMap<Role, List<Creator>>()
                for (entry in resource.credits) {
                    var temp = credits.getOrDefault(entry.role, ArrayList())
                    temp = temp.plus(entry.creator)
                    credits[entry.role] = temp
                }
                ctx.render(
                    filePath = "templates/$name/view.kte",
                    model = mapOf(
                        "resource" to resource,
                        "session" to session,
                        "credits" to credits,
                    ),
                )
            }
        }

    override fun editEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else if (session.role < 2) {
                ctx.redirect(location = "/books/${ctx.pathParam(paramName)}")
            } else {
                val resource = ctx.getResource()
                val creators = Creator.all().toList()
                val genres = Genre.all().toList().filterNot { it in resource.genres }
                val publishers = Publisher.all().toList().filterNot { it == resource.publisher }
                val readers = User.all().toList().filterNot { it in resource.readers.map { it.user } }
                val roles = Role.all().toList()
                val series = Series.all().toList().filterNot { it in resource.series.map { it.series } }
                val wishers = User.all().toList().filterNot { it in resource.wishers }
                ctx.render(
                    filePath = "templates/$name/edit.kte",
                    model = mapOf(
                        "resource" to resource,
                        "session" to session,
                        "creators" to creators,
                        "genres" to genres,
                        "publishers" to publishers,
                        "readers" to readers,
                        "roles" to roles,
                        "series" to series,
                        "wishers" to wishers,
                    ),
                )
            }
        }
}

object CreatorHtmlRouter : BaseHtmlRouter<Creator>(entity = Creator), Logging {
    override fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
                var resources = Creator.all().toList()
                val nameQuery = ctx.queryParam(key = "name")
                if (nameQuery != null) {
                    resources = resources.filter {
                        it.name.contains(nameQuery, ignoreCase = true) || nameQuery.contains(
                            it.name,
                            ignoreCase = true,
                        )
                    }
                }
                ctx.render(
                    filePath = "templates/$name/list.kte",
                    model = mapOf(
                        "resources" to resources,
                        "session" to session,
                        "selected" to mapOf(
                            "name" to nameQuery,
                        ),
                    ),
                )
            }
        }

    override fun viewEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
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
                        "session" to session,
                        "credits" to credits,
                    ),
                )
            }
        }

    override fun editEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else if (session.role < 2) {
                ctx.redirect(location = "/creators/${ctx.pathParam(paramName)}")
            } else {
                val resource = ctx.getResource()
                val books = Book.all().toList()
                val roles = Role.all().toList()
                ctx.render(
                    filePath = "templates/$name/edit.kte",
                    model = mapOf(
                        "resource" to resource,
                        "session" to session,
                        "books" to books,
                        "roles" to roles,
                    ),
                )
            }
        }
}

object GenreHtmlRouter : BaseHtmlRouter<Genre>(entity = Genre), Logging {
    override fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
                var resources = Genre.all().toList()
                val title = ctx.queryParam(key = "title")
                if (title != null) {
                    resources = resources.filter {
                        it.title.contains(title, ignoreCase = true) || title.contains(
                            it.title,
                            ignoreCase = true,
                        )
                    }
                }
                ctx.render(
                    filePath = "templates/$name/list.kte",
                    model = mapOf(
                        "resources" to resources,
                        "session" to session,
                        "selected" to mapOf(
                            "title" to title,
                        ),
                    ),
                )
            }
        }

    override fun viewEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
                val resource = ctx.getResource()
                ctx.render(
                    filePath = "templates/$name/view.kte",
                    model = mapOf("resource" to resource, "session" to session),
                )
            }
        }

    override fun editEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else if (session.role < 2) {
                ctx.redirect(location = "/genres/${ctx.pathParam(paramName)}")
            } else {
                val resource = ctx.getResource()
                val books = Book.all().toList().filterNot { it in resource.books }
                ctx.render(
                    filePath = "templates/$name/edit.kte",
                    model = mapOf(
                        "resource" to resource,
                        "session" to session,
                        "books" to books,
                    ),
                )
            }
        }
}

object PublisherHtmlRouter : BaseHtmlRouter<Publisher>(entity = Publisher), Logging {
    override fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
                var resources = Publisher.all().toList()
                val title = ctx.queryParam(key = "title")
                if (title != null) {
                    resources = resources.filter {
                        it.title.contains(title, ignoreCase = true) || title.contains(
                            it.title,
                            ignoreCase = true,
                        )
                    }
                }
                ctx.render(
                    filePath = "templates/$name/list.kte",
                    model = mapOf(
                        "resources" to resources,
                        "session" to session,
                        "selected" to mapOf(
                            "title" to title,
                        ),
                    ),
                )
            }
        }

    override fun viewEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
                val resource = ctx.getResource()
                ctx.render(
                    filePath = "templates/$name/view.kte",
                    model = mapOf("resource" to resource, "session" to session),
                )
            }
        }

    override fun editEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else if (session.role < 2) {
                ctx.redirect(location = "/publishers/${ctx.pathParam(paramName)}")
            } else {
                val resource = ctx.getResource()
                ctx.render(
                    filePath = "templates/$name/edit.kte",
                    model = mapOf("resource" to resource, "session" to session),
                )
            }
        }
}

object RoleHtmlRouter : BaseHtmlRouter<Role>(entity = Role), Logging {
    override fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
                var resources = Role.all().toList()
                val title = ctx.queryParam(key = "title")
                if (title != null) {
                    resources = resources.filter {
                        it.title.contains(title, ignoreCase = true) || title.contains(
                            it.title,
                            ignoreCase = true,
                        )
                    }
                }
                ctx.render(
                    filePath = "templates/$name/list.kte",
                    model = mapOf(
                        "resources" to resources,
                        "session" to session,
                        "selected" to mapOf(
                            "title" to title,
                        ),
                    ),
                )
            }
        }

    override fun viewEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
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
                        "resource" to resource,
                        "session" to session,
                        "credits" to credits,
                    ),
                )
            }
        }

    override fun editEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else if (session.role < 2) {
                ctx.redirect(location = "/roles/${ctx.pathParam(paramName)}")
            } else {
                val resource = ctx.getResource()
                val books = Book.all().toList()
                val creators = Creator.all().toList()
                ctx.render(
                    filePath = "templates/$name/edit.kte",
                    model = mapOf(
                        "resource" to resource,
                        "session" to session,
                        "books" to books,
                        "creators" to creators,
                    ),
                )
            }
        }
}

object SeriesHtmlRouter : BaseHtmlRouter<Series>(entity = Series), Logging {
    override fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
                var resources = Series.all().toList()
                val title = ctx.queryParam(key = "title")
                if (title != null) {
                    resources = resources.filter {
                        it.title.contains(title, ignoreCase = true) || title.contains(
                            it.title,
                            ignoreCase = true,
                        )
                    }
                }
                ctx.render(
                    filePath = "templates/$name/list.kte",
                    model = mapOf(
                        "resources" to resources,
                        "session" to session,
                        "selected" to mapOf(
                            "title" to title,
                        ),
                    ),
                )
            }
        }

    override fun viewEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
                val resource = ctx.getResource()
                ctx.render(
                    filePath = "templates/$name/view.kte",
                    model = mapOf("resource" to resource, "session" to session),
                )
            }
        }

    override fun editEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else if (session.role < 2) {
                ctx.redirect(location = "/series/${ctx.pathParam(paramName)}")
            } else {
                val resource = ctx.getResource()
                val books = Book.all().toList().filterNot { it in resource.books.map { it.book } }
                ctx.render(
                    filePath = "templates/$name/edit.kte",
                    model = mapOf(
                        "resource" to resource,
                        "session" to session,
                        "books" to books,
                    ),
                )
            }
        }
}

object UserHtmlRouter : BaseHtmlRouter<User>(entity = User), Logging {
    override fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
                var resources = User.all().toList()
                val username = ctx.queryParam(key = "username")
                if (username != null) {
                    resources = resources.filter {
                        it.username.contains(
                            username,
                            ignoreCase = true,
                        ) || username.contains(it.username, ignoreCase = true)
                    }
                }
                ctx.render(
                    filePath = "templates/$name/list.kte",
                    model = mapOf(
                        "resources" to resources,
                        "session" to session,
                        "selected" to mapOf(
                            "username" to username,
                        ),
                    ),
                )
            }
        }

    override fun viewEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
                val resource = ctx.getResource()
                val readListSize = resource.readBooks.count()
                val stats = mapOf(
                    "wishlist" to Book.all().count { !it.isCollected && resource in it.wishers },
                    "shared" to Book.all().count { !it.isCollected && it.wishers.empty() },
                    "unread" to Book.all().count { it.isCollected } - readListSize,
                    "read" to readListSize,
                )
                ctx.render(
                    filePath = "templates/$name/view.kte",
                    model = mapOf("resource" to resource, "session" to session, "stats" to stats),
                )
            }
        }

    override fun editEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            val resource = ctx.getResource()
            if (session == null) {
                ctx.redirect(location = "/")
            } else if (session != resource && (session.role < 2 || session.role < resource.role)) {
                ctx.redirect(location = "/users/${ctx.pathParam(paramName)}")
            } else {
                val readBooks = Book.all().toList()
                    .filter { it.isCollected }
                    .filterNot { it in resource.readBooks.map { it.book } }
                val wishedBooks = Book.all().toList()
                    .filterNot { it.isCollected }
                    .filterNot { it in resource.wishedBooks }
                ctx.render(
                    filePath = "templates/$name/edit.kte",
                    model = mapOf(
                        "resource" to resource,
                        "session" to session,
                        "readBooks" to readBooks,
                        "wishedBooks" to wishedBooks,
                    ),
                )
            }
        }

    fun wishlistEndpoint(ctx: Context): Unit =
        Utils.query {
            val session = ctx.getSession()
            if (session == null) {
                ctx.redirect(location = "/")
            } else {
                val resource = ctx.getResource()
                var books = Book.all().toList()
                    .filter { !it.isCollected && (it.wishers.empty() || resource in it.wishers) }
                val wishlistBooks = books
                val creator = ctx.queryParam(key = "creator-id")?.toLongOrNull()?.let {
                    Creator.findById(id = it)
                }
                if (creator != null) {
                    books = books.filter {
                        creator in it.credits.map { it.creator }
                    }
                }
                val format: Format? = ctx.queryParam(key = "format")?.asEnumOrNull<Format>()
                if (format != null) {
                    books = books.filter {
                        it.format == format
                    }
                }
                val genre = ctx.queryParam(key = "genre-id")?.toLongOrNull()?.let {
                    Genre.findById(id = it)
                }
                if (genre != null) {
                    books = books.filter {
                        genre in it.genres
                    }
                }
                val publisher = ctx.queryParam(key = "publisher-id")?.toLongOrNull()?.let {
                    Publisher.findById(id = it)
                }
                if (publisher != null) {
                    books = books.filter {
                        it.publisher == publisher
                    }
                }
                val series = ctx.queryParam(key = "series-id")?.toLongOrNull()?.let {
                    Series.findById(id = it)
                }
                if (series != null) {
                    books = books.filter {
                        series in it.series.map { it.series }
                    }
                }
                val title = ctx.queryParam(key = "title")
                if (title != null) {
                    books = books.filter {
                        (
                            it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
                        ) || (
                            it.subtitle?.let {
                                it.contains(title, ignoreCase = true) || title.contains(it, ignoreCase = true)
                            } ?: false
                        )
                    }
                }
                ctx.render(
                    filePath = "templates/$name/wishlist.kte",
                    model = mapOf(
                        "resource" to resource,
                        "session" to session,
                        "books" to books,
                        "creators" to wishlistBooks.flatMap { it.credits.map { it.creator } }.toSet().toList(),
                        "selectedCreator" to creator,
                        "formats" to wishlistBooks.map { it.format }.toSet().toList(),
                        "selectedFormat" to format,
                        "genres" to wishlistBooks.flatMap { it.genres }.toSet().toList(),
                        "selectedGenre" to genre,
                        "publishers" to wishlistBooks.map { it.publisher }.toSet().toList(),
                        "selectedPublisher" to publisher,
                        "series" to wishlistBooks.flatMap { it.series.map { it.series } }.toSet().toList(),
                        "selectedSeries" to series,
                        "selectedTitle" to title,
                    ),
                )
            }
        }
}
