package macro.library.book;

import macro.library.Isbn;
import macro.library.database.BookTable;
import macro.library.database.CollectionTable;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Created by Macro303 on 2019-Oct-29
 */
public class CollectionBook implements Comparable<CollectionBook> {
	@NotNull
	private static final Comparator<CollectionBook> comparator = Comparator
			.comparing(CollectionBook::getBook);
	@NotNull
	private final Isbn bookId;
	private int count;

	public CollectionBook(@NotNull Isbn bookId) {
		this(bookId, 1);
	}

	public CollectionBook(@NotNull Isbn bookId, int count) {
		this.bookId = bookId;
		this.count = count;
	}

	@NotNull
	public Book getBook() {
		var temp = BookTable.INSTANCE.selectUnique(bookId);
		assert temp != null;
		return temp;
	}

	public CollectionBook add() {
		CollectionTable.INSTANCE.insert(this);
		return this;
	}

	public CollectionBook push() {
		CollectionTable.INSTANCE.update(this);
		return this;
	}

	public void remove() {
		CollectionTable.INSTANCE.delete(bookId);
	}

	@Override
	public int compareTo(@NotNull CollectionBook other) {
		return comparator.compare(this, other);
	}

	//<editor-fold desc="Getters and Setters" defaultstate="collapsed">
	@NotNull
	public Isbn getBookId() {
		return bookId;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	//</editor-fold>

	//<editor-fold desc="Object Functions" defaultstate="collapsed">
	@Override
	public int hashCode() {
		int result = bookId.hashCode();
		result = 31 * result + count;
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof CollectionBook)) return false;

		CollectionBook that = (CollectionBook) o;

		if (count != that.count) return false;
		return bookId.equals(that.bookId);
	}

	@Override
	public String toString() {
		return "CollectionBook{" +
				"bookId=" + bookId +
				", count=" + count +
				'}';
	}
	//</editor-fold>
}