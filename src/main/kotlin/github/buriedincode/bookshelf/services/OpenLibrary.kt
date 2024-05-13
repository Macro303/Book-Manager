package github.buriedincode.bookshelf.services

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.services.openlibrary.Author
import github.buriedincode.bookshelf.services.openlibrary.Edition
import github.buriedincode.bookshelf.services.openlibrary.SearchResponse
import github.buriedincode.bookshelf.services.openlibrary.Work
import kotlinx.serialization.SerializationException
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.Logging
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.stream.Collectors

object OpenLibrary : Logging {
    private const val BASE_URL = "https://openlibrary.org"
    private val USER_AGENT = "Bookshelf/${Utils.VERSION} (${System.getProperty(
        "os.name",
    )}/${System.getProperty("os.version")}; Kotlin/${KotlinVersion.CURRENT})"
    private val CLIENT: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    private fun encodeURI(
        endpoint: String,
        params: Map<String, String> = HashMap(),
    ): URI {
        var encodedUrl = "$BASE_URL$endpoint"
        if (params.isNotEmpty()) {
            encodedUrl = params.keys
                .stream()
                .sorted()
                .map {
                    "$it=${URLEncoder.encode(params[it], StandardCharsets.UTF_8)}"
                }
                .collect(Collectors.joining("&", "$encodedUrl?", ""))
        }
        return URI.create(encodedUrl)
    }

    @Throws(ServiceException::class)
    private fun performGetRequest(uri: URI): String {
        try {
            val request = HttpRequest.newBuilder()
                .uri(uri)
                .setHeader("Accept", "application/json")
                .setHeader("User-Agent", USER_AGENT)
                .GET()
                .build()
            val response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString())
            val level = when (response.statusCode()) {
                in 100 until 200 -> Level.WARN
                in 200 until 300 -> Level.DEBUG
                in 300 until 400 -> Level.INFO
                in 400 until 500 -> Level.WARN
                else -> Level.ERROR
            }
            logger.log(level, "GET: ${response.statusCode()} - $uri")
            if (response.statusCode() == 200) {
                return response.body()
            }
            logger.error(response.body())
            throw ServiceException("Exception")
        } catch (ioe: IOException) {
            throw ServiceException(cause = ioe)
        } catch (hcte: HttpConnectTimeoutException) {
            throw ServiceException(cause = hcte)
        } catch (ie: InterruptedException) {
            throw ServiceException(cause = ie)
        } catch (se: SerializationException) {
            throw ServiceException(cause = se)
        }
    }

    @Throws(ServiceException::class)
    fun getRequest(uri: URI): String {
        /* this.cache?.let {
            it.select(url = uri.toString())?.let {
                logger.debug("Using cached response for $uri")
                return it
            }
        } */
        val response = performGetRequest(uri = uri)
        // this.cache?.insert(url = uri.toString(), response = response)
        return response
    }

    @Throws(ServiceException::class)
    fun search(params: Map<String, String>): List<SearchResponse.Work> {
        val uri = encodeURI(endpoint = "/search.json", params = params)
        try {
            val response = Utils.JSON.decodeFromString<SearchResponse<SearchResponse.Work>>(getRequest(uri = uri))
            val results = response.docs
            return results
        } catch (se: SerializationException) {
            throw ServiceException(cause = se)
        }
    }

    @Throws(ServiceException::class)
    fun getAuthor(id: String): Author {
        val uri = encodeURI(endpoint = "/author/$id.json")
        try {
            return Utils.JSON.decodeFromString<Author>(getRequest(uri = uri))
        } catch (se: SerializationException) {
            throw ServiceException(cause = se)
        }
    }

    @Throws(ServiceException::class)
    fun getEdition(id: String): Edition {
        val uri = encodeURI(endpoint = "/edition/$id.json")
        try {
            return Utils.JSON.decodeFromString<Edition>(getRequest(uri = uri))
        } catch (se: SerializationException) {
            throw ServiceException(cause = se)
        }
    }

    @Throws(ServiceException::class)
    fun getEditionByISBN(isbn: String): Edition {
        val uri = encodeURI(endpoint = "/isbn/$isbn.json")
        try {
            return Utils.JSON.decodeFromString<Edition>(getRequest(uri = uri))
        } catch (se: SerializationException) {
            throw ServiceException(cause = se)
        }
    }

    @Throws(ServiceException::class)
    fun getWork(id: String): Work {
        val uri = encodeURI(endpoint = "/work/$id.json")
        try {
            return Utils.JSON.decodeFromString<Work>(getRequest(uri = uri))
        } catch (se: SerializationException) {
            throw ServiceException(cause = se)
        }
    }
}
