package macro.library.open_library;

import macro.library.Isbn;
import macro.library.Util;
import macro.library.book.Book;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

/**
 * Created by Macro303 on 2019-Oct-22
 */
public abstract class OpenLibrary {
	@NotNull
	private static final String URL = "http://openlibrary.org/api/books";

	@Nullable
	public static Book searchBook(@NotNull Isbn isbn) {
		var url = URL + "?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data";
		var request = Util.httpRequest(url);
		if (request == null)
			return null;
		JSONObject response = request.getObject();
		var bookObj = response.optJSONObject("ISBN:" + isbn);
		if (bookObj == null)
			return null;
		var title = bookObj.getString("title");
		var subtitle = bookObj.optString("subtitle");
		if (subtitle.isBlank())
			subtitle = null;
		var authors = bookObj.optJSONArray("authors");
		StringBuilder authorStr = new StringBuilder();
		for (var author : authors) {
			if (authorStr.length() != 0)
				authorStr.append(";");
			authorStr.append(((JSONObject) author).getString("name"));
		}
		var publishers = bookObj.optJSONArray("publishers");
		StringBuilder publisherStr = new StringBuilder();
		for (var publisher : publishers) {
			if (publisherStr.length() != 0)
				publisherStr.append(";");
			publisherStr.append(((JSONObject) publisher).getString("name"));
		}
		return new Book(isbn, title, subtitle, authorStr.toString(), publisherStr.toString());
	}
}