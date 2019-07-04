package statix.cli.incremental.changes;

public class NotApplicableException extends RuntimeException {
    public NotApplicableException(IncrementalChange change, String file) {
        super("Incremental change " + change + " is not applicable to " + file);
    }
    
    public NotApplicableException(String msg) {
        super(msg);
    }
    
    public static NotApplicableException withReason(IncrementalChange change, String reason) {
        return new NotApplicableException("Incremental change " + change + " is not applicable here: " + reason);
    }
}
