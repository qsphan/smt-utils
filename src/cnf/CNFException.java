package cnf;

public class CNFException extends Exception {

	private static final long serialVersionUID = -2367506951957439996L;

	public CNFException() {
		super();
	}

	public CNFException(String message) {
		super(message);
	}

	public CNFException(String message, Exception inner) {
		super(message, inner);
	}
}
