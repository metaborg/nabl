package mb.scopegraph.oopsla20.diff;

import org.metaborg.util.functions.Predicate2;

public interface ScopeGraphDifferOps<S, D> {

    boolean isMatchAllowed(S current, S previous);

    java.util.Set<S> getCurrentScopes(D datum);

    java.util.Set<S> getPreviousScopes(D datum);

    boolean matchDatums(D current, D previous, Predicate2<S, S> matchScopes);

}