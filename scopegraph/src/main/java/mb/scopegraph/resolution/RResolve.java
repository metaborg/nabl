package mb.scopegraph.resolution;

import java.io.Serializable;

public final class RResolve<L> implements RExp<L>, Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes") private static final RResolve instance = new RResolve();

    private RResolve() {
    }

    @Override public <R> R match(Cases<L, R> cases) {
        return cases.caseResolve();
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<L, R, E> cases) throws E {
        return cases.caseResolve();
    }

    @Override public String toString() {
        return "resolve";
    }

    @Override public boolean equals(Object obj) {
        return obj == this;
    }

    @Override public int hashCode() {
        return 1;
    }

    protected Object readResolve() {
        return instance;
    }

    @SuppressWarnings("unchecked") public static final <L> RResolve<L> of() {
        return instance;
    }

}
