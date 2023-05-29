package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.ErrorResponse
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.PublisherEntry
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.PublisherInput
import github.buriedincode.bookshelf.tables.PublisherTable
import io.javalin.http.*
import io.javalin.openapi.*
import org.apache.logging.log4j.kotlin.Logging

object PublisherApiRouter : Logging {
    private fun Context.getPublisher(): Publisher {
        return this.pathParam("publisher-id").toLongOrNull()?.let {
            Publisher.findById(id = it) ?: throw NotFoundResponse(message = "Publisher not found")
        } ?: throw BadRequestResponse(message = "Invalid Publisher Id")
    }

    private fun Context.getPublisherInput(): PublisherInput = this.bodyValidator<PublisherInput>()
        .check({ it.title.isNotBlank() }, error = "Title must not be empty")
        .get()

    @OpenApi(
        description = "List all Publishers",
        methods = [HttpMethod.GET],
        operationId = "listPublishers",
        path = "/publishers",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(Array<PublisherEntry>::class)]),
        ],
        security = [],
        summary = "List all Publishers",
        tags = ["Publisher"]
    )
    fun listPublishers(ctx: Context): Unit = Utils.query(description = "List Publishers") {
        val publishers = Publisher.all()
        ctx.json(publishers.map { it.toJson() })
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
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Create Publisher",
        tags = ["Publisher"]
    )
    fun createPublisher(ctx: Context): Unit = Utils.query(description = "Create Publisher") {
        val body = ctx.getPublisherInput()
        val exists = Publisher.find {
            PublisherTable.titleCol eq body.title
        }.firstOrNull()
        if (exists != null)
            throw ConflictResponse(message = "Publisher already exists")
        val publisher = Publisher.new {
            title = body.title
        }

        ctx.status(HttpStatus.CREATED).json(publisher.toJson(showAll = true))
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
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Get Publisher by id",
        tags = ["Publisher"]
    )
    fun getPublisher(ctx: Context): Unit = Utils.query(description = "Get Publisher") {
        val publisher = ctx.getPublisher()
        ctx.json(publisher.toJson(showAll = true))
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
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Update Publisher",
        tags = ["Publisher"]
    )
    fun updatePublisher(ctx: Context): Unit = Utils.query(description = "Update Publisher") {
        val publisher = ctx.getPublisher()
        val body = ctx.getPublisherInput()
        val exists = Publisher.find {
            PublisherTable.titleCol eq body.title
        }.firstOrNull()
        if (exists != null && exists != publisher)
            throw ConflictResponse(message = "Publisher already exists")
        publisher.title = body.title

        ctx.json(publisher.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete Publisher",
        methods = [HttpMethod.DELETE],
        operationId = "deletePublisher",
        path = "/publishers/{publisher-id}",
        pathParams = [OpenApiParam(name = "publisher-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "204"),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Delete Publisher",
        tags = ["Publisher"]
    )
    fun deletePublisher(ctx: Context): Unit = Utils.query(description = "Delete Publisher") {
        val publisher = ctx.getPublisher()
        publisher.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }
}