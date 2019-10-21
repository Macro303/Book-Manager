package macro.library;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Macro303 on 2019-Oct-21
 */
public abstract class Util {
	private static final Logger LOGGER = LogManager.getLogger(Util.class);
	public static final String SQLITE_DATABASE = "Book-Manager.sqlite";
	public static final String DATABASE_URL = "jdbc:sqlite:" + SQLITE_DATABASE;

	public static String padStr(@NotNull String str, int count){
		return String.format("%" + count + "s", str);
	}
}