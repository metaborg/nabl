package mb.statix.solver.guard;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.solver.Delay;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.IGuard;
import mb.statix.solver.State;

public class GInequal implements IGuard {

    private final ITerm term1;
    private final ITerm term2;

    public GInequal(ITerm term1, ITerm term2) {
        this.term1 = term1;
        this.term2 = term2;
    }

    @Override public IGuard apply(ISubstitution.Immutable subst) {
        return new GInequal(subst.apply(term1), subst.apply(term2));
    }

    @Override public Optional<State> solve(State state, IDebugContext debug) throws Delay {
        final IUnifier.Immutable unifier = state.unifier();
        if(unifier.areUnequal(term1, term2)) {
            return Optional.of(state);
        } else if(unifier.areEqual(term1, term2)) {
            return Optional.empty();
        } else {
            throw new Delay();
        }
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append(unifier.toString(term1));
        sb.append(" != ");
        sb.append(unifier.toString(term2));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}