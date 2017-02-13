package org.metaborg.meta.nabl2.constraints.messages;

import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;

public enum MessageKind {

    ERROR, WARNING, NOTE;

    public static IMatcher<MessageKind> matcher() {
        return M.cases(
            // @formatter:off
            M.appl0("Error", e -> ERROR),
            M.appl0("Warning", e -> WARNING),
            M.appl0("Note", e -> NOTE)
            // @formatter:on
        );
    }

}