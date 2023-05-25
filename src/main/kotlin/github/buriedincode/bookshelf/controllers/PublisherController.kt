package github.buriedincode.bookshelf.controllers

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.PublisherEntry
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.PublisherInput
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*
import org.apache.logging.log4j.kotlin.Logging

object PublisherController : Logging {
    @OpenApi(
        description = "List all Publishers",
        methods = [HttpMethod.GET],
        operationId = "listPublishers",
        path = "/publishers",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = []),
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(Array<PublisherEntry>::class)])],
        security = [],
        summary = "List all Publishers",
        tags = ["Publisher"]
    )
    fun listPublishers(ctx: Context): Unit = Utils.query(description = "List Publishers") {
        val results = Publisher.all()
        ctx.json(results.map { it.toJson() })
    }

    @OpenApi(
        description = "Create Publisher",
        methods = [HttpMethod.POST],
        operationId = "createPublisher",
        path = "/publishers",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(PublisherInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "201", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Publisher::class)]),
        ],
        security = [],
        summary = "Create Publisher",
        tags = ["Publisher"]
    )
    fun createPublisher(ctx: Context): Unit = Utils.query(description = "Create Publisher") {
        val input: PublisherInput
        try {
            input = ctx.bodyAsClass<PublisherInput>()
        } catch (upe: UnrecognizedPropertyException) {
            throw BadRequestResponse("Invalid Body: ${upe.message}")
        }
        val result = Publisher.new {
            title = input.title
        }

        ctx.status(HttpStatus.CREATED).json(result.toJson(showAll = true))
    }

    @OpenApi(
        description = "Get Publisher by id",
        methods = [HttpMethod.GET],
        operationId = "getPublisher",
        path = "/publishers/{publisher-id}",
        pathParams = [OpenApiParam(name = "publisher-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Publisher::class)]),
        ],
        security = [],
        summary = "Get Publisher by id",
        tags = ["Publisher"]
    )
    fun getPublisher(ctx: Context): Unit = Utils.query(description = "Get Publisher") {
        val publisherId = ctx.pathParam("publisher-id")
        val result = publisherId.toLongOrNull()?.let { Publisher.findById(id = it) }
            ?: throw BadRequestResponse(message = "Invalid Publisher Id")
        ctx.json(result.toJson(showAll = true))
    }

    @OpenApi(
        description = "Update Publisher",
        methods = [HttpMethod.PUT],
        operationId = "updatePublisher",
        path = "/publishers/{publisher-id}",
        pathParams = [OpenApiParam(name = "publisher-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(PublisherInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Publisher::class)]),
        ],
        security = [],
        summary = "Update Publisher",
        tags = ["Publisher"]
    )
    fun updatePublisher(ctx: Context): Unit = Utils.query(description = "Update Publisher") {
        val publisherId = ctx.pathParam("publisher-id")
        val input: PublisherInput
        try {
            input = ctx.bodyAsClass<PublisherInput>()
        } catch (upe: UnrecognizedPropertyException) {
            throw BadRequestResponse("Invalid Body: ${upe.message}")
        }
        val result = publisherId.toLongOrNull()?.let { Publisher.findById(id = it) }
            ?: throw BadRequestResponse(message = "Invalid Publisher Id")
        result.title = input.title

        ctx.json(result.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete Publisher",
        methods = [HttpMethod.DELETE],
        operationId = "deletePublisher",
        path = "/publishers/{publisher-id}",
        pathParams = [OpenApiParam(name = "publisher-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "204")
        ],
        security = [],
        summary = "Delete Publisher",
        tags = ["Publisher"]
    )
    fun deletePublisher(ctx: Context): Unit = Utils.query(description = "Delete Publisher") {
        val publisherId = ctx.pathParam("publisher-id")
        val result = publisherId.toLongOrNull()?.let { Publisher.findById(id = it) }
            ?: throw BadRequestResponse(message = "Invalid Publisher Id")
        result.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }
}
