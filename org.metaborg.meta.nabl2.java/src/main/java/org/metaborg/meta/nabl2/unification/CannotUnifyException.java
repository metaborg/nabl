package org.metaborg.meta.nabl2.unification;

import org.metaborg.meta.nabl2.terms.ITerm;

public final class CannotUnifyException extends UnificationException {

    private static final long serialVersionUID = -1841084030559880800L;

    private final ITerm first;
    private final ITerm second;

    public CannotUnifyException(ITerm first, ITerm second) {
        this.first = first;
        this.second = second;
    }


    public ITerm getFirst() {
        return first;
    }

    public ITerm getSecond() {
        return second;
    }

}