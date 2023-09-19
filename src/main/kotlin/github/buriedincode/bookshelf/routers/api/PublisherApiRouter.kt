package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.ErrorResponse
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.PublisherEntry
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.PublisherInput
import github.buriedincode.bookshelf.tables.PublisherTable
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyValidator
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiRequestBody
import io.javalin.openapi.OpenApiResponse
import org.apache.logging.log4j.kotlin.Logging

object PublisherApiRouter : CrudHandler, Logging {
    private fun getResource(resourceId: String): Publisher {
        return resourceId.toLongOrNull()?.let {
            Publisher.findById(id = it) ?: throw NotFoundResponse(message = "Publisher not found")
        } ?: throw BadRequestResponse(message = "Invalid Publisher Id")
    }

    private fun Context.getBody(): PublisherInput =
        this.bodyValidator<PublisherInput>()
            .check({ it.title.isNotBlank() }, error = "Title must not be empty")
            .get()

    @OpenApi(
        description = "List all Publishers",
        methods = [HttpMethod.GET],
        operationId = "listPublishers",
        path = "/publishers",
        queryParams = [
            OpenApiParam(name = "title", type = String::class),
        ],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(Array<PublisherEntry>::class)]),
        ],
        summary = "List all Publishers",
        tags = ["Publisher"],
    )
    override fun getAll(ctx: Context): Unit =
        Utils.query {
            var publishers = Publisher.all().toList()
            val title = ctx.queryParam("title")
            if (title != null) {
                publishers = publishers.filter {
                    it.title.contains(title, ignoreCase = true) || title.contains(
                        it.title,
                        ignoreCase = true,
                    )
                }
            }
            ctx.json(publishers.sorted().map { it.toJson() })
        }

    @OpenApi(
        description = "Create Publisher",
        methods = [HttpMethod.POST],
        operationId = "createPublisher",
        path = "/publishers",
        requestBody = OpenApiRequestBody(content = [OpenApiContent(PublisherInput::class)], required = true),
        responses = [
            OpenApiResponse(
                status = "201",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Publisher::class)],
            ),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Create Publisher",
        tags = ["Publisher"],
    )
    override fun create(ctx: Context): Unit =
        Utils.query {
            val body = ctx.getBody()
            val exists = Publisher.find {
                PublisherTable.titleCol eq body.title
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse(message = "Publisher already exists")
            }
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
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Publisher::class)],
            ),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Get Publisher by id",
        tags = ["Publisher"],
    )
    override fun getOne(
        ctx: Context,
        resourceId: String,
    ): Unit =
        Utils.query {
            val publisher = getResource(resourceId = resourceId)
            ctx.json(publisher.toJson(showAll = true))
        }

    @OpenApi(
        description = "Update Publisher",
        methods = [HttpMethod.PATCH],
        operationId = "updatePublisher",
        path = "/publishers/{publisher-id}",
        pathParams = [OpenApiParam(name = "publisher-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(PublisherInput::class)], required = true),
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(github.buriedincode.bookshelf.docs.Publisher::class)],
            ),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Update Publisher",
        tags = ["Publisher"],
    )
    override fun update(
        ctx: Context,
        resourceId: String,
    ): Unit =
        Utils.query {
            val publisher = getResource(resourceId = resourceId)
            val body = ctx.getBody()
            val exists = Publisher.find {
                PublisherTable.titleCol eq body.title
            }.firstOrNull()
            if (exists != null && exists != publisher) {
                throw ConflictResponse(message = "Publisher already exists")
            }
            publisher.title = body.title

            ctx.json(publisher.toJson(showAll = true))
        }

    @OpenApi(
        description = "Delete Publisher",
        methods = [HttpMethod.DELETE],
        operationId = "deletePublisher",
        path = "/publishers/{publisher-id}",
        pathParams = [OpenApiParam(name = "publisher-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "204"),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Delete Publisher",
        tags = ["Publisher"],
    )
    override fun delete(
        ctx: Context,
        resourceId: String,
    ): Unit =
        Utils.query {
            val publisher = getResource(resourceId = resourceId)
            publisher.delete()
            ctx.status(HttpStatus.NO_CONTENT)
        }
}
