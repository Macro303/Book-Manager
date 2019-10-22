package macro.library;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import macro.library.config.Config;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Macro303 on 2019-Oct-21
 */
public abstract class Util {
	private static final Logger LOGGER = LogManager.getLogger(Util.class);
	private static final Map<String, String> HEADERS = Map.of(
			"Accept", "application/json; charset=UTF-8",
			"User-Agent", "Book-Manager"
	);
	public static final String SQLITE_DATABASE = "Book-Manager.sqlite";
	public static final String DATABASE_URL = "jdbc:sqlite:" + SQLITE_DATABASE;
	public static Comparator<String> nullSafeComparator = Comparator.nullsLast(String::compareToIgnoreCase);

	static {
		Unirest.setProxy(Config.CONFIG.getProxy().getHttpHost());
	}

	public static String padStr(@Nullable String str, int count) {
		if (str == null || Objects.equals(str, "null"))
			str = "";
		return String.format("%" + count + "s", str);
	}

	@Nullable
	public static JsonNode httpRequest(@NotNull String url) {
		return httpRequest(url, HEADERS);
	}

	@Nullable
	public static JsonNode httpRequest(@NotNull String url, @NotNull Map<String, String> headers) {
		var request = Unirest.get(url);
		request.headers(headers);
		LOGGER.debug("GET : >>> - " + request.getUrl() + " - " + headers);
		HttpResponse<JsonNode> response;
		try {
			response = request.asJson();
		} catch (UnirestException ue) {
			LOGGER.error("Unable to load URL: " + ue);
			return null;
		}
		var level = Level.ERROR;
		if (response.getStatus() < 100)
			level = Level.ERROR;
		else if (response.getStatus() < 200)
			level = Level.INFO;
		else if (response.getStatus() < 300)
			level = Level.INFO;
		else if (response.getStatus() < 400)
			level = Level.WARN;
		else if (response.getStatus() < 500)
			level = Level.WARN;
		LOGGER.log(level, "GET: " + response.getStatus() + " " + response.getStatusText() + " - " + request.getUrl());
		LOGGER.debug("Response: " + response.getBody());
		return response.getStatus() != 200 ? null : response.getBody();
	}
}