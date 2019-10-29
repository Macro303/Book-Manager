package macro.library.book;

import macro.library.database.BookTable;
import macro.library.database.WishlistTable;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Created by Macro303 on 2019-Oct-29
 */
public class WishlistBook implements Comparable<WishlistBook> {
	@NotNull
	private static final Comparator<WishlistBook> comparator = Comparator
			.comparing(WishlistBook::getBook);
	@NotNull
	private final Isbn bookId;
	private int count;

	public WishlistBook(@NotNull Isbn bookId) {
		this(bookId, 1);
	}

	public WishlistBook(@NotNull Isbn bookId, int count) {
		this.bookId = bookId;
		this.count = count;
	}

	@NotNull
	public Book getBook() {
		var temp = BookTable.INSTANCE.selectUnique(bookId);
		assert temp != null;
		return temp;
	}

	public WishlistBook add() {
		WishlistTable.INSTANCE.insert(this);
		return this;
	}

	public WishlistBook push() {
		WishlistTable.INSTANCE.update(this);
		return this;
	}

	public void remove() {
		WishlistTable.INSTANCE.delete(bookId);
	}

	@Override
	public int compareTo(@NotNull WishlistBook other) {
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
		if (!(o instanceof WishlistBook)) return false;

		WishlistBook that = (WishlistBook) o;

		if (count != that.count) return false;
		return bookId.equals(that.bookId);
	}

	@Override
	public String toString() {
		return "WishlistBook{" +
				"bookId=" + bookId +
				", count=" + count +
				'}';
	}
	//</editor-fold>
}