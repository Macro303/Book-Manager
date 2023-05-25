package github.buriedincode.bookshelf

import github.buriedincode.bookshelf.controllers.*
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.openapi.OpenApiContact
import io.javalin.openapi.OpenApiInfo
import io.javalin.openapi.OpenApiLicense
import io.javalin.openapi.OpenApiServer
import io.javalin.openapi.plugin.OpenApiConfiguration
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.redoc.ReDocConfiguration
import io.javalin.openapi.plugin.redoc.ReDocPlugin
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerPlugin
import org.apache.logging.log4j.kotlin.logger

private const val VERSION = "0.0.0"

fun main() {
    val logger = logger("github.buriedincode.bookshelf.App")
    val app = Javalin.create {
        it.routing.ignoreTrailingSlashes = true
        it.routing.treatMultipleSlashesAsSingleSlash = true
        it.requestLogger.http { ctx, ms ->
            logger.info("${ctx.method()} - ${ctx.path()} - ${ctx.statusCode()} - ${ctx.ip()} => ${ms}ms")
        }
        it.plugins.register(OpenApiPlugin(OpenApiConfiguration().apply {
            info = OpenApiInfo().apply {
                title = "Bookshelf API"
                description = "Lots and lots of well described and documented APIs."
                contact = OpenApiContact().apply {
                    name = "Jonah Jackson"
                    url = "https://github.com/Buried-In-Code/Bookshelf"
                    email = "BuriedInCode@tuta.io"
                }
                license = OpenApiLicense().apply {
                    name = "MIT License"
                    identifier = "MIT"
                }
                version = VERSION
            }
            servers = arrayOf(OpenApiServer().apply {
                url = "http://localhost:8003/api/v${VERSION.split(".")[0]}/"
                description = "Local DEV Server"
            })
        }))
        it.plugins.register(SwaggerPlugin(SwaggerConfiguration().apply {
            uiPath = "/docs"
        }))
        it.plugins.register(ReDocPlugin(ReDocConfiguration()))
    }
    app.routes {
        path("html") {}
        path("api") {
            path("v${VERSION.split(".")[0]}") {
                path("books") {
                    get(BookController::listBooks)
                    post(BookController::createBook)
                    path("{book-id}") {
                        get(BookController::getBook)
                        put(BookController::updateBook)
                        delete(BookController::deleteBook)
                    }
                }
                path("genres") {
                    get(GenreController::listGenres)
                    post(GenreController::createGenre)
                    path("{genre-id}") {
                        get(GenreController::getGenre)
                        put(GenreController::updateGenre)
                        delete(GenreController::deleteGenre)
                    }
                }
                path("publishers") {
                    get(PublisherController::listPublishers)
                    post(PublisherController::createPublisher)
                    path("{publisher-id}") {
                        get(PublisherController::getPublisher)
                        put(PublisherController::updatePublisher)
                        delete(PublisherController::deletePublisher)
                    }
                }
                path("series") {
                    get(SeriesController::listSeries)
                    post(SeriesController::createSeries)
                    path("{series-id}") {
                        get(SeriesController::getSeries)
                        put(SeriesController::updateSeries)
                        delete(SeriesController::deleteSeries)
                    }
                }
                path("users") {
                    get(UserController::listUsers)
                    post(UserController::createUser)
                    path("{user-id}") {
                        get(UserController::getUser)
                        put(UserController::updateUser)
                        delete(UserController::deleteUser)
                    }
                }
            }
        }
    }
    app.start(Settings.INSTANCE.websiteHost, Settings.INSTANCE.websitePort)
}
