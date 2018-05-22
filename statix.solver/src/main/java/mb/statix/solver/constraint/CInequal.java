package mb.statix.solver.constraint;

import java.util.Optional;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;

public class CInequal implements IConstraint {

    private final ITerm term1;
    private final ITerm term2;

    public CInequal(ITerm term1, ITerm term2) {
        this.term1 = term1;
        this.term2 = term2;
    }

    @Override public IConstraint apply(Function1<ITerm, ITerm> map) {
        return new CInequal(map.apply(term1), map.apply(term2));
    }

    @Override public Optional<Config> solve(State state) {
        IUnifier.Immutable unifier = state.unifier();
        if(unifier.areUnequal(term1, term2)) {
            return Optional.of(Config.builder().state(state).build());
        } else {
            return Optional.empty();
        }
    }

    @Override public String toString(IUnifier unifier) {
        return toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(term1);
        sb.append(" != ");
        sb.append(term2);
        return sb.toString();
    }

}