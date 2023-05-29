package github.buriedincode.bookshelf

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import github.buriedincode.bookshelf.routers.api.*
import github.buriedincode.bookshelf.routers.html.*
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
import io.javalin.rendering.template.JavalinJte
import org.apache.logging.log4j.kotlin.logger
import java.nio.file.Path

private const val VERSION = "0.0.0"

fun main() {
    val logger = logger("github.buriedincode.bookshelf.App")
    JavalinJte.init(templateEngine = createTemplateEngine())
    val app = Javalin.create {
        it.http.prefer405over404 = true
        it.requestLogger.http { ctx, ms ->
            logger.info("${ctx.method()} - ${ctx.matchedPath()} - ${ctx.statusCode()} - ${ctx.ip()} => ${ms}ms")
        }
        it.routing.ignoreTrailingSlashes = true
        it.routing.treatMultipleSlashesAsSingleSlash = true
        it.staticFiles.add {
            it.hostedPath = "/static"
            it.directory = "/static"
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
                url = "http://localhost:8003/api/v${VERSION.split(".")[0]}.${VERSION.split(".")[1]}/"
                description = "Local DEV Server"
            })
        }))
        it.plugins.register(SwaggerPlugin(SwaggerConfiguration().apply {
            uiPath = "/docs"
        }))
        it.plugins.register(ReDocPlugin(ReDocConfiguration()))
    }
    app.routes {
        path("/") {
            get {
                it.render("templates/index.kte")
            }
            path("books") {
                get(BookHtmlRouter::listEndpoint)
                path("{book-id}") {
                    get(BookHtmlRouter::viewEndpoint)
                    get("edit", BookHtmlRouter::editEndpoint)
                }
            }
            path("genres") {
                get(GenreHtmlRouter::listEndpoint)
                path("{genre-id}") {
                    get(GenreHtmlRouter::viewEndpoint)
                    get("edit", GenreHtmlRouter::editEndpoint)
                }
            }
            path("publishers") {
                get(PublisherHtmlRouter::listEndpoint)
                path("{publisher-id}") {
                    get(PublisherHtmlRouter::viewEndpoint)
                    get("edit", PublisherHtmlRouter::editEndpoint)
                }
            }
            path("series") {
                get(SeriesHtmlRouter::listEndpoint)
                path("{series-id}") {
                    get(SeriesHtmlRouter::viewEndpoint)
                    get("edit", SeriesHtmlRouter::editEndpoint)
                }
            }
            path("users") {
                get(UserHtmlRouter::listEndpoint)
                path("{user-id}") {
                    get(UserHtmlRouter::viewEndpoint)
                    get("edit", UserHtmlRouter::editEndpoint)
                    get("wishlist", UserHtmlRouter::wishlistEndpoint)
                }
            }
        }
        path("api") {
            path("v${VERSION.split(".")[0]}.${VERSION.split(".")[1]}") {
                path("books") {
                    get(BookApiRouter::listBooks)
                    post(BookApiRouter::createBook)
                    path("{book-id}") {
                        get(BookApiRouter::getBook)
                        put(BookApiRouter::updateBook)
                        delete(BookApiRouter::deleteBook)
                        patch("discard", BookApiRouter::discardBook)
                        patch("collect", BookApiRouter::collectBook)
                        patch("unread", BookApiRouter::unreadBook)
                        patch("read", BookApiRouter::readBook)
                        patch("unwish", BookApiRouter::unwishBook)
                        patch("wish", BookApiRouter::wishBook)
                        path("creators") {
                            patch(BookApiRouter::addCreator)
                            delete(BookApiRouter::removeCreator)
                        }
                        path("genres") {
                            patch(BookApiRouter::addGenre)
                            delete(BookApiRouter::removeGenre)
                        }
                        path("series") {
                            patch(BookApiRouter::addSeries)
                            delete(BookApiRouter::removeSeries)
                        }
                    }
                    post("import", BookApiRouter::importBook)
                }
                path("genres") {
                    get(GenreApiRouter::listGenres)
                    post(GenreApiRouter::createGenre)
                    path("{genre-id}") {
                        get(GenreApiRouter::getGenre)
                        put(GenreApiRouter::updateGenre)
                        delete(GenreApiRouter::deleteGenre)
                        path("books") {
                            patch(GenreApiRouter::addBook)
                            delete(GenreApiRouter::removeBook)
                        }
                    }
                }
                path("publishers") {
                    get(PublisherApiRouter::listPublishers)
                    post(PublisherApiRouter::createPublisher)
                    path("{publisher-id}") {
                        get(PublisherApiRouter::getPublisher)
                        put(PublisherApiRouter::updatePublisher)
                        delete(PublisherApiRouter::deletePublisher)
                    }
                }
                path("series") {
                    get(SeriesApiRouter::listSeries)
                    post(SeriesApiRouter::createSeries)
                    path("{series-id}") {
                        get(SeriesApiRouter::getSeries)
                        put(SeriesApiRouter::updateSeries)
                        delete(SeriesApiRouter::deleteSeries)
                        path("books") {
                            patch(SeriesApiRouter::addBook)
                            delete(SeriesApiRouter::removeBook)
                        }
                    }
                }
                path("users") {
                    get(UserApiRouter::listUsers)
                    post(UserApiRouter::createUser)
                    path("{user-id}") {
                        get(UserApiRouter::getUser)
                        put(UserApiRouter::updateUser)
                        delete(UserApiRouter::deleteUser)
                        path("read") {
                            patch(UserApiRouter::addReadBook)
                            delete(UserApiRouter::removeReadBook)
                        }
                        path("wisher") {
                            patch(UserApiRouter::addWishedBook)
                            delete(UserApiRouter::removeWishedBook)
                        }
                    }
                }
            }
        }
    }
    app.start(Settings.INSTANCE.websiteHost, Settings.INSTANCE.websitePort)
}

private fun createTemplateEngine(): TemplateEngine {
    return if (Settings.INSTANCE.websiteDevelopment) {
        val codeResolver = DirectoryCodeResolver(Path.of("src", "main", "jte"))
        TemplateEngine.create(codeResolver, ContentType.Html)
    } else {
        TemplateEngine.createPrecompiled(Path.of("jte-classes"), ContentType.Html)
    }
}