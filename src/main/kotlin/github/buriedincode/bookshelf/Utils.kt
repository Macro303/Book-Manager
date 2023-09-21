package github.buriedincode.bookshelf

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.sksamuel.hoplite.Secret
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.io.path.div

object Utils : Logging {
    private val HOME_ROOT: Path = Paths.get(System.getProperty("user.home"))
    private val DATABASE: Database by lazy {
        Database.connect(url = "jdbc:sqlite:${Settings.load().database}", driver = "org.sqlite.JDBC")
    }

    internal const val VERSION = "0.1.0"
    internal val CACHE_ROOT = HOME_ROOT / ".cache" / "bookshelf"
    internal val CONFIG_ROOT = HOME_ROOT / ".config" / "bookshelf"
    internal val DATA_ROOT = HOME_ROOT / ".local" / "share" / "bookshelf"

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    init {
        if (!Files.exists(CACHE_ROOT)) {
            CACHE_ROOT.toFile().mkdirs()
        }
        if (!Files.exists(CONFIG_ROOT)) {
            CONFIG_ROOT.toFile().mkdirs()
        }
        if (!Files.exists(DATA_ROOT)) {
            DATA_ROOT.toFile().mkdirs()
        }
    }

    private fun getDayNumberSuffix(day: Int): String {
        return if (day in 11..13) {
            "th"
        } else {
            when (day % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }
    }

    internal val JSON_MAPPER: ObjectMapper = JsonMapper.builder()
        .addModule(JavaTimeModule())
        .addModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, true)
                .configure(KotlinFeature.NullToEmptyMap, true)
                .configure(KotlinFeature.NullIsSameAsDefault, true)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, true)
                .build(),
        )
        .build()

    internal fun <T> query(block: () -> T): T {
        val startTime = LocalDateTime.now()
        val transaction = transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, db = DATABASE) {
            repetitionAttempts = 1
            addLogger(Slf4jSqlDebugLogger)
            block()
        }
        logger.debug("Took ${ChronoUnit.MILLIS.between(startTime, LocalDateTime.now())}ms")
        return transaction
    }

    inline fun <reified T : Enum<T>> String.asEnumOrNull(): T? = enumValues<T>().firstOrNull { it.name.equals(this, ignoreCase = true) }

    inline fun <reified T : Enum<T>> T.titlecase(): String {
        return this.name.lowercase().split("_").joinToString(" ") {
            it.replaceFirstChar(Char::uppercaseChar)
        }
    }

    fun toHumanReadable(milliseconds: Float): String {
        val duration = Duration.ofMillis(milliseconds.toLong())
        val minutes = duration.toMinutes()
        if (minutes > 0) {
            val seconds = duration.minusMinutes(minutes).toSeconds()
            val millis = duration.minusMinutes(minutes).minusSeconds(seconds)
            return "${minutes}min ${seconds}sec ${millis}ms"
        }
        val seconds = duration.toSeconds()
        if (seconds > 0) {
            val millis = duration.minusSeconds(seconds).toMillis()
            return "${seconds}sec ${millis}ms"
        }
        return "${duration.toMillis()}ms"
    }

    fun getHumanReadableDateFormatter(date: LocalDate): DateTimeFormatter =
        DateTimeFormatter.ofPattern(
            "d'${getDayNumberSuffix(date.dayOfMonth)}' MMM yyyy",
        )

    fun Secret?.isNullOrBlank(): Boolean = this?.value.isNullOrBlank()
}
