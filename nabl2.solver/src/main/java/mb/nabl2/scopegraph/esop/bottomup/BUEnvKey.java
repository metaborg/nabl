package mb.nabl2.scopegraph.esop.bottomup;

import java.io.Serializable;
import java.util.Objects;

import mb.nabl2.regexp.IRegExpMatcher;

class BUEnvKey<S, L> implements Serializable {
    private static final long serialVersionUID = 1L;

    public final BUEnvKind kind;
    public final S scope;
    public final IRegExpMatcher<L> wf;

    public BUEnvKey(BUEnvKind kind, S scope, IRegExpMatcher<L> wf) {
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
            result = Objects.hash(kind, scope, wf.regexp());
            hashCode = result;
        }
        return result;
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked") BUEnvKey<S, L> other = (BUEnvKey<S, L>) obj;
        return kind.equals(other.kind) && scope.equals(other.scope) && wf.regexp().equals(other.wf.regexp());
    }

    @Override public String toString() {
        return kind.toString() + "/" + scope.toString() + "/" + wf.regexp().toString();
    }

}