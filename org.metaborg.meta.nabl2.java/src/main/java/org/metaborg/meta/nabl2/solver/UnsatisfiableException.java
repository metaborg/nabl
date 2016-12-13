package org.metaborg.meta.nabl2.solver;

import org.metaborg.meta.nabl2.constraints.IConstraint;

import com.google.common.collect.ImmutableList;

public class UnsatisfiableException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ImmutableList<IConstraint> unsatCore;

    public UnsatisfiableException(IConstraint... unsatCore) {
        this((Throwable) null, unsatCore);
    }

    public UnsatisfiableException(Throwable cause, IConstraint... unsatCore) {
        this("Constraints cannot be satisfied.", cause, unsatCore);
    }

    public UnsatisfiableException(String message, IConstraint... unsatCore) {
        this(message, null, unsatCore);
    }

    public UnsatisfiableException(String message, Throwable cause, IConstraint... unsatCore) {
        super(message, cause);
        this.unsatCore = ImmutableList.copyOf(unsatCore);
    }

    public Iterable<IConstraint> getUnsatCore() {
        return unsatCore;
    }

}