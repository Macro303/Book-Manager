package macro.library.database;

import macro.library.author.Author;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Created by Macro303 on 2019-Oct-29
 */
public class AuthorTable extends IdTable<Author, UUID> {
	@NotNull
	private static final Logger LOGGER = LogManager.getLogger(AuthorTable.class);
	@NotNull
	public static final AuthorTable INSTANCE = new AuthorTable();

	private AuthorTable() {
		super("author", "uuid");
	}

	@Override
	public boolean insert(@NotNull Author item) {
		var query = String.format("INSERT INTO %s(uuid, firstName, lastName, middleNames) VALUES(?, ?, ?, ?);", tableName);
		return insert(query, item.getUUID(), item.getFirstName(), item.getLastName(), item.getOtherNames() == null ? null : String.join(";", item.getOtherNames()));
	}

	@Override
	public boolean update(@NotNull Author item) {
		var query = String.format("UPDATE %s SET firstName = ?, lastName = ?, middleNames = ? WHERE uuid = ?;", tableName);
		return update(query, item.getFirstName(), item.getLastName(), String.join(";", item.getOtherNames() == null ? null : String.join(";", item.getOtherNames())), item.getUUID());
	}

	@Override
	protected void createTable() {
		var query = String.format("CREATE TABLE %s(uuid TEXT PRIMARY KEY NOT NULL UNIQUE, firstName TEXT NOT NULL, lastName TEXT NOT NULL, middleNames TEXT, UNIQUE(firstName, lastName));", tableName);
		insert(query);
	}

	@Nullable
	@Override
	protected Author parse(@NotNull ResultSet result) throws SQLException {
		String[] middleNames = null;
		var middleName = result.getString("middleNames");
		if (middleName != null)
			middleNames = middleName.split(";");
		return new Author(
				UUID.fromString(result.getString("uuid")),
				result.getString("firstName"),
				result.getString("lastName"),
				middleNames
		);
	}

	@Nullable
	public Author select(@NotNull String firstName, @NotNull String lastName) {
		var query = String.format("SELECT * FROM %s WHERE firstName = ? AND lastName = ?;", tableName);
		return search(query, firstName, lastName).stream().findFirst().orElse(null);
	}
}