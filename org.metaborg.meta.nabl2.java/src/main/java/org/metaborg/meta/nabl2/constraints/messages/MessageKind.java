package org.metaborg.meta.nabl2.constraints.messages;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;

public enum MessageKind {

    ERROR, WARNING, NOTE;

    private static final String ERROR_OP = "Error";
    private static final String WARNING_OP = "Warning";
    private static final String NOTE_OP = "Note";

    public static IMatcher<MessageKind> matcher() {
        return M.cases(
            // @formatter:off
            M.appl0(ERROR_OP, e -> ERROR),
            M.appl0(WARNING_OP, e -> WARNING),
            M.appl0(NOTE_OP, e -> NOTE)
            // @formatter:on
        );
    }

    public static ITerm build(MessageKind kind) {
        switch(kind) {
            case ERROR:
                return TB.newAppl(ERROR_OP);
            case WARNING:
                return TB.newAppl(WARNING_OP);
            case NOTE:
                return TB.newAppl(NOTE_OP);
            default:
                throw new IllegalArgumentException();
        }
    }

}