package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.ErrorResponse
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.UserEntry
import github.buriedincode.bookshelf.models.*
import github.buriedincode.bookshelf.tables.UserTable
import io.javalin.http.*
import io.javalin.openapi.*
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection

object UserApiRouter : Logging {
    private fun Context.getUser(): User {
        return this.pathParam("user-id").toLongOrNull()?.let {
            User.findById(id = it) ?: throw NotFoundResponse(message = "User not found")
        } ?: throw BadRequestResponse(message = "Invalid User Id")
    }

    private fun Context.getUserInput(): UserInput = this.bodyValidator<UserInput>()
        .check({ it.role >= 0 }, error = "Role must be greater than or equal to 0")
        .check({ it.username.isNotBlank() }, error = "Username must not be empty")
        .get()

    @OpenApi(
        description = "List all Users",
        methods = [HttpMethod.GET],
        operationId = "listUsers",
        path = "/users",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(Array<UserEntry>::class)]),
        ],
        security = [],
        summary = "List all Users",
        tags = ["User"]
    )
    fun listUsers(ctx: Context): Unit = Utils.query(description = "List Users") {
        val users = User.all()
        ctx.json(users.map { it.toJson() })
    }

    @OpenApi(
        description = "Create User",
        methods = [HttpMethod.POST],
        operationId = "createUser",
        path = "/users",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(UserInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "201", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Create User",
        tags = ["User"]
    )
    fun createUser(ctx: Context): Unit = Utils.query(description = "Create User") {
        val body = ctx.getUserInput()
        val exists = User.find {
            UserTable.usernameCol eq body.username
        }.firstOrNull()
        if (exists != null)
            throw ConflictResponse(message = "User already exists")
        val user = User.new {
            imageUrl = body.imageUrl
            readBooks = SizedCollection(body.readBookIds.map {
                Book.findById(id = it)
                    ?: throw NotFoundResponse(message = "Book not found")
            })
            role = body.role
            username = body.username
            wishedBooks = SizedCollection(body.wishedBookIds.map {
                Book.findById(id = it)
                    ?: throw NotFoundResponse(message = "Book not found")
            })
        }

        ctx.status(HttpStatus.CREATED).json(user.toJson(showAll = true))
    }

    @OpenApi(
        description = "Get User by id",
        methods = [HttpMethod.GET],
        operationId = "getUser",
        path = "/users/{user-id}",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Get User by id",
        tags = ["User"]
    )
    fun getUser(ctx: Context): Unit = Utils.query(description = "Get User") {
        val user = ctx.getUser()
        ctx.json(user.toJson(showAll = true))
    }

    @OpenApi(
        description = "Update User",
        methods = [HttpMethod.PUT],
        operationId = "updateUser",
        path = "/users/{user-id}",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(UserInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Update User",
        tags = ["User"]
    )
    fun updateUser(ctx: Context): Unit = Utils.query(description = "Update User") {
        val user = ctx.getUser()
        val body = ctx.getUserInput()
        val exists = User.find {
            UserTable.usernameCol eq body.username
        }.firstOrNull()
        if (exists != null && exists != user)
            throw ConflictResponse(message = "User already exists")
        user.imageUrl = body.imageUrl
        user.readBooks = SizedCollection(body.readBookIds.map {
            Book.findById(id = it)
                ?: throw NotFoundResponse(message = "Book not found")
        })
        user.role = body.role
        user.username = body.username
        user.wishedBooks = SizedCollection(body.wishedBookIds.map {
            Book.findById(id = it)
                ?: throw NotFoundResponse(message = "Book not found")
        })

        ctx.json(user.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete User",
        methods = [HttpMethod.DELETE],
        operationId = "deleteUser",
        path = "/users/{user-id}",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "204"),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Delete User",
        tags = ["User"]
    )
    fun deleteUser(ctx: Context): Unit = Utils.query(description = "Delete User") {
        val user = ctx.getUser()
        user.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }

    private fun Context.getIdValue(): IdValue = this.bodyValidator<IdValue>()
        .check({ it.id > 0 }, error = "Id must be greater than 0")
        .get()

    @OpenApi(
        description = "Add Book to User read list",
        methods = [HttpMethod.PATCH],
        operationId = "addBookToUserReadList",
        path = "/user/{user-id}/read",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Add Book to User read list",
        tags = ["User"]
    )
    fun addReadBook(ctx: Context): Unit = Utils.query(description = "Add Book to User read list") {
        val user = ctx.getUser()
        val body = ctx.getIdValue()
        val book = Book.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Book not found")
        if (book in user.readBooks)
            throw ConflictResponse(message = "Book already is on User read list")
        val temp = user.readBooks.toMutableList()
        temp.add(book)
        user.readBooks = SizedCollection(temp)

        ctx.json(user.toJson(showAll = true))
    }

    @OpenApi(
        description = "Remove Book from User read list",
        methods = [HttpMethod.DELETE],
        operationId = "removeBookFromUserReadList",
        path = "/user/{user-id}/read",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Remove Book from User read list",
        tags = ["User"]
    )
    fun removeReadBook(ctx: Context): Unit = Utils.query(description = "Remove Book from User read list") {
        val user = ctx.getUser()
        val body = ctx.getIdValue()
        val book = Book.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Book not found")
        if (!user.readBooks.contains(book))
            throw NotFoundResponse(message = "Book isn't linked to User read list")
        val temp = user.readBooks.toMutableList()
        temp.remove(book)
        user.readBooks = SizedCollection(temp)

        ctx.json(user.toJson(showAll = true))
    }

    @OpenApi(
        description = "Add Book to User wished list",
        methods = [HttpMethod.PATCH],
        operationId = "addBookToUserWishedList",
        path = "/user/{user-id}/wished",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Add Book to User wished list",
        tags = ["User"]
    )
    fun addWishedBook(ctx: Context): Unit = Utils.query(description = "Add Book to User wished list") {
        val user = ctx.getUser()
        val body = ctx.getIdValue()
        val book = Book.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Book not found")
        if (book in user.wishedBooks)
            throw ConflictResponse(message = "Book already is on User wished list")
        val temp = user.wishedBooks.toMutableList()
        temp.add(book)
        user.wishedBooks = SizedCollection(temp)

        ctx.json(user.toJson(showAll = true))
    }

    @OpenApi(
        description = "Remove Book from User wished list",
        methods = [HttpMethod.DELETE],
        operationId = "removeBookFromUserWishedList",
        path = "/user/{user-id}/wished",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Remove Book from User wished list",
        tags = ["User"]
    )
    fun removeWishedBook(ctx: Context): Unit = Utils.query(description = "Remove Book from User wished list") {
        val user = ctx.getUser()
        val body = ctx.getIdValue()
        val book = Book.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Book not found")
        if (!user.wishedBooks.contains(book))
            throw NotFoundResponse(message = "Book isn't linked to User wished list")
        val temp = user.wishedBooks.toMutableList()
        temp.remove(book)
        user.wishedBooks = SizedCollection(temp)

        ctx.json(user.toJson(showAll = true))
    }
}