package macro.library.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Macro303 on 2019-Oct-22
 */
public abstract class IdTable<T, S> extends Table<T> {
	@NotNull
	protected final String idName;

	protected IdTable(@NotNull String tableName, @NotNull String idName) {
		super(tableName);
		this.idName = idName;
	}

	@Nullable
	public T selectUnique(S unique) {
		var query = String.format("SELECT * FROM %s WHERE %s = ?", tableName, idName);
		return search(query, unique).stream().findFirst().orElse(null);
	}

	public boolean delete(S unique){
		var query = String.format("DELETE FROM %s WHERE %s = ?;", tableName, idName);
		return delete(query, unique);
	}

	public abstract boolean insert(T item);
	public abstract boolean update(T item);
}