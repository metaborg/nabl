package mb.nabl2.constraints.messages;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

public enum MessageKind {

    ERROR, WARNING, NOTE;

    private static final String ERROR_OP = "Error";
    private static final String WARNING_OP = "Warning";
    private static final String NOTE_OP = "Note";

    public static IMatcher<MessageKind> matcher() {
        // @formatter:off
        return M.cases(
            M.appl0(ERROR_OP, e -> ERROR),
            M.appl0(WARNING_OP, e -> WARNING),
            M.appl0(NOTE_OP, e -> NOTE)
        );
        // @formatter:on
    }

    public static ITerm build(MessageKind kind) {
        switch(kind) {
            case ERROR:
                return B.newAppl(ERROR_OP);
            case WARNING:
                return B.newAppl(WARNING_OP);
            case NOTE:
                return B.newAppl(NOTE_OP);
            default:
                throw new IllegalArgumentException();
        }
    }

}