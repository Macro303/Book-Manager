package github.buriedincode.bookshelf.controllers

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.PublisherEntry
import github.buriedincode.bookshelf.docs.UserEntry
import github.buriedincode.bookshelf.models.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection

object UserController : Logging {
    @OpenApi(
        description = "List all Users",
        methods = [HttpMethod.GET],
        operationId = "listUsers",
        path = "/users",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = []),
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(Array<UserEntry>::class)])],
        security = [],
        summary = "List all Users",
        tags = ["User"]
    )
    fun listUsers(ctx: Context): Unit = Utils.query(description = "List Users") {
        val results = User.all()
        ctx.json(results.map { it.toJson() })
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
        ],
        security = [],
        summary = "Create User",
        tags = ["User"]
    )
    fun createUser(ctx: Context): Unit = Utils.query(description = "Create User") {
        val input: UserInput
        try {
            input = ctx.bodyAsClass<UserInput>()
        } catch (upe: UnrecognizedPropertyException) {
            throw BadRequestResponse("Invalid Body: ${upe.message}")
        }
        val result = User.new {
            read_books = SizedCollection(input.readBookIds.map {
                Book.findById(id = it) ?: throw BadRequestResponse(message = "Invalid Book Id")
            })
            username = input.username
            wished_books = SizedCollection(input.wishedBookIds.map {
                Book.findById(id = it) ?: throw BadRequestResponse(message = "Invalid Book Id")
            })
        }

        ctx.status(HttpStatus.CREATED).json(result.toJson(showAll = true))
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
        ],
        security = [],
        summary = "Get User by id",
        tags = ["User"]
    )
    fun getUser(ctx: Context): Unit = Utils.query(description = "Get User") {
        val userId = ctx.pathParam("user-id")
        val result = userId.toLongOrNull()?.let { User.findById(id = it) }
            ?: throw BadRequestResponse(message = "Invalid User Id")
        ctx.json(result.toJson(showAll = true))
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
        ],
        security = [],
        summary = "Update User",
        tags = ["User"]
    )
    fun updateUser(ctx: Context): Unit = Utils.query(description = "Update User") {
        val userId = ctx.pathParam("user-id")
        val input: UserInput
        try {
            input = ctx.bodyAsClass<UserInput>()
        } catch (upe: UnrecognizedPropertyException) {
            throw BadRequestResponse("Invalid Body: ${upe.message}")
        }
        val result = userId.toLongOrNull()?.let { User.findById(id = it) }
            ?: throw BadRequestResponse(message = "Invalid User Id")
        result.read_books = SizedCollection(input.readBookIds.map {
            Book.findById(id = it) ?: throw BadRequestResponse(message = "Invalid Book Id")
        })
        result.username = input.username
        result.wished_books = SizedCollection(input.wishedBookIds.map {
            Book.findById(id = it) ?: throw BadRequestResponse(message = "Invalid Book Id")
        })

        ctx.json(result.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete User",
        methods = [HttpMethod.DELETE],
        operationId = "deleteUser",
        path = "/users/{user-id}",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "204")
        ],
        security = [],
        summary = "Delete User",
        tags = ["User"]
    )
    fun deleteUser(ctx: Context): Unit = Utils.query(description = "Delete User") {
        val userId = ctx.pathParam("user-id")
        val result = userId.toLongOrNull()?.let { User.findById(id = it) }
            ?: throw BadRequestResponse(message = "Invalid User Id")
        result.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }
}