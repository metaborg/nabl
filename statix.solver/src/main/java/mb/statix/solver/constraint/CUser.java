package mb.statix.solver.constraint;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.MatchException;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.terms.unification.UnificationException;
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

    public Optional<Result> solve(final State state, Completeness completeness, IDebugContext debug)
            throws InterruptedException {
        final boolean wasErroneous = state.isErroneous();
        final Set<Rule> rules = Sets.newHashSet(state.spec().rules().get(name));
        final List<Result> results = Lists.newArrayListWithExpectedSize(1);
        final Iterator<Rule> it = rules.iterator();
        outer: while(it.hasNext()) {
            State result = state.withErroneous(false); // clear errors
            final Rule rule;
            try {
                final Tuple2<State, Rule> appl = it.next().apply(args, result);
                result = appl._1();
                rule = appl._2();
            } catch(MatchException | UnificationException e) {
                debug.warn("Failed to instantiate {}(_) for arguments {}", name, args);
                continue;
            }
            debug.info("Try rule {}", rule.toString(result.unifier()));
            for(IGuard guard : rule.getGuard()) {
                Optional<State> maybeResult = guard.solve(result, debug);
                if(!maybeResult.isPresent()) {
                    debug.info("Rule delayed (unsolved guard constraint)");
                    continue outer;
                } else if ((result = maybeResult.get()).isErroneous()) {
                    debug.info("Rule rejected (unsatisfied guard constraint)");
                    it.remove();
                    continue outer;
                }
            }
            if(state.entails(result, rule.getGuardVars())) {
                debug.info("Rule accepted");
                result = result.addErroneous(wasErroneous); // restore errors
                results.add(Result.of(result, rule.getBody()));
            } else {
                debug.info("Rule delayed (instantiated variables)");
            }
        }
        if(!results.isEmpty()) {
            if(results.size() > 1) {
                debug.error("Found overlapping rules");
                throw new IllegalArgumentException("Found overlapping rules");
            } else {
                return Optional.of(results.get(0));
            }
        } else if(rules.isEmpty()) {
            debug.info("No rule applies");
            return Optional.of(Result.of(state.addErroneous(true), ImmutableSet.of()));
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