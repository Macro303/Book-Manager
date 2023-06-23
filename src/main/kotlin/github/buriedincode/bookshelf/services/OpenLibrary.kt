package github.buriedincode.bookshelf.services

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import github.buriedincode.bookshelf.Utils.VERSION
import github.buriedincode.bookshelf.services.openlibrary.Author
import github.buriedincode.bookshelf.services.openlibrary.Edition
import github.buriedincode.bookshelf.services.openlibrary.Work
import io.javalin.http.InternalServerErrorResponse
import org.apache.logging.log4j.kotlin.Logging
import org.apache.logging.log4j.Level
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.stream.Collectors


object OpenLibrary : Logging {
    private const val BASE_URL = "https://openlibrary.org"
    private val CLIENT: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private val MAPPER: ObjectMapper = JsonMapper.builder()
        .addModule(JavaTimeModule())
        .addModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, true)
                .configure(KotlinFeature.NullToEmptyMap, true)
                .configure(KotlinFeature.NullIsSameAsDefault, true)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, true)
                .build()
        )
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()

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
            var level = Level.ERROR
            when {
                response.statusCode() < 100 -> level = Level.WARN
                response.statusCode() < 200 -> level = Level.INFO
                response.statusCode() < 300 -> level = Level.INFO
                response.statusCode() < 400 -> level = Level.WARN
                response.statusCode() < 500 -> level = Level.ERROR
            }
            logger.log(level, "GET: ${response.statusCode()} - ${uri}")
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

    fun getBook(editionId: String): Pair<Edition, Work> {
        val edition = sendRequest(uri = encodeURI(endpoint = "/edition/$editionId.json"), clazz = Edition::class.java)
            ?: throw InternalServerErrorResponse(message = "Unable to find edition with id: $editionId")
        val workId = edition.works.first().key.split("/").last()
        val work = sendRequest(uri = encodeURI(endpoint = "/work/$workId.json"), clazz = Work::class.java)
            ?: throw InternalServerErrorResponse(message = "Unable to find work with id: $workId")
        return edition to work
    }

    fun getAuthor(authorId: String): String {
        val author = sendRequest(uri = encodeURI(endpoint = "/author/$authorId.json"), clazz = Author::class.java)
            ?: throw InternalServerErrorResponse(message = "Unable to find author with id: $authorId")
        return author.name
    }
}