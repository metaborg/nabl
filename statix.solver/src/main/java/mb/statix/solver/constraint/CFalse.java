package mb.statix.solver.constraint;

import java.util.Optional;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;

public class CFalse implements IConstraint {

    public IConstraint apply(Function1<ITerm, ITerm> map) {
        return this;
    }

    public Optional<Config> solve(final State state) {
        final State newState = state.withErroneous(true);
        return Optional.of(Config.builder().state(newState).build());
    }

    @Override public String toString() {
        return "false";
    }

}