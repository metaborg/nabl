package mb.p_raffrayi;

public class TypeCheckingFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    public TypeCheckingFailedException() {
    }

    public TypeCheckingFailedException(String message) {
        super(message);
    }

    public TypeCheckingFailedException(Throwable cause) {
        super(cause);
    }

    public TypeCheckingFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}