package github.buriedincode.bookshelf

import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.routers.api.BookApiRouter
import github.buriedincode.bookshelf.routers.api.CreatorApiRouter
import github.buriedincode.bookshelf.routers.api.GenreApiRouter
import github.buriedincode.bookshelf.routers.api.PublisherApiRouter
import github.buriedincode.bookshelf.routers.api.RoleApiRouter
import github.buriedincode.bookshelf.routers.api.SeriesApiRouter
import github.buriedincode.bookshelf.routers.api.UserApiRouter
import github.buriedincode.bookshelf.routers.html.BookHtmlRouter
import github.buriedincode.bookshelf.routers.html.CreatorHtmlRouter
import github.buriedincode.bookshelf.routers.html.GenreHtmlRouter
import github.buriedincode.bookshelf.routers.html.PublisherHtmlRouter
import github.buriedincode.bookshelf.routers.html.RoleHtmlRouter
import github.buriedincode.bookshelf.routers.html.SeriesHtmlRouter
import github.buriedincode.bookshelf.routers.html.UserHtmlRouter
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.ApiBuilder.put
import io.javalin.http.ContentType
import io.javalin.rendering.template.JavalinJte
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.Logging
import java.nio.file.Path
import kotlin.io.path.div
import gg.jte.ContentType as JteType

object App : Logging {
    private fun createTemplateEngine(environment: Settings.Environment): TemplateEngine {
        return if (environment == Settings.Environment.DEV) {
            val codeResolver = DirectoryCodeResolver(Path.of("src") / "main" / "jte")
            TemplateEngine.create(codeResolver, JteType.Html)
        } else {
            TemplateEngine.createPrecompiled(Path.of("jte-classes"), JteType.Html)
        }
    }

    private fun createJavalinApp(): Javalin {
        return Javalin.create {
            it.http.prefer405over404 = true
            it.http.defaultContentType = ContentType.JSON
            it.requestLogger.http { ctx, ms ->
                val level = when {
                    ctx.statusCode() in (100..<200) -> Level.WARN
                    ctx.statusCode() in (200..<300) -> Level.INFO
                    ctx.statusCode() in (300..<400) -> Level.INFO
                    ctx.statusCode() in (400..<500) -> Level.WARN
                    else -> Level.ERROR
                }
                logger.log(level, "${ctx.statusCode()}: ${ctx.method()} - ${ctx.path()} => ${Utils.toHumanReadable(ms)}")
            }
            it.routing.ignoreTrailingSlashes = true
            it.routing.treatMultipleSlashesAsSingleSlash = true
            it.routing.caseInsensitiveRoutes = true
            it.staticFiles.add {
                it.hostedPath = "/static"
                it.directory = "/static"
            }
        }
    }

    fun start(settings: Settings) {
        val engine = createTemplateEngine(environment = settings.environment)
        engine.setTrimControlStructures(true)
        JavalinJte.init(templateEngine = engine)

        val app = createJavalinApp()
        app.routes {
            path("/") {
                get { ctx ->
                    Utils.query {
                        ctx.render(
                            filePath = "templates/index.kte",
                            model = mapOf(
                                "session" to ctx.cookie("bookshelf_session-id")?.toLongOrNull()?.let {
                                    User.findById(it)
                                },
                                "users" to User.all().toList(),
                            ),
                        )
                    }
                }
                path("books") {
                    get(BookHtmlRouter::listEndpoint)
                    get("create", BookHtmlRouter::createEndpoint)
                    path("{book-id}") {
                        get(BookHtmlRouter::viewEndpoint)
                        get("update", BookHtmlRouter::updateEndpoint)
                    }
                }
                path("creators") {
                    get(CreatorHtmlRouter::listEndpoint)
                    get("create", CreatorHtmlRouter::createEndpoint)
                    path("{creator-id}") {
                        get(CreatorHtmlRouter::viewEndpoint)
                        get("update", CreatorHtmlRouter::updateEndpoint)
                    }
                }
                path("genres") {
                    get(GenreHtmlRouter::listEndpoint)
                    get("create", GenreHtmlRouter::createEndpoint)
                    path("{genre-id}") {
                        get(GenreHtmlRouter::viewEndpoint)
                        get("update", GenreHtmlRouter::updateEndpoint)
                    }
                }
                path("publishers") {
                    get(PublisherHtmlRouter::listEndpoint)
                    get("create", PublisherHtmlRouter::createEndpoint)
                    path("{publisher-id}") {
                        get(PublisherHtmlRouter::viewEndpoint)
                        get("update", PublisherHtmlRouter::updateEndpoint)
                    }
                }
                path("roles") {
                    get(RoleHtmlRouter::listEndpoint)
                    get("create", RoleHtmlRouter::createEndpoint)
                    path("{role-id}") {
                        get(RoleHtmlRouter::viewEndpoint)
                        get("update", RoleHtmlRouter::updateEndpoint)
                    }
                }
                path("series") {
                    get(SeriesHtmlRouter::listEndpoint)
                    get("create", SeriesHtmlRouter::createEndpoint)
                    path("{series-id}") {
                        get(SeriesHtmlRouter::viewEndpoint)
                        get("update", SeriesHtmlRouter::updateEndpoint)
                    }
                }
                path("users") {
                    get(UserHtmlRouter::listEndpoint)
                    get("create", UserHtmlRouter::createEndpoint)
                    path("{user-id}") {
                        get(UserHtmlRouter::viewEndpoint)
                        get("update", UserHtmlRouter::updateEndpoint)
                        get("wishlist", UserHtmlRouter::wishlistEndpoint)
                    }
                }
            }
            path("api") {
                path("books") {
                    get(BookApiRouter::listEndpoint)
                    post(BookApiRouter::createEndpoint)
                    path("{book-id}") {
                        get(BookApiRouter::getEndpoint)
                        put(BookApiRouter::updateEndpoint)
                        delete(BookApiRouter::deleteEndpoint)
                        put("pull", BookApiRouter::pullBook)
                        path("collect") {
                            post(BookApiRouter::collectBook)
                            delete(BookApiRouter::discardBook)
                        }
                        path("wish") {
                            post(BookApiRouter::addWisher)
                            delete(BookApiRouter::removeWisher)
                        }
                        path("read") {
                            post(BookApiRouter::addReader)
                            delete(BookApiRouter::removeReader)
                        }
                        path("credits") {
                            post(BookApiRouter::addCredit)
                            delete(BookApiRouter::removeCredit)
                        }
                        path("genres") {
                            post(BookApiRouter::addGenre)
                            delete(BookApiRouter::removeGenre)
                        }
                        path("series") {
                            post(BookApiRouter::addSeries)
                            delete(BookApiRouter::removeSeries)
                        }
                    }
                }
                path("creators") {
                    get(CreatorApiRouter::listEndpoint)
                    post(CreatorApiRouter::createEndpoint)
                    path("{creator-id}") {
                        get(CreatorApiRouter::getEndpoint)
                        put(CreatorApiRouter::updateEndpoint)
                        delete(CreatorApiRouter::deleteEndpoint)
                        path("credits") {
                            post(CreatorApiRouter::addCredit)
                            delete(CreatorApiRouter::removeCredit)
                        }
                    }
                }
                path("genres") {
                    get(GenreApiRouter::listEndpoint)
                    post(GenreApiRouter::createEndpoint)
                    path("{genre-id}") {
                        get(GenreApiRouter::getEndpoint)
                        put(GenreApiRouter::updateEndpoint)
                        delete(GenreApiRouter::deleteEndpoint)
                        path("books") {
                            post(GenreApiRouter::addBook)
                            delete(GenreApiRouter::removeBook)
                        }
                    }
                }
                path("publishers") {
                    get(PublisherApiRouter::listEndpoint)
                    post(PublisherApiRouter::createEndpoint)
                    path("{publisher-id}") {
                        get(PublisherApiRouter::getEndpoint)
                        put(PublisherApiRouter::updateEndpoint)
                        delete(PublisherApiRouter::deleteEndpoint)
                        path("books") {
                            post(PublisherApiRouter::addBook)
                            delete(PublisherApiRouter::removeBook)
                        }
                    }
                }
                path("roles") {
                    get(RoleApiRouter::listEndpoint)
                    post(RoleApiRouter::createEndpoint)
                    path("{role-id}") {
                        get(RoleApiRouter::getEndpoint)
                        put(RoleApiRouter::updateEndpoint)
                        delete(RoleApiRouter::deleteEndpoint)
                        path("credits") {
                            post(RoleApiRouter::addCredit)
                            delete(RoleApiRouter::removeCredit)
                        }
                    }
                }
                path("series") {
                    get(SeriesApiRouter::listEndpoint)
                    post(SeriesApiRouter::createEndpoint)
                    path("{series-id}") {
                        get(SeriesApiRouter::getEndpoint)
                        put(SeriesApiRouter::updateEndpoint)
                        delete(SeriesApiRouter::deleteEndpoint)
                        path("books") {
                            post(SeriesApiRouter::addBook)
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
                            post(UserApiRouter::addReadBook)
                            delete(UserApiRouter::removeReadBook)
                        }
                        path("wished") {
                            post(UserApiRouter::addWishedBook)
                            delete(UserApiRouter::removeWishedBook)
                        }
                    }
                }
            }
        }
        app.start(settings.website.host, settings.website.port)
    }
}

fun main(
    @Suppress("UNUSED_PARAMETER") vararg args: String,
) {
    println("Bookshelf v${Utils.VERSION}")
    println("Kotlin v${KotlinVersion.CURRENT}")
    println("Java v${System.getProperty("java.version")}")
    println("Arch: ${System.getProperty("os.arch")}")

    val settings = Settings.load()
    println(settings)

    App.start(settings = settings)
}
