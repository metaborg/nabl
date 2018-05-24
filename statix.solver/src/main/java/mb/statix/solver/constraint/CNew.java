package mb.statix.solver.constraint;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;

public class CNew implements IConstraint {

    private final List<ITerm> terms;

    public CNew(Iterable<ITerm> terms) {
        this.terms = ImmutableList.copyOf(terms);
    }

    @Override public IConstraint apply(Function1<ITerm, ITerm> map) {
        return new CNew(terms.stream().map(map::apply).collect(Collectors.toList()));
    }

    @Override public Optional<Config> solve(State state, IDebugContext debug) {
        final List<IConstraint> constraints = Lists.newArrayList();
        State newState = state;
        for(ITerm t : terms) {
            final String base = M.var(ITermVar::getName).match(t).orElse("s");
            Tuple2<ITerm, State> ss = newState.freshScope(base);
            constraints.add(new CEqual(t, ss._1()));
            newState = ss._2();
        }
        return Optional.of(Config.of(newState, constraints));
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("new ");
        sb.append(terms.stream().map(unifier::findRecursive).collect(Collectors.toList()));
        return sb.toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("new ");
        sb.append(terms);
        return sb.toString();
    }

}