package github.buriedincode.bookshelf.models

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.kotlin.Logging
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateDeserializer : JsonDeserializer<LocalDate?>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext?): LocalDate? {
        val mapper = parser.codec as ObjectMapper
        val root = mapper.readTree(parser) as JsonNode
        if (root.isNull || root.asText() == "null") {
            return null
        }
        return LocalDate.parse(root.asText(), DateTimeFormatter.ISO_DATE)
    }

    companion object : Logging
}
