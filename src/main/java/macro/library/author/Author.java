package macro.library.author;

import macro.library.book.Book;
import macro.library.database.AuthorTable;
import macro.library.database.BookAuthorTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created by Macro303 on 2019-Oct-29
 */
public class Author implements Comparable<Author> {
	@NotNull
	private static final Comparator<Author> comparator = Comparator
			.comparing(Author::getLastName)
			.thenComparing(Author::getFirstName);
	@NotNull
	private final UUID uuid;
	@NotNull
	private String firstName;
	@NotNull
	private String lastName;
	@Nullable
	private String[] otherNames;

	public Author(@NotNull String firstName, @NotNull String lastName) {
		this(firstName, lastName, null);
	}

	public Author(@NotNull String firstName, @NotNull String lastName, @Nullable String[] otherNames) {
		this(UUID.randomUUID(), firstName, lastName, otherNames);
	}

	public Author(@NotNull UUID uuid, @NotNull String firstName, @NotNull String lastName, @Nullable String[] otherNames) {
		this.uuid = uuid;
		this.firstName = firstName;
		this.lastName = lastName;
		this.otherNames = otherNames;
	}

	@NotNull
	public static Author parseName(@NotNull String name) {
		var nameParts = new ArrayList<>(Arrays.asList(name.split(" ")));
		var first = nameParts.remove(0).trim();
		var last = nameParts.remove(nameParts.size() - 1).trim();
		var otherNames = nameParts.toArray(String[]::new);
		if (nameParts.size() == 0)
			otherNames = null;
		return new Author(first, last, otherNames);
	}

	public Author add() {
		AuthorTable.INSTANCE.insert(this);
		return this;
	}

	public Author push() {
		AuthorTable.INSTANCE.update(this);
		return this;
	}

	public void remove() {
		AuthorTable.INSTANCE.delete(uuid);
	}

	@Override
	public int compareTo(@NotNull Author other) {
		return comparator.compare(this, other);
	}

	public List<Book> getBooks() {
		return BookAuthorTable.INSTANCE.searchAuthor(uuid);
	}

	public String getDisplay() {
		return this.lastName + ", " + this.firstName + (this.otherNames == null ? "" : " " + String.join(" ", this.otherNames));
	}

	//<editor-fold desc="Getters and Setters" defaultstate="collapsed">
	@NotNull
	public UUID getUUID() {
		return uuid;
	}

	@NotNull
	public String getLastName() {
		return lastName;
	}

	public void setLastName(@NotNull String lastName) {
		this.lastName = lastName;
	}

	@NotNull
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(@NotNull String firstName) {
		this.firstName = firstName;
	}

	@Nullable
	public String[] getOtherNames() {
		return otherNames;
	}

	public void setOtherNames(@Nullable String[] otherNames) {
		this.otherNames = otherNames;
	}
	//</editor-fold>

	//<editor-fold desc="Object Functions" defaultstate="collapsed">
	@Override
	public int hashCode() {
		int result = uuid.hashCode();
		result = 31 * result + lastName.hashCode();
		result = 31 * result + firstName.hashCode();
		result = 31 * result + Arrays.hashCode(otherNames);
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Author)) return false;

		Author author = (Author) o;

		if (!uuid.equals(author.uuid)) return false;
		if (!lastName.equals(author.lastName)) return false;
		if (!firstName.equals(author.firstName)) return false;
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		return Arrays.equals(otherNames, author.otherNames);
	}

	@Override
	public String toString() {
		return "Author{" +
				"uuid=" + uuid +
				", lastName='" + lastName + '\'' +
				", firstName='" + firstName + '\'' +
				", otherNames=" + Arrays.toString(otherNames) +
				'}';
	}
	//</editor-fold>
}