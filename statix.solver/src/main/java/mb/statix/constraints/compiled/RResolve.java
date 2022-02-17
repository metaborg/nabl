package mb.statix.constraints.compiled;

import java.io.Serializable;

public final class RResolve implements RExp, Serializable {

    private static final long serialVersionUID = 1L;

    private static final RResolve instance = new RResolve();

    private RResolve() {
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseResolve();
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
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

    public static final RResolve of() {
        return instance;
    }


}
