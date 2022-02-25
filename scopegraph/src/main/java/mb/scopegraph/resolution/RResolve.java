package mb.scopegraph.resolution;

import java.io.Serializable;

import mb.scopegraph.oopsla20.reference.ResolutionException;

public final class RResolve<L> implements RExp<L>, Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes") private static final RResolve instance = new RResolve();

    private RResolve() {
    }

    @Override public <R> R match(Cases<L, R> cases) {
        return cases.caseResolve();
    }

    @Override public <R, E extends Throwable> R matchInResolution(ResolutionCases<L, R> cases)
            throws ResolutionException, InterruptedException {
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
