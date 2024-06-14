package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.asEnumOrNull
import github.buriedincode.bookshelf.Utils.toLocalDateOrNull
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.BookInput
import github.buriedincode.bookshelf.models.BookSeries
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Credit
import github.buriedincode.bookshelf.models.Format
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.IdInput
import github.buriedincode.bookshelf.models.ImportBook
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.ReadBook
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.models.Series
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.services.OpenLibrary
import github.buriedincode.bookshelf.services.getId
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.BookTable
import github.buriedincode.bookshelf.tables.CreatorTable
import github.buriedincode.bookshelf.tables.CreditTable
import github.buriedincode.bookshelf.tables.PublisherTable
import github.buriedincode.bookshelf.tables.ReadBookTable
import github.buriedincode.bookshelf.tables.RoleTable
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.NotImplementedResponse
import io.javalin.http.bodyAsClass
import kotlinx.datetime.toJavaLocalDate
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or

object BookApiRouter : BaseApiRouter<Book>(entity = Book), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = Book.all().toList()
            ctx.queryParam("creator-id")?.toLongOrNull()?.let {
                Creator.findById(it)?.let { creator ->
                    resources = resources.filter { creator in it.credits.map { it.creator } }
                }
            }
            ctx.queryParam("format")?.asEnumOrNull<Format>()?.let { format ->
                resources = resources.filter { format == it.format }
            }
            ctx.queryParam("genre-id")?.toLongOrNull()?.let {
                Genre.findById(it)?.let { genre ->
                    resources = resources.filter { genre in it.genres }
                }
            }
            ctx.queryParam("has-goodreads")?.lowercase()?.toBooleanStrictOrNull()?.let { goodreads ->
                resources = resources.filter { it.goodreadsId != null == goodreads }
            }
            ctx.queryParam("goodreads-id")?.let { goodreads ->
                resources = resources.filter { goodreads == it.goodreadsId }
            }
            ctx.queryParam("has-google-books")?.lowercase()?.toBooleanStrictOrNull()?.let { googleBooks ->
                resources = resources.filter { it.googleBooksId != null == googleBooks }
            }
            ctx.queryParam("google-books-id")?.let { googleBooks ->
                resources = resources.filter { googleBooks == it.googleBooksId }
            }
            ctx.queryParam("has-image")?.lowercase()?.toBooleanStrictOrNull()?.let { image ->
                resources = resources.filter { it.imageUrl != null == image }
            }
            ctx.queryParam("has-isbn")?.lowercase()?.toBooleanStrictOrNull()?.let { isbn ->
                resources = resources.filter { it.isbn != null == isbn }
            }
            ctx.queryParam("isbn")?.let { isbn ->
                resources = resources.filter { isbn == it.isbn }
            }
            ctx.queryParam("is-collected")?.lowercase()?.toBooleanStrictOrNull()?.let { collected ->
                resources = resources.filter { collected == it.isCollected }
            }
            ctx.queryParam("has-library-thing")?.lowercase()?.toBooleanStrictOrNull()?.let { libraryThing ->
                resources = resources.filter { it.libraryThingId != null == libraryThing }
            }
            ctx.queryParam("library-thing-id")?.let { libraryThing ->
                resources = resources.filter { libraryThing == it.libraryThingId }
            }
            ctx.queryParam("has-open-library")?.lowercase()?.toBooleanStrictOrNull()?.let { openLibrary ->
                resources = resources.filter { it.openLibraryId != null == openLibrary }
            }
            ctx.queryParam("open-library-id")?.let { openLibrary ->
                resources = resources.filter { openLibrary == it.openLibraryId }
            }
            ctx.queryParam("before-publish-date")?.toLocalDateOrNull("yyyy-MM-dd")?.let { publishDate ->
                resources = resources.filter { it.publishDate?.isBefore(publishDate) ?: false }
            }
            ctx.queryParam("after-publish-date")?.toLocalDateOrNull("yyyy-MM-dd")?.let { publishDate ->
                resources = resources.filter { it.publishDate?.isAfter(publishDate) ?: false }
            }
            ctx.queryParam("publisher-id")?.toLongOrNull()?.let {
                Publisher.findById(it)?.let { publisher ->
                    resources = resources.filter { publisher == it.publisher }
                }
            }
            ctx.queryParam("reader-id")?.toLongOrNull()?.let { readerId ->
                resources = resources.filter { readerId in it.readers.map { it.user.id.value } }
            }
            ctx.queryParam("series-id")?.toLongOrNull()?.let {
                Series.findById(it)?.let { series ->
                    resources = resources.filter { series in it.series.map { it.series } }
                }
            }
            ctx.queryParam("title")?.let { title ->
                resources = resources.filter {
                    (
                        it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
                    ) ||
                        (
                            it.subtitle?.let {
                                it.contains(title, ignoreCase = true) || title.contains(it, ignoreCase = true)
                            } ?: false
                        )
                }
            }
            ctx.queryParam("wisher-id")?.toLongOrNull()?.let { wisherId ->
                resources = resources.filter { wisherId in it.wishers.map { it.id.value } }
            }
            ctx.json(resources.sorted().map { it.toJson() })
        }
    }

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val body = ctx.bodyAsClass<BookInput>()
            val exists = Book
                .find {
                    (BookTable.titleCol eq body.title) and (BookTable.subtitleCol eq body.subtitle)
                }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse("Book already exists")
            }
            val resource = Book.new {
                this.format = body.format
                this.goodreadsId = body.goodreadsId
                this.googleBooksId = body.googleBooksId
                this.imageUrl = body.imageUrl
                this.isCollected = body.isCollected
                this.isbn = body.isbn
                this.libraryThingId = body.libraryThingId
                this.openLibraryId = body.openLibraryId
                this.publishDate = body.publishDate
                this.publisher = body.publisherId?.let {
                    Publisher.findById(it) ?: throw NotFoundResponse("Publisher not found.")
                }
                this.subtitle = body.subtitle
                this.summary = body.summary
                this.title = body.title
            }

            ctx.status(HttpStatus.CREATED).json(resource.toJson(showAll = true))
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<BookInput>()
            val exists = Book
                .find {
                    (BookTable.titleCol eq body.title) and (BookTable.subtitleCol eq body.subtitle)
                }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse("Book already exists")
            }
            resource.format = body.format
            resource.goodreadsId = body.goodreadsId
            resource.googleBooksId = body.googleBooksId
            resource.imageUrl = body.imageUrl
            resource.isbn = body.isbn
            resource.libraryThingId = body.libraryThingId
            resource.openLibraryId = body.openLibraryId
            resource.publishDate = body.publishDate
            resource.publisher = body.publisherId?.let {
                Publisher.findById(it) ?: throw NotFoundResponse("Publisher not found.")
            }
            resource.subtitle = body.subtitle
            resource.summary = body.summary
            resource.title = body.title

            ctx.json(resource.toJson(showAll = true))
        }
    }

    override fun deleteEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            resource.credits.forEach {
                it.delete()
            }
            resource.readers.forEach {
                it.delete()
            }
            resource.series.forEach {
                it.delete()
            }
            resource.delete()
            ctx.status(HttpStatus.NO_CONTENT)
        }
    }

    fun import(ctx: Context) {
        Utils.query {
            val body = ctx.bodyAsClass<ImportBook>()

            val edition = body.openLibraryId?.let {
                OpenLibrary.getEdition(id = it)
            } ?: body.isbn?.let {
                OpenLibrary.getEditionByISBN(isbn = it)
            } ?: throw NotImplementedResponse(message = "Import only available via OpenLibrary editionId or isbn currently.")
            val work = OpenLibrary.getWork(id = edition.works.first().getId())
            val editionId = edition.getId()
            val workId = work.getId()

            val exists = if (edition.isbn13.isEmpty() && edition.isbn10.isEmpty()) {
                Book
                    .find {
                        (
                            (BookTable.titleCol eq edition.title) and
                                (BookTable.subtitleCol eq edition.subtitle)
                        ) or (BookTable.openLibraryCol eq editionId)
                    }.firstOrNull()
            } else {
                Book
                    .find {
                        (
                            (BookTable.titleCol eq edition.title) and
                                (BookTable.subtitleCol eq edition.subtitle)
                        ) or (
                            BookTable.openLibraryCol eq editionId
                        ) or (
                            BookTable.isbnCol eq (edition.isbn13.firstOrNull() ?: edition.isbn10.first())
                        )
                    }.firstOrNull()
            }
            if (exists != null) {
                throw ConflictResponse("Book already exists")
            }

            val resource = Book.new {
                this.format = Format.PAPERBACK
//                this.genres = SizedCollection(
//                    edition.genres.map {
//                        Genre.find {
//                            GenreTable.titleCol eq it
//                        }.firstOrNull() ?: Genre.new {
//                            this.title = it
//                        }
//                    },
//                )
                this.goodreadsId = edition.identifiers.goodreads.firstOrNull()
                this.googleBooksId = edition.identifiers.google.firstOrNull()
                this.imageUrl = "https://covers.openlibrary.org/b/OLID/$editionId-L.jpg"
                this.isbn = edition.isbn13.firstOrNull() ?: edition.isbn10.firstOrNull()
                this.isCollected = body.isCollected
                this.libraryThingId = edition.identifiers.librarything.firstOrNull()
                this.openLibraryId = editionId
                this.publishDate = edition.publishDate?.toJavaLocalDate()
                this.publisher = edition.publishers.firstOrNull()?.let {
                    Publisher
                        .find {
                            PublisherTable.titleCol eq it
                        }.firstOrNull() ?: Publisher.new {
                        this.title = it
                    }
                }
                this.subtitle = edition.subtitle
//                this.summary = edition.description ?: work.description
                this.title = edition.title
            }
            work.authors
                .map {
                    OpenLibrary.getAuthor(id = it.author.getId())
                }.map {
                    val creator = Creator
                        .find {
                            CreatorTable.nameCol eq it.name
                        }.firstOrNull() ?: Creator.new {
                        this.name = it.name
                    }
//                it.photos.firstOrNull()?.let {
//                    creator.imageUrl = "https://covers.openlibrary.org/a/id/$it-L.jpg"
//                }
                    creator
                }.forEach {
                    val role = Role
                        .find {
                            RoleTable.titleCol eq "Author"
                        }.firstOrNull() ?: Role.new {
                        this.title = "Author"
                    }
                    Credit
                        .find {
                            (CreditTable.bookCol eq resource.id) and
                                (CreditTable.creatorCol eq it.id) and
                                (CreditTable.roleCol eq role.id)
                        }.firstOrNull() ?: Credit.new {
                        this.book = resource
                        this.creator = it
                        this.role = role
                    }
                }
            edition.contributors.forEach {
                val creator = Creator
                    .find {
                        CreatorTable.nameCol eq it.name
                    }.firstOrNull() ?: Creator.new {
                    this.name = it.name
                }
                val role = Role
                    .find {
                        RoleTable.titleCol eq it.role
                    }.firstOrNull() ?: Role.new {
                    this.title = it.role
                }
                Credit
                    .find {
                        (CreditTable.bookCol eq resource.id) and
                            (CreditTable.creatorCol eq creator.id) and
                            (CreditTable.roleCol eq role.id)
                    }.firstOrNull() ?: Credit.new {
                    this.book = resource
                    this.creator = creator
                    this.role = role
                }
            }

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun pull(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()

            val edition = resource.openLibraryId?.let {
                OpenLibrary.getEdition(id = it)
            } ?: resource.isbn?.let {
                OpenLibrary.getEditionByISBN(isbn = it)
            } ?: throw NotImplementedResponse(message = "Pull only available via OpenLibrary editionId or isbn currently.")
            val work = OpenLibrary.getWork(id = edition.works.first().getId())
            val editionId = edition.getId()
            val workId = work.getId()

            val exists = if (edition.isbn13.isEmpty() && edition.isbn10.isEmpty()) {
                Book
                    .find {
                        (
                            (BookTable.titleCol eq edition.title) and
                                (BookTable.subtitleCol eq edition.subtitle)
                        ) or (BookTable.openLibraryCol eq editionId)
                    }.firstOrNull()
            } else {
                Book
                    .find {
                        (
                            (BookTable.titleCol eq edition.title) and
                                (BookTable.subtitleCol eq edition.subtitle)
                        ) or (
                            BookTable.openLibraryCol eq editionId
                        ) or (
                            BookTable.isbnCol eq (edition.isbn13.firstOrNull() ?: edition.isbn10.first())
                        )
                    }.firstOrNull()
            }
            if (exists != null && exists != resource) {
                throw ConflictResponse("Book already exists")
            }

            // book.format = Format.PAPERBACK
//            resource.genres = SizedCollection(
//                edition.genres.map {
//                    Genre.find {
//                        GenreTable.titleCol eq it
//                    }.firstOrNull() ?: Genre.new {
//                        title = it
//                    }
//                },
//            )
            resource.goodreadsId = edition.identifiers.goodreads.firstOrNull()
            resource.googleBooksId = edition.identifiers.google.firstOrNull()
            resource.imageUrl = "https://covers.openlibrary.org/b/OLID/$editionId-L.jpg"
            resource.isbn = edition.isbn13.firstOrNull() ?: edition.isbn10.firstOrNull()
            resource.libraryThingId = edition.identifiers.librarything.firstOrNull()
            resource.openLibraryId = editionId
            resource.publishDate = edition.publishDate?.toJavaLocalDate()
            edition.publishers.firstOrNull()?.let {
                resource.publisher = Publisher
                    .find {
                        PublisherTable.titleCol eq it
                    }.firstOrNull() ?: Publisher.new {
                    title = it
                }
            }
            resource.subtitle = edition.subtitle
//            resource.summary = edition.description ?: work.description
            resource.title = edition.title
            resource.credits.forEach {
                it.delete()
            }
            work.authors
                .map {
                    OpenLibrary.getAuthor(id = it.author.getId())
                }.map {
                    val creator = Creator
                        .find {
                            CreatorTable.nameCol eq it.name
                        }.firstOrNull() ?: Creator.new {
                        name = it.name
                    }
//                it.photos.firstOrNull()?.let {
//                    creator.imageUrl = "https://covers.openlibrary.org/a/id/$it-L.jpg"
//                }
                    creator
                }.forEach {
                    val role = Role
                        .find {
                            RoleTable.titleCol eq "Author"
                        }.firstOrNull() ?: Role.new {
                        title = "Author"
                    }
                    Credit
                        .find {
                            (CreditTable.bookCol eq resource.id) and
                                (CreditTable.creatorCol eq it.id) and
                                (CreditTable.roleCol eq role.id)
                        }.firstOrNull() ?: Credit.new {
                        this.book = resource
                        this.creator = it
                        this.role = role
                    }
                }
            edition.contributors.forEach {
                val creator = Creator
                    .find {
                        CreatorTable.nameCol eq it.name
                    }.firstOrNull() ?: Creator.new {
                    name = it.name
                }
                val role = Role
                    .find {
                        RoleTable.titleCol eq it.role
                    }.firstOrNull() ?: Role.new {
                    title = it.role
                }
                Credit
                    .find {
                        (CreditTable.bookCol eq resource.id) and
                            (CreditTable.creatorCol eq creator.id) and
                            (CreditTable.roleCol eq role.id)
                    }.firstOrNull() ?: Credit.new {
                    this.book = resource
                    this.creator = creator
                    this.role = role
                }
            }

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun collectBook(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            resource.isCollected = true
            resource.wishers = SizedCollection()
            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun discardBook(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            resource.isCollected = false
            resource.readers.forEach {
                it.delete()
            }
            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun addReader(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            if (!resource.isCollected) {
                throw BadRequestResponse(message = "Book hasn't been collected to be able to read")
            }
            val body = ctx.bodyAsClass<BookInput.Reader>()
            val user = User.findById(body.userId) ?: throw NotFoundResponse("User not found.")
            val readBook = ReadBook
                .find {
                    (ReadBookTable.bookCol eq resource.id) and (ReadBookTable.userCol eq user.id)
                }.firstOrNull() ?: ReadBook.new {
                this.book = resource
                this.user = user
            }
            readBook.readDate = body.readDate

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun removeReader(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            if (!resource.isCollected) {
                throw BadRequestResponse(message = "Book hasn't been collected to be able to unread")
            }
            val body = ctx.bodyAsClass<IdInput>()
            val user = User.findById(body.id) ?: throw NotFoundResponse("User not found.")
            val readBook = ReadBook
                .find {
                    (ReadBookTable.bookCol eq resource.id) and (ReadBookTable.userCol eq user.id)
                }.firstOrNull() ?: throw BadRequestResponse(message = "Book has not been read by this User.")
            readBook.delete()

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun addWisher(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            if (resource.isCollected) {
                throw BadRequestResponse(message = "Book has already been collected")
            }
            val body = ctx.bodyAsClass<IdInput>()
            val user = User.findById(body.id) ?: throw NotFoundResponse("User not found.")
            val temp = resource.wishers.toMutableSet()
            temp.add(user)
            resource.wishers = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun removeWisher(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            if (resource.isCollected) {
                throw BadRequestResponse(message = "Book has already been collected")
            }
            val body = ctx.bodyAsClass<IdInput>()
            val user = User.findById(body.id) ?: throw NotFoundResponse("User not found.")
            if (!resource.wishers.contains(user)) {
                throw BadRequestResponse(message = "Book hasn't been wished by User")
            }
            val temp = resource.wishers.toMutableList()
            temp.remove(user)
            resource.wishers = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun addCredit(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<BookInput.Credit>()
            val creator = Creator.findById(body.creatorId) ?: throw NotFoundResponse("Creator not found.")
            val role = Role.findById(body.roleId) ?: throw NotFoundResponse("Role not found.")
            Credit
                .find {
                    (CreditTable.bookCol eq resource.id) and
                        (CreditTable.creatorCol eq creator.id) and
                        (CreditTable.roleCol eq role.id)
                }.firstOrNull() ?: Credit.new {
                this.book = resource
                this.creator = creator
                this.role = role
            }

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun removeCredit(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<BookInput.Credit>()
            val creator = Creator.findById(body.creatorId) ?: throw NotFoundResponse("Creator not found.")
            val role = Role.findById(body.roleId) ?: throw NotFoundResponse("Role not found.")
            val credit = Credit
                .find {
                    (CreditTable.bookCol eq resource.id) and
                        (CreditTable.creatorCol eq creator.id) and
                        (CreditTable.roleCol eq role.id)
                }.firstOrNull() ?: throw NotFoundResponse(message = "Unable to find Book Creator Role")
            credit.delete()

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun addGenre(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<IdInput>()
            val genre = Genre.findById(body.id) ?: throw NotFoundResponse("Genre not found.")
            val temp = resource.genres.toMutableSet()
            temp.add(genre)
            resource.genres = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun removeGenre(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<IdInput>()
            val genre = Genre.findById(body.id) ?: throw NotFoundResponse("Genre not found.")
            if (!resource.genres.contains(genre)) {
                throw BadRequestResponse(message = "Genre is already linked to Book")
            }
            val temp = resource.genres.toMutableList()
            temp.remove(genre)
            resource.genres = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun addSeries(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<BookInput.Series>()
            val series = Series.findById(body.seriesId) ?: throw NotFoundResponse("Series not found.")
            val bookSeries = BookSeries
                .find {
                    (BookSeriesTable.bookCol eq resource.id) and (BookSeriesTable.seriesCol eq series.id)
                }.firstOrNull() ?: BookSeries.new {
                this.book = resource
                this.series = series
            }
            bookSeries.number = if (body.number == 0) null else body.number

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun removeSeries(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<IdInput>()
            val series = Series.findById(body.id) ?: throw NotFoundResponse("Series not found.")
            val bookSeries = BookSeries
                .find {
                    (BookSeriesTable.bookCol eq resource.id) and (BookSeriesTable.seriesCol eq series.id)
                }.firstOrNull() ?: throw NotFoundResponse(message = "Book isn't linked to Series")
            bookSeries.delete()

            ctx.json(resource.toJson(showAll = true))
        }
    }

    fun search(ctx: Context) {
        Utils.query {
            val body = ctx.bodyAsClass<BookInput>()
            val results = OpenLibrary.search(title = body.title)
            ctx.json(results)
        }
    }
}
