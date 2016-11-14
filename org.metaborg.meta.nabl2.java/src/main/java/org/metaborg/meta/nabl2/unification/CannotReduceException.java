package org.metaborg.meta.nabl2.unification;

import org.metaborg.meta.nabl2.terms.ITermOp;

public final class CannotReduceException extends UnificationException {

    private static final long serialVersionUID = 7475905078983795126L;

    private final ITermOp op;

    public CannotReduceException(ITermOp op, Throwable cause) {
        super(cause);
        this.op = op;
    }

    public ITermOp getOp() {
        return op;
    }

}