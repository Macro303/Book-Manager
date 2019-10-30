package macro.library.external;

import macro.library.Util;
import macro.library.author.Author;
import macro.library.book.Book;
import macro.library.book.Isbn;
import macro.library.database.AuthorTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Macro303 on 2019-Oct-22
 */
public abstract class OpenLibrary {
	@NotNull
	private static final Logger LOGGER = LogManager.getLogger(OpenLibrary.class);
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
		var publishers = bookObj.optJSONArray("publishers");
		if (publishers == null)
			publishers = new JSONArray();
		StringBuilder publisherStr = new StringBuilder();
		for (var publisher : publishers) {
			if (publisherStr.length() != 0)
				publisherStr.append(";");
			publisherStr.append(((JSONObject) publisher).getString("name"));
		}
		return new Book(isbn, title, subtitle, publisherStr.toString());
	}

	@NotNull
	public static List<Author> searchAuthors(@NotNull Isbn isbn) {
		var authorList = new ArrayList<Author>();
		var url = URL + "?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data";
		var request = Util.httpRequest(url);
		if (request == null)
			return authorList;
		JSONObject response = request.getObject();
		var bookObj = response.optJSONObject("ISBN:" + isbn);
		if (bookObj == null)
			return authorList;
		var authors = bookObj.optJSONArray("authors");
		if (authors == null)
			authors = new JSONArray();
		for (var authorName : authors) {
			var name = ((JSONObject) authorName).getString("name");
			var author = Author.parseName(name);
			var found = AuthorTable.INSTANCE.select(author.getFirstName(), author.getLastName());
			if (found == null)
				author = author.add();
			authorList.add(author);
		}
		return authorList;
	}
}