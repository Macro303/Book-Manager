package github.buriedincode.bookshelf

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import github.buriedincode.bookshelf.App.authenticateUser
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.models.UserRole
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
import github.buriedincode.bookshelf.tables.UserTable
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.ApiBuilder.put
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import io.javalin.rendering.template.JavalinJte
import io.javalin.validation.ValidationException
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.Logging
import java.nio.file.Path
import kotlin.io.path.div

object App : Logging {
    private fun createTemplateEngine(environment: Settings.Environment): TemplateEngine {
        return if (environment == Settings.Environment.DEV) {
            val codeResolver = DirectoryCodeResolver(Path.of("src") / "main" / "jte")
            TemplateEngine.create(codeResolver, ContentType.Html)
        } else {
            TemplateEngine.createPrecompiled(Path.of("jte-classes"), ContentType.Html)
        }
    }

    private fun Context.authenticateUser(): User? {
        val credentials = basicAuthCredentials()
        if (credentials != null) {
            return Utils.query {
                val user = User.find {
                    (UserTable.usernameCol eq credentials.username)
                }.firstOrNull()
                if (user != null) {
                    this.attribute("session", user)
                }
                return@query user
            }
        }
        return null
    }

    private fun Context.redirectUnauthorized() {
        val resourceType = this.path().split("/")[1]
        when (val endPath = this.path().split("/")[2]) {
            "create" -> this.redirect(resourceType)
            else -> this.redirect("$resourceType/$endPath")
        }
    }

    private fun createJavalinApp(): Javalin {
        return Javalin.create {
            it.http.prefer405over404 = true
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
            it.accessManager { handler, ctx, minRequiredRoles ->
                val user = ctx.authenticateUser()
                val userRoles = user?.let { setOf(it.role) } ?: emptySet()
                if (userRoles.intersect(minRequiredRoles).isNotEmpty()) {
                    handler.handle(ctx)
                } else if (ctx.path().startsWith("/api") || ctx.path().startsWith("/uploads")) {
                    throw UnauthorizedResponse()
                } else {
                    ctx.redirectUnauthorized()
                }
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
                get({ ctx ->
                    Utils.query {
                        ctx.render(
                            filePath = "templates/index.kte",
                            model = mapOf(
                                "session" to ctx.attribute<User>("session"),
                            ),
                        )
                    }
                }, UserRole.GUEST)
                path("books") {
                    get(BookHtmlRouter::listEndpoint, UserRole.GUEST)
                    get("create", BookHtmlRouter::createEndpoint, UserRole.MODERATOR)
                    path("{book-id}") {
                        get(BookHtmlRouter::viewEndpoint, UserRole.GUEST)
                        get("update", BookHtmlRouter::updateEndpoint, UserRole.MODERATOR)
                    }
                }
                path("creators") {
                    get(CreatorHtmlRouter::listEndpoint, UserRole.GUEST)
                    get("create", CreatorHtmlRouter::createEndpoint, UserRole.MODERATOR)
                    path("{creator-id}") {
                        get(CreatorHtmlRouter::viewEndpoint, UserRole.GUEST)
                        get("update", CreatorHtmlRouter::updateEndpoint, UserRole.MODERATOR)
                    }
                }
                path("genres") {
                    get(GenreHtmlRouter::listEndpoint, UserRole.GUEST)
                    get("create", GenreHtmlRouter::createEndpoint, UserRole.MODERATOR)
                    path("{genre-id}") {
                        get(GenreHtmlRouter::viewEndpoint, UserRole.GUEST)
                        get("update", GenreHtmlRouter::updateEndpoint, UserRole.MODERATOR)
                    }
                }
                path("publishers") {
                    get(PublisherHtmlRouter::listEndpoint, UserRole.GUEST)
                    get("create", PublisherHtmlRouter::createEndpoint, UserRole.MODERATOR)
                    path("{publisher-id}") {
                        get(PublisherHtmlRouter::viewEndpoint, UserRole.GUEST)
                        get("update", PublisherHtmlRouter::updateEndpoint, UserRole.MODERATOR)
                    }
                }
                path("roles") {
                    get(RoleHtmlRouter::listEndpoint, UserRole.GUEST)
                    get("create", RoleHtmlRouter::createEndpoint, UserRole.MODERATOR)
                    path("{role-id}") {
                        get(RoleHtmlRouter::viewEndpoint, UserRole.GUEST)
                        get("update", RoleHtmlRouter::updateEndpoint, UserRole.MODERATOR)
                    }
                }
                path("series") {
                    get(SeriesHtmlRouter::listEndpoint, UserRole.GUEST)
                    get("create", SeriesHtmlRouter::createEndpoint, UserRole.MODERATOR)
                    path("{series-id}") {
                        get(SeriesHtmlRouter::viewEndpoint, UserRole.GUEST)
                        get("update", SeriesHtmlRouter::updateEndpoint, UserRole.MODERATOR)
                    }
                }
                path("users") {
                    get(UserHtmlRouter::listEndpoint, UserRole.GUEST)
                    get("create", UserHtmlRouter::createEndpoint, UserRole.GUEST)
                    path("{user-id}") {
                        get(UserHtmlRouter::viewEndpoint, UserRole.GUEST)
                        get("update", UserHtmlRouter::updateEndpoint, UserRole.USER)
                        get("wishlist", UserHtmlRouter::wishlistEndpoint, UserRole.GUEST)
                    }
                }
            }
            path("api") {
                path("books") {
                    get(BookApiRouter::listEndpoint, UserRole.GUEST)
                    post(BookApiRouter::createEndpoint, UserRole.MODERATOR)
                    path("{book-id}") {
                        get(BookApiRouter::getEndpoint, UserRole.GUEST)
                        put(BookApiRouter::updateEndpoint, UserRole.MODERATOR)
                        delete(BookApiRouter::deleteEndpoint, UserRole.ADMIN)
                        put("pull", BookApiRouter::pullBook, UserRole.MODERATOR)
                        path("wish") {
                            patch(BookApiRouter::addWisher, UserRole.USER)
                            delete(BookApiRouter::removeWisher, UserRole.USER)
                        }
                        path("collect") {
                            patch(BookApiRouter::collectBook, UserRole.USER)
                            delete(BookApiRouter::discardBook, UserRole.USER)
                        }
                        path("read") {
                            patch(BookApiRouter::addReader, UserRole.USER)
                            delete(BookApiRouter::removeReader, UserRole.USER)
                        }
                        path("credits") {
                            patch(BookApiRouter::addCredit, UserRole.MODERATOR)
                            delete(BookApiRouter::removeCredit, UserRole.MODERATOR)
                        }
                        path("genres") {
                            patch(BookApiRouter::addGenre, UserRole.MODERATOR)
                            delete(BookApiRouter::removeGenre, UserRole.MODERATOR)
                        }
                        path("series") {
                            patch(BookApiRouter::addSeries, UserRole.MODERATOR)
                            delete(BookApiRouter::removeSeries, UserRole.MODERATOR)
                        }
                    }
                }
                path("creators") {
                    get(CreatorApiRouter::listEndpoint, UserRole.GUEST)
                    post(CreatorApiRouter::createEndpoint, UserRole.MODERATOR)
                    path("{creator-id}") {
                        get(CreatorApiRouter::getEndpoint, UserRole.GUEST)
                        put(CreatorApiRouter::updateEndpoint, UserRole.MODERATOR)
                        delete(CreatorApiRouter::deleteEndpoint, UserRole.ADMIN)
                    }
                }
                path("genres") {
                    get(GenreApiRouter::listEndpoint, UserRole.GUEST)
                    post(GenreApiRouter::createEndpoint, UserRole.MODERATOR)
                    path("{genre-id}") {
                        get(GenreApiRouter::getEndpoint, UserRole.GUEST)
                        put(GenreApiRouter::updateEndpoint, UserRole.MODERATOR)
                        delete(GenreApiRouter::deleteEndpoint, UserRole.ADMIN)
                    }
                }
                path("publishers") {
                    get(PublisherApiRouter::listEndpoint, UserRole.GUEST)
                    post(PublisherApiRouter::createEndpoint, UserRole.MODERATOR)
                    path("{publisher-id}") {
                        get(PublisherApiRouter::getEndpoint, UserRole.GUEST)
                        put(PublisherApiRouter::updateEndpoint, UserRole.MODERATOR)
                        delete(PublisherApiRouter::deleteEndpoint, UserRole.ADMIN)
                    }
                }
                path("roles") {
                    get(RoleApiRouter::listEndpoint, UserRole.GUEST)
                    post(RoleApiRouter::createEndpoint, UserRole.MODERATOR)
                    path("{role-id}") {
                        get(RoleApiRouter::getEndpoint, UserRole.GUEST)
                        put(RoleApiRouter::updateEndpoint, UserRole.MODERATOR)
                        delete(RoleApiRouter::deleteEndpoint, UserRole.ADMIN)
                    }
                }
                path("series") {
                    get(SeriesApiRouter::listEndpoint, UserRole.GUEST)
                    post(SeriesApiRouter::createEndpoint, UserRole.MODERATOR)
                    path("{series-id}") {
                        get(SeriesApiRouter::getEndpoint, UserRole.GUEST)
                        put(SeriesApiRouter::updateEndpoint, UserRole.MODERATOR)
                        delete(SeriesApiRouter::deleteEndpoint, UserRole.ADMIN)
                    }
                }
                path("users") {
                    get(UserApiRouter::listEndpoint, UserRole.GUEST)
                    post(UserApiRouter::createEndpoint, UserRole.GUEST)
                    path("{user-id}") {
                        get(UserApiRouter::getEndpoint, UserRole.GUEST)
                        put(UserApiRouter::updateEndpoint, UserRole.USER)
                        delete(UserApiRouter::deleteEndpoint, UserRole.USER)
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
