package mb.nabl2.scopegraph.esop.bottomup;

import java.io.Serializable;

import mb.nabl2.regexp.IRegExpMatcher;

class BUEnvKey<S, L> implements Serializable {
    private static final long serialVersionUID = 1L;

    public final BUEnvKind kind;
    public final S scope;
    public final IRegExpMatcher<L> wf;

    BUEnvKey(BUEnvKind kind, S scope, IRegExpMatcher<L> wf) {
        this.kind = kind;
        this.scope = scope;
        this.wf = wf;
        if(wf.isEmpty()) {
            throw new AssertionError();
        }
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = System.identityHashCode(this);
            hashCode = result;
        }
        return result;
    }

    @Override public boolean equals(Object obj) {
        return this == obj;
    }

    @Override public String toString() {
        return kind.toString() + "/" + scope.toString() + "/" + wf.regexp().toString();
    }

}