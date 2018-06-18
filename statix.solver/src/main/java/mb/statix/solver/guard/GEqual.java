package mb.statix.solver.guard;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnificationException;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.IGuard;
import mb.statix.solver.State;

public class GEqual implements IGuard {

    private final ITerm term1;
    private final ITerm term2;

    public GEqual(ITerm term1, ITerm term2) {
        this.term1 = term1;
        this.term2 = term2;
    }

    @Override public IGuard apply(ISubstitution.Immutable subst) {
        return new GEqual(subst.apply(term1), subst.apply(term2));
    }

    @Override public Optional<State> solve(State state, IDebugContext debug) {
        IUnifier.Immutable unifier = state.unifier();
        try {
            final IUnifier.Immutable.Result<IUnifier.Immutable> result = unifier.unify(term1, term2);
            debug.info("Unification succeeded");
            return Optional.of(state.withUnifier(result.unifier()));
        } catch(UnificationException e) {
            debug.info("Unification failed");
            return Optional.of(state.withErroneous(true));
        }
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append(unifier.toString(term1));
        sb.append(" == ");
        sb.append(unifier.toString(term2));
        return sb.toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(term1);
        sb.append(" == ");
        sb.append(term2);
        return sb.toString();
    }

}