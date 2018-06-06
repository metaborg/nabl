package mb.statix.solver.constraint;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.MatchException;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.statix.solver.Completeness;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Result;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public class CUser implements IConstraint {

    private final String name;
    private final List<ITerm> args;

    public CUser(String name, Iterable<? extends ITerm> args) {
        this.name = name;
        this.args = ImmutableList.copyOf(args);
    }

    @Override public Iterable<Tuple2<ITerm, ITerm>> scopeExtensions(Spec spec) {
        return spec.scopeExtensions().get(name).stream().map(il -> ImmutableTuple2.of(args.get(il._1()), il._2()))
                .collect(Collectors.toList());
    }

    public IConstraint apply(Function1<ITerm, ITerm> map) {
        final List<ITerm> newArgs = args.stream().map(map::apply).collect(Collectors.toList());
        return new CUser(name, newArgs);
    }

    public Optional<Result> solve(State state, Completeness completeness, IDebugContext debug)
            throws InterruptedException {
        final Set<Rule> rules = Sets.newHashSet(state.spec().rules().get(name));
        final Iterator<Rule> it = rules.iterator();
        while(it.hasNext()) {
            final Rule rule = it.next();
            Tuple3<Config, Set<ITermVar>, Set<IConstraint>> appl;
            try {
                appl = rule.apply(args, state, completeness);
            } catch(MatchException e) {
                debug.warn("Failed to instantiate {}(_) for arguments {}", name, args);
                continue;
            }
            debug.info("Try rule {}", rule);
            final Config result = Solver.solve(appl._1(), debug.subContext());
            if(result.state().isErroneous()) {
                debug.info("Rule rejected");
            } else if(result.constraints().isEmpty()
                    && state.unifier().equals(result.state().unifier().removeAll(appl._2()).unifier())) {
                // FIXME Check scope graph entailment
                debug.info("Rule accepted");
                return Optional.of(Result.of(result.state(), appl._3()));
            } else {
                debug.info("Rule delayed");
            }
        }
        if(rules.isEmpty()) {
            debug.info("No rule applies");
            return Optional.of(Result.of(state, ImmutableSet.of(new CFalse())));
        } else {
            return Optional.empty();
        }
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("(");
        sb.append(args.stream().map(unifier::findRecursive).collect(Collectors.toList()));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("(");
        sb.append(args);
        sb.append(")");
        return sb.toString();
    }

}