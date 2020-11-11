package github.macro

import io.ktor.http.HttpStatusCode

/**
 * Created by Macro303 on 2019-Nov-11
 */
data class ErrorMessage(
	val request: String,
	val message: String,
	val code: HttpStatusCode,
	val cause: Throwable? = null
) : ISendable {
	override fun toJson(full: Boolean): Map<String, Any?> {
		val output = mutableMapOf(
			"request" to request,
			"message" to message,
			"code" to code
		)
		if (cause != null)
			output["cause"] = cause
		return output.toSortedMap()
	}
}