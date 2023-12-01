package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.ReadBook
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.models.UserInput
import github.buriedincode.bookshelf.models.UserRole
import github.buriedincode.bookshelf.tables.ReadBookTable
import github.buriedincode.bookshelf.tables.UserTable
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.UnauthorizedResponse
import io.javalin.http.bodyValidator
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
            ctx.queryParam("username")?.let { username ->
                resources = resources.filter {
                    it.username.contains(username, ignoreCase = true) || username.contains(it.username, ignoreCase = true)
                }
            }
            ctx.json(resources.sorted().map { it.toJson() })
        }
    }

    private fun Context.getInput(): UserInput =
        this.bodyValidator<UserInput>()
            .check({ it.username.isNotBlank() }, error = "Username must not be empty")
            .get()

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val input = ctx.getInput()
            val exists = User.find {
                UserTable.usernameCol eq input.username
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse(message = "User already exists")
            }
            val user = User.new {
                image = input.image
                role = input.role
                username = input.username
                wishedBooks = SizedCollection(
                    input.wishedBookIds.map {
                        Book.findById(id = it)
                            ?: throw NotFoundResponse(message = "Unable to find Book: `$it`")
                    },
                )
            }
            input.readBooks.forEach {
                val book = Book.findById(id = it.bookId)
                    ?: throw NotFoundResponse(message = "Unable to find Book: `${it.bookId}`")
                val readBook = ReadBook.find {
                    (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq user.id)
                }.firstOrNull() ?: ReadBook.new {
                    this.book = book
                    this.user = user
                }
                readBook.readDate = it.readDate
            }

            ctx.status(HttpStatus.CREATED).json(user.toJson(showAll = true))
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val session = ctx.attribute<User>("session")!!
            val resource = ctx.getResource()
            if (session != resource && !(session.role >= UserRole.MODERATOR && session.role > resource.role)) {
                throw UnauthorizedResponse()
            }
            val input = ctx.getInput()
            val exists = User.find {
                UserTable.usernameCol eq input.username
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse(message = "User already exists")
            }
            resource.image = input.image
            input.readBooks.forEach {
                val book = Book.findById(id = it.bookId)
                    ?: throw NotFoundResponse(message = "Unable to find Book: `${it.bookId}`")
                val readBook = ReadBook.find {
                    (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq resource.id)
                }.firstOrNull() ?: ReadBook.new {
                    this.book = book
                    this.user = resource
                }
                readBook.readDate = it.readDate
            }
            resource.role = input.role
            resource.username = input.username
            resource.wishedBooks = SizedCollection(
                input.wishedBookIds.map {
                    Book.findById(id = it)
                        ?: throw NotFoundResponse(message = "Unable to find Book: `$it`")
                },
            )

            ctx.json(resource.toJson(showAll = true))
        }
    }

    override fun deleteEndpoint(ctx: Context) {
        Utils.query {
            val session = ctx.attribute<User>("session")!!
            val resource = ctx.getResource()
            if (session != resource && !(session.role >= UserRole.MODERATOR && session.role > resource.role)) {
                throw UnauthorizedResponse()
            }
            resource.readBooks.forEach {
                it.delete()
            }
            resource.delete()
            ctx.status(HttpStatus.NO_CONTENT)
        }
    }
}
