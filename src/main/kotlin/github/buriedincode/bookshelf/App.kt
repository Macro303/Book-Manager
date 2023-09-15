package github.buriedincode.bookshelf

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.routers.BookHtmlRouter
import github.buriedincode.bookshelf.routers.CreatorHtmlRouter
import github.buriedincode.bookshelf.routers.GenreHtmlRouter
import github.buriedincode.bookshelf.routers.PublisherHtmlRouter
import github.buriedincode.bookshelf.routers.RoleHtmlRouter
import github.buriedincode.bookshelf.routers.SeriesHtmlRouter
import github.buriedincode.bookshelf.routers.UserHtmlRouter
import github.buriedincode.bookshelf.routers.api.BookApiRouter
import github.buriedincode.bookshelf.routers.api.CreatorApiRouter
import github.buriedincode.bookshelf.routers.api.GenreApiRouter
import github.buriedincode.bookshelf.routers.api.PublisherApiRouter
import github.buriedincode.bookshelf.routers.api.RoleApiRouter
import github.buriedincode.bookshelf.routers.api.SeriesApiRouter
import github.buriedincode.bookshelf.routers.api.UserApiRouter
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.ApiBuilder.put
import io.javalin.http.BadRequestResponse
import io.javalin.openapi.OpenApiContact
import io.javalin.openapi.OpenApiLicense
import io.javalin.openapi.OpenApiServer
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.OpenApiPluginConfiguration
import io.javalin.openapi.plugin.redoc.ReDocConfiguration
import io.javalin.openapi.plugin.redoc.ReDocPlugin
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerPlugin
import io.javalin.rendering.template.JavalinJte
import io.javalin.validation.ValidationException
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.Logging
import java.nio.file.Path
import kotlin.io.path.div

object App : Logging {
    private fun createTemplateEngine(environment: Environment): TemplateEngine {
        return if (environment == Environment.DEV) {
            val codeResolver = DirectoryCodeResolver(Path.of("src") / "main" / "jte")
            TemplateEngine.create(codeResolver, ContentType.Html)
        } else {
            TemplateEngine.createPrecompiled(Path.of("jte-classes"), ContentType.Html)
        }
    }

    fun start(settings: Settings) {
        val engine = createTemplateEngine(environment = settings.environment)
        // engine.setTrimControlStructures = true
        JavalinJte.init(templateEngine = engine)
        val app = Javalin.create {
            it.http.prefer405over404 = true
            it.requestLogger.http { ctx, ms ->
                val level = when {
                    ctx.statusCode() in (100 until 200) -> Level.WARN
                    ctx.statusCode() in (200 until 300) -> Level.INFO
                    ctx.statusCode() in (300 until 400) -> Level.INFO
                    ctx.statusCode() in (400 until 500) -> Level.WARN
                    else -> Level.ERROR
                }
                logger.log(level, "${ctx.statusCode()}: ${ctx.method()} - ${ctx.path()} => ${Utils.toHumanReadable(ms)}")
            }
            it.routing.ignoreTrailingSlashes = true
            it.routing.treatMultipleSlashesAsSingleSlash = true
            it.staticFiles.add {
                it.hostedPath = "/static"
                it.directory = "/static"
            }
            it.plugins.register(
                OpenApiPlugin(
                    configuration = OpenApiPluginConfiguration().apply {
                        withDefinitionConfiguration { _, definition ->
                            definition.withOpenApiInfo {
                                it.title = "Bookshelf API"
                                it.summary = "Lots and lots of well described and documented APIs."
                                it.contact = OpenApiContact().apply {
                                    name = "Jonah Jackson"
                                    url = "https://github.com/Buried-In-Code/Bookshelf"
                                    email = "BuriedInCode@tuta.io"
                                }
                                it.license = OpenApiLicense().apply {
                                    name = "MIT License"
                                    identifier = "MIT"
                                }
                                it.version = Utils.VERSION
                            }
                            definition.withServer {
                                OpenApiServer().apply {
                                    url = "http://${settings.website.host}:${settings.website.port}/api"
                                    description = "Local DEV Server"
                                }
                            }
                        }
                    },
                ),
            )
            it.plugins.register(
                SwaggerPlugin(
                    SwaggerConfiguration().apply {
                        uiPath = "/docs"
                    },
                ),
            )
            it.plugins.register(ReDocPlugin(ReDocConfiguration()))
        }
        app.routes {
            path("/") {
                get { ctx ->
                    Utils.query {
                        val cookie = ctx.cookie("bookshelf_session-id")?.toLongOrNull() ?: -1L
                        val session = User.findById(id = cookie)
                        if (session == null) {
                            ctx.render("templates/index.kte")
                        } else {
                            ctx.redirect("/users/${session.id.value}")
                        }
                    }
                }
                path("books") {
                    get(BookHtmlRouter::listEndpoint)
                    path("{book-id}") {
                        get(BookHtmlRouter::viewEndpoint)
                        get("edit", BookHtmlRouter::editEndpoint)
                    }
                }
                path("creators") {
                    get(CreatorHtmlRouter::listEndpoint)
                    path("{creator-id}") {
                        get(CreatorHtmlRouter::viewEndpoint)
                        get("edit", CreatorHtmlRouter::editEndpoint)
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
                path("roles") {
                    get(RoleHtmlRouter::listEndpoint)
                    path("{role-id}") {
                        get(RoleHtmlRouter::viewEndpoint)
                        get("edit", RoleHtmlRouter::editEndpoint)
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
                path("books") {
                    post("import", BookApiRouter::importBook)
                    path("{book-id}") {
                        crud(BookApiRouter)
                        put("refresh", BookApiRouter::refreshBook)
                        path("wish") {
                            patch(BookApiRouter::addWisher)
                            delete(BookApiRouter::removeWisher)
                        }
                        path("collect") {
                            patch(BookApiRouter::collectBook)
                            delete(BookApiRouter::discardBook)
                        }
                        path("read") {
                            patch(BookApiRouter::addReader)
                            delete(BookApiRouter::removeReader)
                        }
                        path("credits") {
                            patch(BookApiRouter::addCredit)
                            delete(BookApiRouter::removeCredit)
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
                }
                path("creators") {
                    path("{creator-id}") {
                        crud(CreatorApiRouter)
                        path("credits") {
                            patch(CreatorApiRouter::addCredit)
                            delete(CreatorApiRouter::removeCredit)
                        }
                    }
                }
                path("genres") {
                    path("{genre-id}") {
                        crud(GenreApiRouter)
                        path("books") {
                            patch(GenreApiRouter::addBook)
                            delete(GenreApiRouter::removeBook)
                        }
                    }
                }
                path("publishers") {
                    path("{publisher-id}") {
                        crud(PublisherApiRouter)
                    }
                }
                path("roles") {
                    path("{role-id}") {
                        crud(RoleApiRouter)
                        path("credits") {
                            patch(RoleApiRouter::addCredit)
                            delete(RoleApiRouter::removeCredit)
                        }
                    }
                }
                path("series") {
                    path("{series-id}") {
                        crud(SeriesApiRouter)
                        path("books") {
                            patch(SeriesApiRouter::addBook)
                            delete(SeriesApiRouter::removeBook)
                        }
                    }
                }
                path("users") {
                    get(UserApiRouter::listEndpoint)
                    post(UserApiRouter::createEndpoint)
                    path("{user-id}") {
                        get(UserApiRouter::getEndpoint)
                        put(UserApiRouter::updateEndpoint)
                        delete(UserApiRouter::deleteEndpoint)
                        path("read") {
                            patch(UserApiRouter::addReadBook)
                            delete(UserApiRouter::removeReadBook)
                        }
                        path("wished") {
                            patch(UserApiRouter::addWishedBook)
                            delete(UserApiRouter::removeWishedBook)
                        }
                    }
                }
            }
        }
        app.exception(ValidationException::class.java) { e, _ ->
            val details = HashMap<String, List<String>>()
            e.errors.forEach { (key, value) ->
                var entry = details.getOrDefault(key, ArrayList())
                entry = entry.plus(value.map { it.message })
                details[key] = entry
            }
            throw BadRequestResponse(
                message = "Validation Error",
                details = details.mapValues { it.value.joinToString(", ") },
            )
        }
        app.start(settings.website.host, settings.website.port)
    }
}

fun main(vararg args: String) {
    println("Kotlin v${KotlinVersion.CURRENT}")
    println("Java v${System.getProperty("java.version")}")
    println("Arch: ${System.getProperty("os.arch")}")
    val settings = Settings.load()
    println(settings.toString())
    App.start(settings = settings)
}
