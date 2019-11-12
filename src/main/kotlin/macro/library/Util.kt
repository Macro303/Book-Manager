package macro.library

import com.google.gson.GsonBuilder
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.JsonNode
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.exceptions.UnirestException
import io.ktor.http.ContentType
import io.ktor.http.withCharset
import macro.library.config.Config.Companion.CONFIG
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import java.nio.charset.Charset

/**
 * Created by Macro303 on 2019-Oct-30
 */
object Util {
	private val LOGGER = LogManager.getLogger(Util::class.java)
	private val HEADERS = mapOf(
		"Accept" to ContentType.Application.Json.withCharset(Charset.forName("UTF-8")).toString(),
		"User-Agent" to "Book-Manager"
	)
	val SQLITE_DATABASE = "Book-Manager.sqlite"
	internal val DATABASE_URL = "jdbc:sqlite:$SQLITE_DATABASE"
	internal val GSON = GsonBuilder()
		.setPrettyPrinting()
		.serializeNulls()
		.disableHtmlEscaping()
		.create()

	init {
		Unirest.setProxy(CONFIG.proxy.getHttpHost())
	}

	@JvmOverloads
	fun httpRequest(url: String, headers: Map<String, String> = HEADERS): JsonNode? {
		val request = Unirest.get(url)
		request.headers(headers)
		LOGGER.debug("GET : >>> - ${request.url} - $headers")
		val response: HttpResponse<JsonNode>
		try {
			response = request.asJson()
		} catch (ue: UnirestException) {
			LOGGER.error("Unable to load URL: $ue")
			return null
		}

		var level = Level.ERROR
		when {
			response.status < 100 -> level = Level.ERROR
			response.status < 200 -> level = Level.INFO
			response.status < 300 -> level = Level.INFO
			response.status < 400 -> level = Level.WARN
			response.status < 500 -> level = Level.WARN
		}
		LOGGER.log(level, "GET: ${response.status} ${response.statusText} - ${request.url}")
		LOGGER.debug("Response: ${response.body}")
		return if (response.status != 200) null else response.body
	}
}