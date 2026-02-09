
public class Book {

	private final String title;
	private boolean available;

	public Book(String title, boolean available) {
		super();
		this.title = title;
		this.available = available;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public String getTitle() {
		return title;
	}
	
	
}
