package macro.library.book;

import macro.library.Util;
import macro.library.author.Author;
import macro.library.database.BookAuthorTable;
import macro.library.database.BookTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Created by Macro303 on 2019-Oct-22
 */
public class Book implements Comparable<Book> {
	@NotNull
	private static final Comparator<Book> comparator = Comparator
			.comparing(Book::getTitle)
			.thenComparing(Book::getSubtitle, Util.nullSafeComparator)
			.thenComparing(Book::getFormat)
			.thenComparing(Book::getISBN);
	@NotNull
	private final Isbn isbn;
	@NotNull
	private String title;
	@Nullable
	private String subtitle;
	@NotNull
	private String publisher;
	@NotNull
	private Format format;

	public Book(@NotNull Isbn isbn, @NotNull String title, @Nullable String subtitle, @NotNull String publisher) {
		this(isbn, title, subtitle, publisher, Format.PAPERBACK);
	}

	public Book(@NotNull Isbn isbn, @NotNull String title, @Nullable String subtitle, @NotNull String publisher, @NotNull Format format) {
		this.isbn = isbn;
		this.title = title;
		this.subtitle = subtitle;
		this.publisher = publisher;
		this.format = format;
	}

	public Book add() {
		BookTable.INSTANCE.insert(this);
		return this;
	}

	public Book push() {
		BookTable.INSTANCE.update(this);
		return this;
	}

	public void remove() {
		BookTable.INSTANCE.delete(isbn);
	}

	@Override
	public int compareTo(@NotNull Book other) {
		return comparator.compare(this, other);
	}

	//<editor-fold desc="Getters and Setters" defaultstate="collapsed">
	@NotNull
	public Isbn getISBN() {
		return isbn;
	}

	@NotNull
	public String getTitle() {
		return title;
	}

	public void setTitle(@NotNull String title) {
		this.title = title;
	}

	@Nullable
	public String getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(@Nullable String subtitle) {
		this.subtitle = subtitle;
	}

	@NotNull
	public List<Author> getAuthors() {
		return BookAuthorTable.INSTANCE.searchBook(isbn);
	}

	@NotNull
	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(@NotNull String publisher) {
		this.publisher = publisher;
	}

	@NotNull
	public Format getFormat() {
		return format;
	}

	public void setFormat(@NotNull Format format) {
		this.format = format;
	}
	//</editor-fold>

	//<editor-fold desc="Object Functions" defaultstate="collapsed">
	@Override
	public int hashCode() {
		int result = isbn.hashCode();
		result = 31 * result + title.hashCode();
		result = 31 * result + (subtitle != null ? subtitle.hashCode() : 0);
		result = 31 * result + publisher.hashCode();
		result = 31 * result + format.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Book)) return false;

		Book book = (Book) o;

		if (!isbn.equals(book.isbn)) return false;
		if (!title.equals(book.title)) return false;
		if (!Objects.equals(subtitle, book.subtitle)) return false;
		if (!publisher.equals(book.publisher)) return false;
		return format == book.format;
	}

	@Override
	public String toString() {
		return "Book{" +
				"isbn=" + isbn +
				", title='" + title + '\'' +
				", subtitle='" + subtitle + '\'' +
				", publisher='" + publisher + '\'' +
				", format=" + format +
				'}';
	}
	//</editor-fold>
}