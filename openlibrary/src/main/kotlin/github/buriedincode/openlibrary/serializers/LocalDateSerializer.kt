package github.buriedincode.openlibrary.serializers

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField

@OptIn(ExperimentalSerializationApi::class)
object LocalDateSerializer : KSerializer<LocalDate?> {
    private val formatter = DateTimeFormatterBuilder()
        .appendOptional(DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd").toFormatter())
        .appendOptional(DateTimeFormatterBuilder().appendPattern("MMMM d, yyyy").toFormatter())
        .appendOptional(DateTimeFormatterBuilder().appendPattern("yyyy-MMM-dd").toFormatter())
        .appendOptional(DateTimeFormatterBuilder().appendPattern("MMM dd, yyyy").toFormatter())
        .appendOptional(
            DateTimeFormatterBuilder()
                .appendPattern(
                    "yyyy",
                ).parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .toFormatter(),
        ).appendOptional(DateTimeFormatterBuilder().appendPattern("MMMM yyyy").parseDefaulting(ChronoField.DAY_OF_MONTH, 1).toFormatter())
        .appendOptional(DateTimeFormatterBuilder().appendPattern("MMM, yyyy").parseDefaulting(ChronoField.DAY_OF_MONTH, 1).toFormatter())
        .appendOptional(DateTimeFormatterBuilder().appendPattern("d MMMM yyyy").toFormatter())
        .appendOptional(DateTimeFormatterBuilder().appendPattern("dd/MM/yyyy").toFormatter())
        .toFormatter()

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate? {
        val dateString = when (val jsonElement = decoder.decodeSerializableValue(JsonElement.serializer())) {
            is JsonObject -> jsonElement["value"]?.jsonPrimitive?.content
            is JsonPrimitive -> jsonElement.content
            else -> null
        } ?: return null

        try {
            return java.time.LocalDate
                .parse(dateString, formatter)
                .toKotlinLocalDate()
        } catch (dtpe: DateTimeParseException) {
            throw dtpe
        }
    }

    override fun serialize(encoder: Encoder, value: LocalDate?) {
        if (value != null) {
            encoder.encodeString(value.toString())
        } else {
            encoder.encodeNull()
        }
    }
}
