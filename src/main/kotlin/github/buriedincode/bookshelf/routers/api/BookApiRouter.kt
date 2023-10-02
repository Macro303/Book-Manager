package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.asEnumOrNull
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.BookCreditInput
import github.buriedincode.bookshelf.models.BookImport
import github.buriedincode.bookshelf.models.BookInput
import github.buriedincode.bookshelf.models.BookReadInput
import github.buriedincode.bookshelf.models.BookSeries
import github.buriedincode.bookshelf.models.BookSeriesInput
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Credit
import github.buriedincode.bookshelf.models.Format
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.IdValue
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.ReadBook
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.models.Series
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.services.OpenLibrary
import github.buriedincode.bookshelf.services.openlibrary.Edition
import github.buriedincode.bookshelf.services.openlibrary.Work
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.BookTable
import github.buriedincode.bookshelf.tables.CreatorTable
import github.buriedincode.bookshelf.tables.CreditTable
import github.buriedincode.bookshelf.tables.GenreTable
import github.buriedincode.bookshelf.tables.PublisherTable
import github.buriedincode.bookshelf.tables.ReadBookTable
import github.buriedincode.bookshelf.tables.RoleTable
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.NotImplementedResponse
import io.javalin.http.bodyValidator
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or

object BookApiRouter : Logging {
    fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            var books = Book.all().toList()
            val creator = ctx.queryParam("creator-id")?.toLongOrNull()?.let {
                Creator.findById(id = it)
            }
            if (creator != null) {
                books = books.filter {
                    creator in it.credits.map { it.creator }
                }
            }
            val format: Format? = ctx.queryParam("format")?.asEnumOrNull<Format>()
            if (format != null) {
                books = books.filter {
                    it.format == format
                }
            }
            val genre = ctx.queryParam("genre-id")?.toLongOrNull()?.let {
                Genre.findById(id = it)
            }
            if (genre != null) {
                books = books.filter {
                    genre in it.genres
                }
            }
            val publisher = ctx.queryParam("publisher-id")?.toLongOrNull()?.let {
                Publisher.findById(id = it)
            }
            if (publisher != null) {
                books = books.filter {
                    it.publisher == publisher
                }
            }
            val series = ctx.queryParam("series-id")?.toLongOrNull()?.let {
                Series.findById(id = it)
            }
            if (series != null) {
                books = books.filter {
                    series in it.series.map { it.series }
                }
            }
            val title = ctx.queryParam("title")
            if (title != null) {
                books = books.filter {
                    it.title.contains(title, ignoreCase = true) ||
                        title.contains(it.title, ignoreCase = true) ||
                        (
                            it.subtitle?.let {
                                it.contains(title, ignoreCase = true) || title.contains(it, ignoreCase = true)
                            } ?: false
                        )
                }
            }
            ctx.json(books.sorted().map { it.toJson() })
        }

    private fun Context.getInput(): BookInput =
        this.bodyValidator<BookInput>()
            .check({ it.title.isNotBlank() }, error = "Title must not be empty")
            .check({ it.series.all { it.seriesId > 0 } }, error = "bookId must be greater than 0")
            .get()

    fun createEndpoint(ctx: Context): Unit =
        Utils.query {
            val input = ctx.getInput()
            val exists = Book.find {
                (BookTable.titleCol eq input.title) and (BookTable.subtitleCol eq input.subtitle)
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse(message = "Book already exists")
            }
            val book = Book.new {
                description = input.description
                format = input.format
                genres = SizedCollection(
                    input.genreIds.map {
                        Genre.findById(id = it)
                            ?: throw NotFoundResponse(message = "Unable to find Genre: `$it`")
                    },
                )
                goodreadsId = input.goodreadsId
                googleBooksId = input.googleBooksId
                imageUrl = input.imageUrl
                isCollected = input.isCollected
                isbn = input.isbn
                libraryThingId = input.libraryThingId
                openLibraryId = input.openLibraryId
                publishDate = input.publishDate
                publisher = input.publisherId?.let {
                    Publisher.findById(id = it)
                        ?: throw NotFoundResponse(message = "Unable to find Publisher: `$it`")
                }
                subtitle = input.subtitle
                title = input.title
                wishers = SizedCollection(
                    input.wisherIds.map {
                        User.findById(id = it)
                            ?: throw NotFoundResponse(message = "Unable to find User: `$it`")
                    },
                )
            }
            input.credits.forEach {
                val creator = Creator.findById(id = it.creatorId)
                    ?: throw NotFoundResponse(message = "Unable to find Creator: `${it.creatorId}`")
                val role = Role.findById(id = it.roleId)
                    ?: throw NotFoundResponse(message = "Unable to find Role: `${it.roleId}`")
                Credit.find {
                    (CreditTable.bookCol eq book.id) and
                        (CreditTable.creatorCol eq creator.id) and
                        (CreditTable.roleCol eq role.id)
                }.firstOrNull() ?: Credit.new {
                    this.book = book
                    this.creator = creator
                    this.role = role
                }
            }
            input.readers.forEach {
                val user = User.findById(id = it.userId)
                    ?: throw NotFoundResponse(message = "Unable to find User: `${it.userId}`")
                val readBook = ReadBook.find {
                    (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq user.id)
                }.firstOrNull() ?: ReadBook.new {
                    this.book = book
                    this.user = user
                }
                readBook.readDate = it.readDate
            }
            input.series.forEach {
                val series = Series.findById(id = it.seriesId)
                    ?: throw NotFoundResponse(message = "Unable to find Series: `${it.seriesId}`")
                val bookSeries = BookSeries.find {
                    (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq series.id)
                }.firstOrNull() ?: BookSeries.new {
                    this.book = book
                    this.series = series
                }
                bookSeries.number = if (it.number == 0) null else it.number
            }

            ctx.status(HttpStatus.CREATED).json(book.toJson(showAll = true))
        }

    private fun Context.getResource(): Book {
        return this.pathParam("book-id").toLongOrNull()?.let {
            Book.findById(id = it) ?: throw NotFoundResponse(message = "Unable to find Book: `$it`")
        } ?: throw BadRequestResponse(message = "Unable to find Book: `${this.pathParam("book-id")}`")
    }

    fun getEndpoint(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            ctx.json(resource.toJson(showAll = true))
        }

    fun updateEndpoint(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getInput()
            val exists = Book.find {
                (BookTable.titleCol eq input.title) and (BookTable.subtitleCol eq input.subtitle)
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse(message = "Book already exists")
            }
            resource.description = input.description
            resource.format = input.format
            resource.genres = SizedCollection(
                input.genreIds.map {
                    Genre.findById(id = it)
                        ?: throw NotFoundResponse(message = "Unable to find Genre: `$it`")
                },
            )
            resource.goodreadsId = input.goodreadsId
            resource.googleBooksId = input.googleBooksId
            resource.imageUrl = input.imageUrl
            resource.isCollected = input.isCollected
            resource.isbn = input.isbn
            resource.libraryThingId = input.libraryThingId
            resource.openLibraryId = input.openLibraryId
            resource.publishDate = input.publishDate
            resource.publisher = input.publisherId?.let {
                Publisher.findById(id = it)
                    ?: throw NotFoundResponse(message = "Unable to find Publisher: `$it`")
            }
            resource.subtitle = input.subtitle
            resource.title = input.title
            resource.wishers = SizedCollection(
                input.wisherIds.map {
                    User.findById(id = it)
                        ?: throw NotFoundResponse(message = "Unable to find User: `$it`")
                },
            )
            input.credits.forEach {
                val creator = Creator.findById(id = it.creatorId)
                    ?: throw NotFoundResponse(message = "Unable to find Creator: `${it.creatorId}`")
                val role = Role.findById(id = it.roleId)
                    ?: throw NotFoundResponse(message = "Unable to find Role: `${it.roleId}`")
                Credit.find {
                    (CreditTable.bookCol eq resource.id) and
                        (CreditTable.creatorCol eq creator.id) and
                        (CreditTable.roleCol eq role.id)
                }.firstOrNull() ?: Credit.new {
                    this.book = resource
                    this.creator = creator
                    this.role = role
                }
            }
            input.readers.forEach {
                val user = User.findById(id = it.userId)
                    ?: throw NotFoundResponse(message = "Unable to find User: `${it.userId}`")
                val readBook = ReadBook.find {
                    (ReadBookTable.bookCol eq resource.id) and (ReadBookTable.userCol eq user.id)
                }.firstOrNull() ?: ReadBook.new {
                    this.book = resource
                    this.user = user
                }
                readBook.readDate = it.readDate
            }
            input.series.forEach {
                val series = Series.findById(id = it.seriesId)
                    ?: throw NotFoundResponse(message = "Unable to find Series: `${it.seriesId}`")
                val bookSeries = BookSeries.find {
                    (BookSeriesTable.bookCol eq resource.id) and (BookSeriesTable.seriesCol eq series.id)
                }.firstOrNull() ?: BookSeries.new {
                    this.book = resource
                    this.series = series
                }
                bookSeries.number = if (it.number == 0) null else it.number
            }

            ctx.json(resource.toJson(showAll = true))
        }

    fun deleteEndpoint(ctx: Context): Unit =
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

    private fun Context.getImport(): BookImport =
        this.bodyValidator<BookImport>()
            .check(
                {
                    !it.goodreadsId.isNullOrBlank() ||
                        !it.googleBooksId.isNullOrBlank() ||
                        !it.isbn.isNullOrBlank() ||
                        !it.libraryThingId.isNullOrBlank() ||
                        !it.openLibraryId.isNullOrBlank()
                },
                error = "At least 1 id to import must be specified",
            )
            .get()

    fun importBook(ctx: Context): Unit =
        Utils.query {
            val import = ctx.getImport()

            val edition: Edition
            val work: Work
            if (import.openLibraryId != null) {
                val temp = OpenLibrary.getBook(editionId = import.openLibraryId)
                edition = temp.first
                work = temp.second
            } else if (import.isbn != null) {
                val temp = OpenLibrary.lookupBook(isbn = import.isbn)
                edition = temp.first
                work = temp.second
            } else {
                throw NotImplementedResponse(message = "Only import via OpenLibrary edition id or Isbn currently supported.")
            }
            val exists = if (edition.isbn == null) {
                Book.find {
                    (
                        (BookTable.titleCol eq edition.title) and
                            (BookTable.subtitleCol eq edition.subtitle)
                    ) or (BookTable.openLibraryCol eq edition.editionId)
                }.firstOrNull()
            } else {
                Book.find {
                    (
                        (BookTable.titleCol eq edition.title) and
                            (BookTable.subtitleCol eq edition.subtitle)
                    ) or (BookTable.openLibraryCol eq edition.editionId) or (BookTable.isbnCol eq edition.isbn)
                }.firstOrNull()
            }
            if (exists != null) {
                throw ConflictResponse(message = "This Book already exists in the database.")
            }

            val book = Book.new {
                description = edition.description ?: work.description
                format = Format.PAPERBACK // TODO
                genres = SizedCollection(
                    edition.genres.map {
                        Genre.find {
                            GenreTable.titleCol eq it
                        }.firstOrNull() ?: Genre.new {
                            title = it
                        }
                    },
                )
                goodreadsId = edition.identifiers.goodreads.firstOrNull()
                googleBooksId = edition.identifiers.google.firstOrNull()
                imageUrl = "https://covers.openlibrary.org/b/OLID/${edition.editionId}-L.jpg"
                isbn = edition.isbn
                isCollected = import.isCollected
                libraryThingId = edition.identifiers.librarything.firstOrNull()
                openLibraryId = edition.editionId
                publishDate = edition.publishDate
                edition.publishers.firstOrNull()?.let {
                    publisher = Publisher.find {
                        PublisherTable.titleCol eq it
                    }.firstOrNull() ?: Publisher.new {
                        title = it
                    }
                }
                subtitle = edition.subtitle
                title = edition.title
                wishers = SizedCollection(
                    import.wisherIds.map {
                        User.findById(id = it)
                            ?: throw NotFoundResponse(message = "Unable to find User: `$it`")
                    },
                )
            }
            work.authors.map {
                OpenLibrary.getAuthor(authorId = it.authorId)
            }.map {
                val creator = Creator.find {
                    CreatorTable.nameCol eq it.name
                }.firstOrNull() ?: Creator.new {
                    name = it.name
                }
                it.photos.firstOrNull()?.let {
                    creator.imageUrl = "https://covers.openlibrary.org/a/id/$it-L.jpg"
                }
                creator
            }.forEach {
                Credit.new {
                    this.book = book
                    creator = it
                    role = Role.find {
                        RoleTable.titleCol eq "Author"
                    }.firstOrNull() ?: Role.new {
                        title = "Author"
                    }
                }
            }
            edition.contributors.forEach {
                Credit.new {
                    this.book = book
                    creator = Creator.find {
                        CreatorTable.nameCol eq it.name
                    }.firstOrNull() ?: Creator.new {
                        name = it.name
                    }
                    role = Role.find {
                        RoleTable.titleCol eq it.role
                    }.firstOrNull() ?: Role.new {
                        title = it.role
                    }
                }
            }
            ctx.status(HttpStatus.CREATED).json(book.toJson(showAll = true))
        }

    fun refreshBook(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()

            val edition: Edition
            val work: Work
            if (resource.openLibraryId != null) {
                val temp = OpenLibrary.getBook(editionId = resource.openLibraryId!!)
                edition = temp.first
                work = temp.second
            } else if (resource.isbn != null) {
                val temp = OpenLibrary.lookupBook(isbn = resource.isbn!!)
                edition = temp.first
                work = temp.second
            } else {
                throw NotImplementedResponse(message = "Only refresh via OpenLibrary edition id or Isbn currently supported.")
            }
            val exists = if (edition.isbn == null) {
                Book.find {
                    (
                        (BookTable.titleCol eq edition.title) and
                            (BookTable.subtitleCol eq edition.subtitle)
                    ) or (BookTable.openLibraryCol eq edition.editionId)
                }.firstOrNull()
            } else {
                Book.find {
                    (
                        (BookTable.titleCol eq edition.title) and
                            (BookTable.subtitleCol eq edition.subtitle)
                    ) or (BookTable.openLibraryCol eq edition.editionId) or (BookTable.isbnCol eq edition.isbn)
                }.firstOrNull()
            }
            if (exists != null && exists != resource) {
                throw ConflictResponse(message = "This Book already exists in the database.")
            }

            resource.description = edition.description ?: work.description
            // book.format = Format.PAPERBACK
            resource.genres = SizedCollection(
                edition.genres.map {
                    Genre.find {
                        GenreTable.titleCol eq it
                    }.firstOrNull() ?: Genre.new {
                        title = it
                    }
                },
            )
            resource.goodreadsId = edition.identifiers.goodreads.firstOrNull()
            resource.googleBooksId = edition.identifiers.google.firstOrNull()
            resource.imageUrl = "https://covers.openlibrary.org/b/OLID/${edition.editionId}-L.jpg"
            resource.isbn = edition.isbn
            resource.libraryThingId = edition.identifiers.librarything.firstOrNull()
            resource.openLibraryId = edition.editionId
            resource.publishDate = edition.publishDate
            edition.publishers.firstOrNull()?.let {
                resource.publisher = Publisher.find {
                    PublisherTable.titleCol eq it
                }.firstOrNull() ?: Publisher.new {
                    title = it
                }
            }
            resource.subtitle = edition.subtitle
            resource.title = edition.title
            work.authors.map {
                OpenLibrary.getAuthor(authorId = it.authorId)
            }.map {
                val creator = Creator.find {
                    CreatorTable.nameCol eq it.name
                }.firstOrNull() ?: Creator.new {
                    name = it.name
                }
                it.photos.firstOrNull()?.let {
                    creator.imageUrl = "https://covers.openlibrary.org/a/id/$it-L.jpg"
                }
                creator
            }.forEach {
                val role = Role.find {
                    RoleTable.titleCol eq "Author"
                }.firstOrNull() ?: Role.new {
                    title = "Author"
                }
                Credit.find {
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
                val creator = Creator.find {
                    CreatorTable.nameCol eq it.name
                }.firstOrNull() ?: Creator.new {
                    name = it.name
                }
                val role = Role.find {
                    RoleTable.titleCol eq it.role
                }.firstOrNull() ?: Role.new {
                    title = it.role
                }
                Credit.find {
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

    fun collectBook(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            resource.isCollected = true
            resource.wishers = SizedCollection()
            ctx.json(resource.toJson(showAll = true))
        }

    fun discardBook(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            resource.isCollected = false
            resource.readers.forEach {
                it.delete()
            }
            ctx.json(resource.toJson(showAll = true))
        }

    private fun Context.getReadInput(): BookReadInput =
        this.bodyValidator<BookReadInput>()
            .check({ it.userId > 0 }, error = "UserId must be greater than 0")
            .get()

    fun addReader(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            if (!resource.isCollected) {
                throw BadRequestResponse(message = "Book hasn't been collected to be able to read")
            }
            val input = ctx.getReadInput()
            val user = User.findById(id = input.userId)
                ?: throw NotFoundResponse(message = "Unable to find User: `${input.userId}`")
            val readBook = ReadBook.find {
                (ReadBookTable.bookCol eq resource.id) and (ReadBookTable.userCol eq user.id)
            }.firstOrNull() ?: ReadBook.new {
                this.book = resource
                this.user = user
            }
            readBook.readDate = input.readDate

            ctx.json(resource.toJson(showAll = true))
        }

    private fun Context.getIdValue(): IdValue =
        this.bodyValidator<IdValue>()
            .check({ it.id > 0 }, error = "Id must be greater than 0")
            .get()

    fun removeReader(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            if (!resource.isCollected) {
                throw BadRequestResponse(message = "Book hasn't been collected to be able to unread")
            }
            val input = ctx.getIdValue()
            val user = User.findById(id = input.id)
                ?: throw NotFoundResponse(message = "Unable to find User: `${input.id}`")
            val readBook = ReadBook.find {
                (ReadBookTable.bookCol eq resource.id) and (ReadBookTable.userCol eq user.id)
            }.firstOrNull() ?: throw BadRequestResponse(message = "Book has not been read by this User.")
            readBook.delete()

            ctx.json(resource.toJson(showAll = true))
        }

    fun addWisher(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            if (resource.isCollected) {
                throw BadRequestResponse(message = "Book has already been collected")
            }
            val input = ctx.getIdValue()
            val user = User.findById(id = input.id)
                ?: throw NotFoundResponse(message = "Unable to find User: `${input.id}`")
            val temp = resource.wishers.toMutableSet()
            temp.add(user)
            resource.wishers = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }

    fun removeWisher(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            if (resource.isCollected) {
                throw BadRequestResponse(message = "Book has already been collected")
            }
            val input = ctx.getIdValue()
            val user = User.findById(id = input.id)
                ?: throw NotFoundResponse(message = "Unable to find User: `${input.id}`")
            if (!resource.wishers.contains(user)) {
                throw BadRequestResponse(message = "Book hasn't been wished by User")
            }
            val temp = resource.wishers.toMutableList()
            temp.remove(user)
            resource.wishers = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }

    private fun Context.getCreditInput(): BookCreditInput =
        this.bodyValidator<BookCreditInput>()
            .get()

    fun addCredit(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getCreditInput()
            val creator = Creator.findById(id = input.creatorId)
                ?: throw NotFoundResponse(message = "Unable to find Creator: `${input.creatorId}`")
            val role = Role.findById(id = input.roleId)
                ?: throw NotFoundResponse(message = "Unable to find Role: `${input.creatorId}`")
            Credit.find {
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

    fun removeCredit(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getCreditInput()
            val creator = Creator.findById(id = input.creatorId)
                ?: throw NotFoundResponse(message = "Unable to find Creator: `${input.creatorId}`")
            val role = Role.findById(id = input.roleId)
                ?: throw NotFoundResponse(message = "Unable to find Role: `${input.creatorId}`")
            val credit = Credit.find {
                (CreditTable.bookCol eq resource.id) and
                    (CreditTable.creatorCol eq creator.id) and
                    (CreditTable.roleCol eq role.id)
            }.firstOrNull() ?: throw NotFoundResponse(message = "Unable to find Book Creator Role")
            credit.delete()

            ctx.json(resource.toJson(showAll = true))
        }

    fun addGenre(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getIdValue()
            val genre = Genre.findById(id = input.id)
                ?: throw NotFoundResponse(message = "Unable to find Genre: `${input.id}`")
            val temp = resource.genres.toMutableSet()
            temp.add(genre)
            resource.genres = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }

    fun removeGenre(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getIdValue()
            val genre = Genre.findById(id = input.id)
                ?: throw NotFoundResponse(message = "Unable to find Genre: `${input.id}`")
            if (!resource.genres.contains(genre)) {
                throw BadRequestResponse(message = "Genre is already linked to Book")
            }
            val temp = resource.genres.toMutableList()
            temp.remove(genre)
            resource.genres = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }

    private fun Context.getSeriesInput(): BookSeriesInput =
        this.bodyValidator<BookSeriesInput>()
            .check({ it.seriesId > 0 }, error = "seriesId must be greater than 0")
            .get()

    fun addSeries(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getSeriesInput()
            val series = Series.findById(id = input.seriesId)
                ?: throw NotFoundResponse(message = "Unable to find Series: `${input.seriesId}`")
            val bookSeries = BookSeries.find {
                (BookSeriesTable.bookCol eq resource.id) and (BookSeriesTable.seriesCol eq series.id)
            }.firstOrNull() ?: BookSeries.new {
                this.book = resource
                this.series = series
            }
            bookSeries.number = if (input.number == 0) null else input.number

            ctx.json(resource.toJson(showAll = true))
        }

    fun removeSeries(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getIdValue()
            val series = Series.findById(id = input.id)
                ?: throw NotFoundResponse(message = "Unable to find Series: `${input.id}`")
            val bookSeries = BookSeries.find {
                (BookSeriesTable.bookCol eq resource.id) and (BookSeriesTable.seriesCol eq series.id)
            }.firstOrNull() ?: throw NotFoundResponse(message = "Book isn't linked to Series")
            bookSeries.delete()

            ctx.json(resource.toJson(showAll = true))
        }
}
