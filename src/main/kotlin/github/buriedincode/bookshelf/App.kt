package github.buriedincode.bookshelf

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import github.buriedincode.bookshelf.Utils.VERSION
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.routers.*
import github.buriedincode.bookshelf.routers.api.*
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
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
import io.javalin.validation.ValidationException
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.Logging
import java.nio.file.Path
import java.time.Duration

object App: Logging {
    private fun toHumanReadable(milliseconds: Float): String {
        val duration = Duration.ofMillis(milliseconds.toLong())
        val minutes = duration.toMinutes()
        if (minutes > 0) {
            val seconds = duration.minusMinutes(minutes).toSeconds()
            val millis = duration.minusMinutes(minutes).minusSeconds(seconds)
            return "${minutes}min ${seconds}sec ${millis}ms"
        }
        val seconds = duration.toSeconds()
        if (seconds > 0) {
            val millis = duration.minusSeconds(seconds).toMillis()
            return "${seconds}sec ${millis}ms"
        }
        return "${duration.toMillis()}ms"
    }
    
    private fun createTemplateEngine(): TemplateEngine {
        return if (Settings.load().env == Environment.DEV) {
            val codeResolver = DirectoryCodeResolver(Path.of("src", "main", "jte"))
            TemplateEngine.create(codeResolver, ContentType.Html)
        } else {
            TemplateEngine.createPrecompiled(Path.of("jte-classes"), ContentType.Html)
        }
    }
    
    fun start(settings: Settings) {
        val engine = createTemplateEngine()
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
                logger.log(level, "${ctx.statusCode()}: ${ctx.method()} - ${ctx.path()} => ${toHumanReadable(ms)}")
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
                    url = "http://${settings.website.host}:${settings.website.port}/api/"
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
                get { ctx ->
                    Utils.query {
                        val cookie = ctx.cookie("session-id")?.toLongOrNull() ?: -1L
                        val session = User.findById(id = cookie)
                        if (session == null)
                            ctx.render("templates/index.kte")
                        else
                            ctx.redirect("/users/${session.id.value}")
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
                            patch(BookApiRouter::wishBook)
                            delete(BookApiRouter::unwishBook)
                        }
                        path("collect") {
                            patch(BookApiRouter::collectBook)
                            delete(BookApiRouter::discardBook)
                        }
                        path("read") {
                            patch(BookApiRouter::readBook)
                            delete(BookApiRouter::unreadBook)
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
                    path("{user-id}") {
                        crud(UserApiRouter)
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
            throw BadRequestResponse(message = "Validation Error", details = details.mapValues { it.value.joinToString(", ") })
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
    App.start(settings=settings)
}
