package macro.library.database;

import macro.library.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Macro303 on 2019-Oct-22
 */
public abstract class Table<T> {
	@NotNull
	private static final Logger LOGGER = LogManager.getLogger(Table.class);
	@NotNull
	protected final String tableName;

	protected Table(@NotNull String tableName) {
		this.tableName = tableName;
		if (!this.exists())
			this.createTable();
	}

	private boolean exists() {
		var query = "SELECT name FROM sqlite_master WHERE type = ? AND name = ?;";
		try (var conn = DriverManager.getConnection(Util.DATABASE_URL);
		     var statement = conn.prepareStatement(query)) {
			statement.setString(1, "table");
			statement.setString(2, tableName);
			var results = statement.executeQuery();
			return results.next();
		} catch (SQLException sqle) {
			return false;
		}
	}

	protected abstract void createTable();

	public void dropTable() {
		var query = String.format("DROP TABLE %s", tableName);
		delete(query);
	}

	protected boolean delete(@NotNull String query, @Nullable Object... values) {
		return update(query, values);
	}

	protected boolean update(@NotNull String query, @Nullable Object... values) {
		LOGGER.debug("{}, {}", query, Arrays.toString(values));
		try (var conn = DriverManager.getConnection(Util.DATABASE_URL)) {
			conn.setAutoCommit(false);
			try (var statement = conn.prepareStatement(query)) {
				for (int i = 0; i < (values != null ? values.length : 0); i++)
					statement.setObject(i + 1, values[i]);
				statement.executeUpdate();
				conn.commit();
				return true;
			} catch (SQLException sqle) {
				conn.rollback();
				throw sqle;
			}
		} catch (SQLException sqle) {
			LOGGER.error("Unable to Execute: {}, {} => {}", query, Arrays.toString(values), sqle);
		}
		return false;
	}

	@NotNull
	public List<T> searchAll() {
		var query = String.format("SELECT * FROM %s", tableName);
		return search(query);
	}

	@NotNull
	protected List<T> search(@NotNull String query, @Nullable Object... values) {
		var items = new ArrayList<T>();
		try (var conn = DriverManager.getConnection(Util.DATABASE_URL);
		     var statement = conn.prepareStatement(query)) {
			for (int i = 0; i < (values != null ? values.length : 0); i++)
				statement.setObject(i + 1, values[i]);
			var results = statement.executeQuery();
			while (results.next())
				items.add(parse(results));
		} catch (SQLException sqle) {
			LOGGER.error("Unable to Execute: {}, {} => {}", query, Arrays.toString(values), sqle);
			items.clear();
		}
		return items;
	}

	@Nullable
	protected abstract T parse(ResultSet result) throws SQLException;

	protected boolean insert(@NotNull String query, @Nullable Object... values) {
		return update(query, values);
	}
}