package github.buriedincode.bookshelf.services

import com.fasterxml.jackson.databind.ObjectMapper
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.VERSION
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Format
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.services.models.Edition
import github.buriedincode.bookshelf.services.models.Work
import github.buriedincode.bookshelf.tables.GenreTable
import github.buriedincode.bookshelf.tables.PublisherTable
import io.javalin.http.InternalServerErrorResponse
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.stream.Collectors


object OpenLibrary: Logging {
    private val CLIENT: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private val MAPPER: ObjectMapper = ObjectMapper()
    private const val BASE_URL = "https://openlibrary.org"

    private fun encodeURI(endpoint: String, params: MutableMap<String, String> = HashMap()): URI {
        val encodedUrl = if (params.isEmpty()) {
            BASE_URL + endpoint
        } else {
            params.keys
                .stream()
                .sorted()
                .map { key: String -> key + "=" + URLEncoder.encode(params[key], StandardCharsets.UTF_8) }
                .collect(Collectors.joining("&", "$BASE_URL$endpoint?", ""))
        }
        return URI.create(encodedUrl)
    }

    private fun <T> sendRequest(uri: URI, clazz: Class<T>): T? {
        try {
            val request = HttpRequest.newBuilder()
                .uri(uri)
                .setHeader("Accept", "application/json")
                .setHeader("User-Agent", "Bookshelf-v$VERSION/Java-v${System.getProperty("java.version")}")
                .GET()
                .build()
            val response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                return MAPPER.readValue(response.body(), clazz)
            }
            logger.error(response.body())
        } catch (exc: IOException) {
            logger.error("Unable to make request to: ${uri.path}", exc)
        } catch (exc: InterruptedException) {
            logger.error("Unable to make request to: ${uri.path}", exc)
        }
        return null
    }
    
    fun lookupBook(isbn: String): Pair<Edition, Work> {
        val edition = sendRequest(uri = encodeURI(endpoint = "/isbn/$isbn.json"), clazz = Edition::class.java)
            ?: throw InternalServerErrorResponse(message = "Unable to find edition with isbn: $isbn")
        val workId = edition.works.first().key.split("/").last()
        val work = sendRequest(uri = encodeURI(endpoint = "/work/$workId.json"), clazz = Work::class.java)
            ?: throw InternalServerErrorResponse(message = "Unable to find work with id: $workId")
        return edition to work
    }
    
    fun getBook(editionId: String): Book {
        val edition = sendRequest(uri = encodeURI(endpoint = "/edition/$editionId.json"), clazz = Edition::class.java)
            ?: throw InternalServerErrorResponse(message = "Unable to find edition with id: $editionId")
        val workId = edition.works.first().key.split("/").last()
        val work = sendRequest(uri = encodeURI(endpoint = "/work/$workId.json"), clazz = Work::class.java)
            ?: throw InternalServerErrorResponse(message = "Unable to find work with id: $workId")
        val book = Book.new {
            description = edition.description ?: work.description
            format = Format.PAPERBACK // TODO
            genres = SizedCollection(edition.genres.map {
                Genre.find {
                    GenreTable.titleCol eq it
                }.firstOrNull() ?: Genre.new {
                    title = it
                }
            })
            goodreadsId = edition.identifiers.goodreads.firstOrNull()
            googleBooksId = edition.identifiers.google.firstOrNull()
            imageUrl = "https://covers.openlibrary.org/b/OLID/${edition.editionId}-L.jpg"
            this.isbn = isbn
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
        }
        return book
    }
}