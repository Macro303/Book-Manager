package macro.library.book;

import macro.library.Isbn;
import macro.library.Util;
import macro.library.database.BookTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

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
	private String author;
	@NotNull
	private String publisher;
	@NotNull
	private Format format;

	public Book(@NotNull Isbn isbn, @NotNull String title, @Nullable String subtitle, @NotNull String author, @NotNull String publisher) {
		this(isbn, title, subtitle, author, publisher, Format.PAPERBACK);
	}

	public Book(@NotNull Isbn isbn, @NotNull String title, @Nullable String subtitle, @NotNull String author, @NotNull String publisher, @NotNull Format format) {
		this.isbn = isbn;
		this.title = title;
		this.subtitle = subtitle;
		this.author = author;
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
	public String getAuthor() {
		return author;
	}

	public void setAuthor(@NotNull String author) {
		this.author = author;
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
}