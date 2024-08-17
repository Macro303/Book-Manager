package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.IdInput
import github.buriedincode.bookshelf.models.ReadBook
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.models.UserInput
import github.buriedincode.bookshelf.tables.UserTable
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll

object UserApiRouter : BaseApiRouter<User>(entity = User) {
    override fun list(ctx: Context) = Utils.query {
        val query = UserTable.selectAll()
        ctx.queryParam("username")?.let { username ->
            query.andWhere { UserTable.usernameCol like "%$username%" }
        }
        ctx.json(User.wrapRows(query.withDistinct()).toList().sorted().map { it.toJson() })
    }

    override fun create(ctx: Context) = ctx.processInput<UserInput> { body ->
        User.find(body.username)?.let {
            throw ConflictResponse("User already exists")
        }
        val resource = User.findOrCreate(body.username).apply {
            body.readBooks.forEach {
                ReadBook.new {
                    this.book = Book.findById(it.book) ?: throw NotFoundResponse("Book not found.")
                    this.user = this@apply
                    this.readDate = it.readDate
                }
            }
            imageUrl = body.imageUrl
            wishedBooks = SizedCollection(
                body.wishedBooks.map {
                    Book.findById(it) ?: throw NotFoundResponse("Book not found.")
                },
            )
        }
        ctx.status(HttpStatus.CREATED).json(resource.toJson(showAll = true))
    }

    override fun update(ctx: Context) = manage<UserInput>(ctx) { body, user ->
        User.find(body.username)?.takeIf { it != user }?.let { throw ConflictResponse("User already exists") }
        user.apply {
            readBooks.forEach { it.delete() }
            body.readBooks.forEach {
                ReadBook
                    .findOrCreate(
                        Book.findById(it.book) ?: throw NotFoundResponse("Book not found."),
                        user,
                    ).apply {
                        readDate = it.readDate
                    }
            }
            imageUrl = body.imageUrl
            username = body.username
            wishedBooks = SizedCollection(
                body.wishedBooks.map {
                    Book.findById(it) ?: throw NotFoundResponse("Book not found.")
                },
            )
        }
    }

    override fun delete(ctx: Context) = Utils.query {
        ctx.getResource().apply {
            readBooks.forEach { it.delete() }
            delete()
        }
        ctx.status(HttpStatus.NO_CONTENT)
    }

    fun addReadBook(ctx: Context) = manage<UserInput.ReadBook>(ctx) { body, user ->
        val book = Book.findById(body.book) ?: throw NotFoundResponse("No Book found.")
        if (!book.isCollected) {
            throw BadRequestResponse("Book hasn't been collected")
        }
        ReadBook.findOrCreate(book, user).apply {
            readDate = body.readDate
        }
    }

    fun removeReadBook(ctx: Context) = manage<IdInput>(ctx) { body, user ->
        val book = Book.findById(body.id) ?: throw NotFoundResponse("No Book found.")
        if (!book.isCollected) {
            throw BadRequestResponse("Book hasn't been collected")
        }
        ReadBook.find(book, user)?.delete()
    }

    fun addWishedBook(ctx: Context) = manage<IdInput>(ctx) { body, user ->
        val book = Book.findById(body.id) ?: throw NotFoundResponse("No Book found.")
        if (book.isCollected) {
            throw BadRequestResponse("Book hasn been collected")
        }
        user.wishedBooks = SizedCollection(user.wishedBooks + book)
    }

    fun removeWishedBook(ctx: Context) = manage<IdInput>(ctx) { body, user ->
        val book = Book.findById(body.id) ?: throw NotFoundResponse("No Book found.")
        if (book.isCollected) {
            throw BadRequestResponse("Book hasn been collected")
        }
        user.wishedBooks = SizedCollection(user.wishedBooks - book)
    }
}
