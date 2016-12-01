package org.metaborg.meta.nabl2.solver;

import org.metaborg.meta.nabl2.constraints.IConstraint;

public class UnsatisfiableException extends Exception {

    private static final long serialVersionUID = 1L;

    public UnsatisfiableException(IConstraint... unsatCore) {
    }

    public UnsatisfiableException(Iterable<IConstraint> unsatCore) {
    }

}