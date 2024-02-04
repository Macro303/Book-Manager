package github.buriedincode.bookshelf

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.sksamuel.hoplite.Secret
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.ExperimentalKeywordApi
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
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.io.path.div

object Utils : Logging {
    private val HOME_ROOT: Path = Paths.get(System.getProperty("user.home"))
    private val XDG_CACHE: Path = System.getenv("XDG_CACHE_HOME")?.let {
        Paths.get(it)
    } ?: (HOME_ROOT / ".cache")
    private val XDG_CONFIG: Path = System.getenv("XDG_CONFIG_HOME")?.let {
        Paths.get(it)
    } ?: (HOME_ROOT / ".config")
    private val XDG_DATA: Path = System.getenv("XDG_DATA_HOME")?.let {
        Paths.get(it)
    } ?: (HOME_ROOT / ".local" / "share")

    internal val CACHE_ROOT = XDG_CACHE / "bookshelf"
    internal val CONFIG_ROOT = XDG_CONFIG / "bookshelf"
    internal val DATA_ROOT = XDG_DATA / "bookshelf"
    internal const val VERSION = "0.3.0"

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

    private val DATABASE: Database by lazy {
        val settings = Settings.load()
        if (settings.database.source == Settings.Database.Source.MYSQL) {
            return@lazy Database.connect(
                url = settings.database.url,
                driver = "com.mysql.cj.jdbc.Driver",
                user = settings.database.user ?: "username",
                password = settings.database.password ?: "password",
                databaseConfig = DatabaseConfig {
                    @OptIn(ExperimentalKeywordApi::class)
                    preserveKeywordCasing = true
                },
            )
        } else if (settings.database.source == Settings.Database.Source.POSTGRES) {
            return@lazy Database.connect(
                url = settings.database.url,
                driver = "org.postgresql.Driver",
                user = settings.database.user ?: "user",
                password = settings.database.password ?: "password",
                databaseConfig = DatabaseConfig {
                    @OptIn(ExperimentalKeywordApi::class)
                    preserveKeywordCasing = true
                },
            )
        }
        return@lazy Database.connect(
            url = settings.database.url,
            driver = "org.sqlite.JDBC",
            databaseConfig = DatabaseConfig {
                @OptIn(ExperimentalKeywordApi::class)
                preserveKeywordCasing = true
            },
        )
    }

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

    fun Secret?.isNullOrBlank(): Boolean = this?.value.isNullOrBlank()

    fun String.toLocalDateOrNull(pattern: String): LocalDate? {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
            LocalDate.parse(this, formatter)
        } catch (dtpe: DateTimeParseException) {
            null
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

    fun LocalDate.toHumanReadable(): String {
        val pattern = "d'${getDayNumberSuffix(this.dayOfMonth)}' MMM yyyy"
        return this.toString(pattern)
    }

    fun LocalDate.toString(pattern: String): String {
        return this.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
    }
}
