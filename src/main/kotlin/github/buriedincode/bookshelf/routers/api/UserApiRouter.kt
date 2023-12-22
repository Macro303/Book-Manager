package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.IdInput
import github.buriedincode.bookshelf.models.ReadBook
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.models.UserInput
import github.buriedincode.bookshelf.tables.ReadBookTable
import github.buriedincode.bookshelf.tables.UserTable
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyAsClass
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and

object UserApiRouter : BaseApiRouter<User>(entity = User), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = User.all().toList()
            ctx.queryParam("has-image")?.lowercase()?.toBooleanStrictOrNull()?.let { image ->
                resources = resources.filter { it.image != null == image }
            }
            ctx.queryParam("read-id")?.toLongOrNull()?.let {
                Book.findById(it)?.let { book ->
                    resources = resources.filter { book in it.readBooks.map { it.book } }
                }
            }
            ctx.queryParam("username")?.let { username ->
                resources = resources.filter {
                    it.username.contains(username, ignoreCase = true) || username.contains(it.username, ignoreCase = true)
                }
            }
            ctx.queryParam("wished-id")?.toLongOrNull()?.let {
                Book.findById(it)?.let { book ->
                    resources = resources.filter { book in it.wishedBooks }
                }
            }
            ctx.json(resources.sorted().map { it.toJson() })
        }
    }

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val body = ctx.bodyAsClass<UserInput>()
            val exists = User.find {
                UserTable.usernameCol eq body.username
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse("User already exists")
            }
            val resource = User.new {
                image = body.image
                username = body.username
                wishedBooks = SizedCollection(
                    body.wishedBookIds.map {
                        Book.findById(it) ?: throw NotFoundResponse("No Book found.")
                    },
                )
            }
            body.readBooks.forEach {
                val book = Book.findById(it.bookId)
                    ?: throw NotFoundResponse("No Book found.")
                val readBook = ReadBook.find {
                    (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq resource.id)
                }.firstOrNull() ?: ReadBook.new {
                    this.book = book
                    this.user = user
                }
                readBook.readDate = it.readDate
            }

            ctx.status(HttpStatus.CREATED).json(resource.toJson(showAll = true))
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<UserInput>()
            val exists = User.find {
                UserTable.usernameCol eq body.username
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse("User already exists")
            }
            resource.image = body.image
            resource.username = body.username
            resource.wishedBooks = SizedCollection(
                body.wishedBookIds.map {
                    Book.findById(it) ?: throw NotFoundResponse("No Book found.")
                },
            )
            body.readBooks.forEach {
                val book = Book.findById(it.bookId)
                    ?: throw NotFoundResponse("No Book found.")
                val readBook = ReadBook.find {
                    (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq resource.id)
                }.firstOrNull() ?: ReadBook.new {
                    this.book = book
                    this.user = resource
                }
                readBook.readDate = it.readDate
            }

            ctx.json(resource.toJson(showAll = true))
        }
    }

    override fun deleteEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            resource.readBooks.forEach {
                it.delete()
            }
            resource.delete()
            ctx.status(HttpStatus.NO_CONTENT)
        }
    }

    fun addReadBook(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<UserInput.ReadBook>()
            val book = Book.findById(body.bookId)
                ?: throw NotFoundResponse("No Book found.")
            val readBook = ReadBook.find {
                (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq resource.id)
            }.firstOrNull() ?: ReadBook.new {
                this.book = book
                this.user = resource
            }
            readBook.readDate = body.readDate

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun removeReadBook(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<IdInput>()
            val book = Book.findById(body.id)
                ?: throw NotFoundResponse("No Book found.")
            val readBook = ReadBook.find {
                (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq resource.id)
            }.firstOrNull() ?: throw BadRequestResponse("Read Book not found.")
            readBook.delete()

            ctx.status(HttpStatus.NO_CONTENT)
        }
    }

    fun addWishedBook(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<IdInput>()
            val book = Book.findById(body.id)
                ?: throw NotFoundResponse("No Book found.")
            val temp = resource.wishedBooks.toMutableSet()
            temp.add(book)
            resource.wishedBooks = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun removeWishedBook(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<IdInput>()
            val book = Book.findById(body.id)
                ?: throw NotFoundResponse("No Book found.")
            val temp = resource.wishedBooks.toMutableSet()
            temp.remove(book)
            resource.wishedBooks = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }
    }
}
