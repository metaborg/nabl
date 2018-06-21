package mb.statix.solver.constraint;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.MatchException;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.Completeness;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.IGuard;
import mb.statix.solver.Result;
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

    public IConstraint apply(ISubstitution.Immutable subst) {
        final List<ITerm> newArgs = args.stream().map(subst::apply).collect(Collectors.toList());
        return new CUser(name, newArgs);
    }

    public Optional<Result> solve(State state, Completeness completeness, IDebugContext debug)
            throws InterruptedException {
        final Set<Rule> rules = Sets.newHashSet(state.spec().rules().get(name));
        final Iterator<Rule> it = rules.iterator();
        while(it.hasNext()) {
            final Rule rule = it.next();
            final Tuple2<State, Rule> appl;
            try {
                appl = rule.apply(args, state);
            } catch(MatchException e) {
                debug.warn("Failed to instantiate {}(_) for arguments {}", name, args);
                continue;
            }
            debug.info("Try rule {}", appl._2().toString(appl._1().unifier()));
            Optional<State> maybeResult = Optional.of(appl._1());
            for(IGuard guard : appl._2().getGuard()) {
                if(maybeResult.isPresent()) {
                    maybeResult = guard.solve(maybeResult.get(), debug);
                }
            }
            if(maybeResult.isPresent()) {
                final State result = maybeResult.get();
                if(result.isErroneous()) {
                    debug.info("Rule rejected");
                    it.remove();
                } else if(state.entails(result, appl._2().getGuardVars())) {
                    debug.info("Rule accepted");
                    return Optional.of(Result.of(maybeResult.get(), appl._2().getBody()));
                } else {
                    debug.info("Rule delayed");
                }
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
        sb.append(unifier.toString(args));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}