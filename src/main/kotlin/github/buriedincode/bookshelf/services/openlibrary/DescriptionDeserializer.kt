package github.buriedincode.bookshelf.services.openlibrary

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.kotlin.Logging
import java.io.IOException

class DescriptionDeserializer : JsonDeserializer<String?>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext?): String? {
        val mapper = parser.codec as ObjectMapper
        val root = mapper.readTree(parser) as JsonNode
        if (root.isObject) {
            return root.get("value").asText()
        }
        return root.asText()
    }

    companion object : Logging
}
