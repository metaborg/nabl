package org.metaborg.meta.nabl2.solver;

import org.metaborg.meta.nabl2.constraints.MessageInfo.Kind;
import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.collect.ImmutableList;

public class UnsatisfiableException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Kind kind;
    private final ImmutableList<ITerm> programPoints;

    public UnsatisfiableException(Kind kind, String message, Iterable<ITerm> programPoints) {
        this(kind, message, null, programPoints);
    }

    public UnsatisfiableException(Kind kind, String message, Throwable cause, Iterable<ITerm> programPoints) {
        super(message, cause);
        this.kind = kind;
        this.programPoints = ImmutableList.copyOf(programPoints);
    }

    public Kind getKind() {
        return kind;
    }

    public Iterable<ITerm> getProgramPoints() {
        return programPoints;
    }

}