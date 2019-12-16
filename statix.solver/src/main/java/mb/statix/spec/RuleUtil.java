package mb.statix.spec;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.Diseq;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.terms.unification.ud.PersistentUniDisunifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.StateUtil;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;

public class RuleUtil {

    private static ILogger log = LoggerUtils.logger(RuleUtil.class);

    /**
     * Apply the given list of rules to the given arguments. Returns the result of application if one rule can be
     * applied, empty of empty of no rules apply, or empty if more rules applied. Rules are expected to be in matching
     * order, with the first rule that can be applied is selected if the match is unconditional, or it is the only rule
     * that can be applied.
     * 
     * @param state
     *            Initial state
     * @param rules
     *            Ordered list of rules to apply
     * @param args
     *            Arguments to apply the rules to
     * @param cause
     *            Cause of this rule application, null if none
     * 
     * @return Some application result if one rule applies, some empty if no rules apply, and empty if multiple rules
     *         apply.
     */
    public static final Optional<Optional<Tuple2<Rule, ApplyResult>>> applyOne(IState.Immutable state, List<Rule> rules,
            List<? extends ITerm> args, @Nullable IConstraint cause) {
        return apply(state, rules, args, cause, true).map(rs -> rs.stream().collect(MoreCollectors.toOptional()));
    }

    /**
     * Apply the given list of rules to the given arguments. Returns application results for rules can be applied. Rules
     * are expected to be in matching order, with rules being selected up to and including the first rule that can be
     * applied unconditional.
     * 
     * @param state
     *            Initial state
     * @param rules
     *            Ordered list of rules to apply
     * @param args
     *            Arguments to apply the rules to
     * @param cause
     *            Cause of this rule application, null if none
     * 
     * @return A list of apply results, up to and including the first unconditionally matching result.
     */
    public static final List<Tuple2<Rule, ApplyResult>> applyAll(IState.Immutable state, List<Rule> rules,
            List<? extends ITerm> args, @Nullable IConstraint cause) {
        return apply(state, rules, args, cause, false).get();
    }

    /**
     * Helper method to apply the given list of ordered rules to the given arguments. Returns a list of results for all
     * rules that could be applied, or empty if onlyOne is true, and multiple matches were found.
     */
    private static final Optional<List<Tuple2<Rule, ApplyResult>>> apply(IState.Immutable state, List<Rule> rules,
            List<? extends ITerm> args, @Nullable IConstraint cause, boolean onlyOne) {
        final ImmutableList.Builder<Tuple2<Rule, ApplyResult>> results = ImmutableList.builder();
        final AtomicBoolean foundOne = new AtomicBoolean(false);
        for(Rule rule : rules) {
            // apply rule
            final ApplyResult applyResult;
            if((applyResult = apply(state, rule, args, cause).orElse(null)) == null) {
                // this rule does not apply, continue to next rules
                continue;
            }
            if(onlyOne && foundOne.getAndSet(true)) {
                // we require exactly one, but found multiple
                return Optional.empty();
            }
            results.add(ImmutableTuple2.of(rule, applyResult));

            // stop or add guard to state for next rule
            final Tuple3<Set<ITermVar>, ITerm, ITerm> guard;
            if((guard = applyResult.guard().map(Diseq::toTuple).orElse(null)) == null) {
                // next rules are unreachable after this unconditional match
                break;
            }
            final Optional<IUniDisunifier.Immutable> newUnifier =
                    state.unifier().disunify(guard._1(), guard._2(), guard._3()).map(IUniDisunifier.Result::unifier);
            if(!newUnifier.isPresent()) {
                // guards are equalities missing in the unifier, disunifying them should never fail
                throw new IllegalStateException("Unexpected incompatible guard.");
            }
            state = state.withUnifier(newUnifier.get());
        }
        return Optional.of(results.build());
    }

    /**
     * Apply the given rule to the given arguments. Returns the result of application, or nothing of the rule cannot be
     * applied.
     */
    public static final Optional<ApplyResult> apply(IState.Immutable state, Rule rule, List<? extends ITerm> args,
            @Nullable IConstraint cause) {
        // create equality constraints
        final IState.Transient newState = state.melt();
        final Set.Transient<ITermVar> _universalVars = Set.Transient.of();
        final Function1<Optional<ITermVar>, ITermVar> fresh = v -> {
            final ITermVar f;
            if(v.isPresent()) {
                f = newState.freshVar(v.get().getName());
            } else {
                f = newState.freshVar("_");
            }
            _universalVars.__insert(f);
            return f;
        };
        return P.matchWithEqs(rule.params(), args, state.unifier(), fresh).flatMap(matchResult -> {
            final Set.Immutable<ITermVar> universalVars = _universalVars.freeze();
            final SetView<ITermVar> constrainedVars = Sets.difference(matchResult.constrainedVars(), universalVars);
            final IConstraint newConstraint = rule.body().apply(matchResult.substitution()).withCause(cause);

            final ApplyResult applyResult;
            if(constrainedVars.isEmpty()) {
                applyResult = ApplyResult.of(newState.freeze(), ImmutableSet.of(), Optional.empty(), newConstraint);
            } else {
                // simplify guard constraints
                final IUniDisunifier.Result<IUnifier.Immutable> unifyResult;
                try {
                    if((unifyResult = state.unifier().unify(matchResult.equalities()).orElse(null)) == null) {
                        return Optional.empty();
                    }
                } catch(OccursException e) {
                    return Optional.empty();
                }
                final IUniDisunifier.Immutable newUnifier = unifyResult.unifier();
                final IUnifier.Immutable diff = unifyResult.result();

                // construct guard
                final IUnifier.Immutable guard = diff.retainAll(constrainedVars).unifier();
                if(guard.isEmpty()) {
                    throw new IllegalStateException("Guard not expected to be empty here.");
                }
                final Diseq diseq = Diseq.of(universalVars, guard);

                // construct result
                final IState.Immutable resultState = newState.freeze().withUnifier(newUnifier);
                applyResult = ApplyResult.of(resultState, diff.varSet(), Optional.of(diseq), newConstraint);
            }
            return Optional.of(applyResult);
        });
    }

    /**
     * Convert an ordered list of rules to a set of rules where the match order is reflected in (dis)equality
     * constraints in the rule bodies. The resulting rules can be applied independent of the other rules in the set.
     * Note that compared to using applyAll, mismatches may only be discovered when the body of the returned rules is
     * evaluated, instead of during the matching process already.
     * 
     * @param rules
     *            An ordered list of rules
     * 
     * @return A set of rules that are mutually independent
     */
    public static final java.util.Set<Rule> makeUnordered(List<Rule> rules) {
        final List<Pattern> guards = Lists.newArrayList();
        // go thorugh all rules in sequence
        return rules.stream().<Rule>flatMap(r -> {
            final IUniDisunifier.Transient diseqs = PersistentUniDisunifier.Immutable.of().melt();

            // eliminate wildcards in the patterns
            final FreshVars fresh = new FreshVars(r.varSet());
            final List<Pattern> paramPatterns = r.params().stream().map(p -> p.eliminateWld(() -> fresh.fresh("_")))
                    .collect(ImmutableList.toImmutableList());
            fresh.fix();
            final Pattern paramsPattern = P.newTuple(paramPatterns);

            // create term for params and add implied equalities
            final Tuple2<ITerm, List<Tuple2<ITermVar, ITerm>>> p_eqs = paramsPattern.asTerm(v -> v.get());
            try {
                if(!diseqs.unify(p_eqs._2()).isPresent()) {
                    return Stream.empty();
                }
            } catch(OccursException e) {
                return Stream.empty();
            }

            // add disunifications for all patterns from previous rules
            final boolean guardsOk = guards.stream().allMatch(g -> {
                final IRenaming swap = fresh.fresh(g.getVars());
                final Pattern g1 = g.eliminateWld(() -> fresh.fresh("_"));
                final Tuple2<ITerm, List<Tuple2<ITermVar, ITerm>>> t_eqs = g1.apply(swap).asTerm(v -> v.get());
                // add internal equalities from the guard pattern, which are also reasons why the guard wouldn't match
                final List<ITermVar> leftEqs =
                        t_eqs._2().stream().map(Tuple2::_1).collect(ImmutableList.toImmutableList());
                final List<ITerm> rightEqs =
                        t_eqs._2().stream().map(Tuple2::_2).collect(ImmutableList.toImmutableList());
                final ITerm left = B.newTuple(p_eqs._1(), B.newTuple(leftEqs));
                final ITerm right = B.newTuple(t_eqs._1(), B.newTuple(rightEqs));
                final java.util.Set<ITermVar> universals = fresh.reset();
                return diseqs.disunify(universals, left, right).isPresent();
            });
            if(!guardsOk) {
                return Stream.empty();
            }

            // add params as guard for next rule
            guards.add(paramsPattern);

            final IConstraint body = Constraints.conjoin(StateUtil.asInequalities(diseqs), r.body());
            return Stream.of(r.withParams(paramPatterns).withBody(body));
        }).collect(ImmutableSet.toImmutableSet());
    }

    /**
     * Inline rule into the i-th matching premise of the second rule. Inlining always succeeds (use simplify to solve
     * equalities in the resulting rule). The function returns empty if nothing was inlined because no i-th matching
     * premise existed.
     */
    public static final Optional<Rule> inline(Rule rule, int ith, Rule into) {
        final FreshVars fresh = new FreshVars(into.varSet());
        final IRenaming swap = fresh.fresh(rule.paramVars());

        final Pattern rulePatterns = P.newTuple(rule.params()).eliminateWld(() -> fresh.fresh("_"));
        final Tuple2<ITerm, List<Tuple2<ITermVar, ITerm>>> p_eqs = rulePatterns.asTerm(v -> v.get());
        final ITerm p = swap.apply(p_eqs._1());

        final AtomicInteger i = new AtomicInteger(0);
        final IConstraint newBody = Constraints.map(c -> {
            if(!(c instanceof CUser)) {
                return c;
            }
            final CUser constraint = (CUser) c;
            if(!constraint.name().equals(rule.name())) {
                return c;
            }
            if(i.getAndIncrement() != ith) {
                return c;
            }

            final ITerm t = B.newTuple(constraint.args());
            final CEqual eq = new CEqual(t, p);
            final Set.Immutable<ITermVar> newVars = fresh.reset();
            final IConstraint newConstraint = Constraints.exists(newVars, new CConj(eq, rule.body().apply(swap)));

            return newConstraint;
        }, false).apply(into.body());

        if(i.get() <= ith) {
            // nothing was inlined
            return Optional.empty();
        }

        return Optional.of(into.withBody(newBody));
    }

    public static Optional<Rule> simplify(Rule rule) {
        final List<IConstraint> constraints = Lists.newArrayList();
        final IUniDisunifier.Transient unifier = PersistentUniDisunifier.Immutable.of().melt();
        final FreshVars fresh = new FreshVars(rule.paramVars());
        final Deque<IConstraint> worklist = Lists.newLinkedList();
        worklist.push(rule.body());
        while(!worklist.isEmpty()) {
            final IConstraint constraint = worklist.removeLast();
            // @formatter:off
            final boolean okay = constraint.match(Constraints.<Boolean>cases(
                c -> { constraints.add(c); return true; },
                conj -> { Constraints.disjoin(conj).forEach(worklist::addLast); return true; },
                equal -> { try { return unifier.unify(equal.term1(), equal.term2()).isPresent(); } catch(OccursException e) { return false; } },
                exists -> {
                    final IRenaming renaming = fresh.fresh(exists.vars());
                    worklist.addLast(exists.constraint().apply(renaming));
                    return true;
                },
                c -> { constraints.add(c); return true; },
                inequal -> { return unifier.disunify(inequal.universals(), inequal.term1(), inequal.term2()).isPresent(); },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; }
            ));
            // @formatter:on
            if(!okay) {
                return Optional.empty();
            }
        }
        final Set.Immutable<ITermVar> newVars = fresh.reset();
        final IConstraint newBody = Constraints.exists(newVars,
                Constraints.conjoin(Iterables.concat(StateUtil.asConstraint(unifier), constraints)));
        // FIXME Add option to inline unifier in head patterns
        Rule newRule = Rule.builder().from(rule).body(newBody).build();
        return Optional.of(newRule);
    }

    public static final ListMultimap<String, Rule> makeFragments(Spec spec, Predicate1<String> includePredicate,
            Predicate2<String, String> includeRule, int generations) {
        final Multimap<String, Rule> newRules = ArrayListMultimap.create();
        for(int gen = 0; gen < generations; gen++) {
            final Multimap<String, Rule> genRules = ArrayListMultimap.create();
            for(String name : spec.rules().keySet()) {
                if(!includePredicate.test(name)) {
                    continue;
                }
                final List<Rule> predRules = spec.rules().get(name);

                // prepare arguments (assuming all rules have the same number of arguments)
                final Rule protoRule;
                if((protoRule = predRules.stream().findFirst().orElse(null)) == null) {
                    // skip if we have no rules
                    continue;
                }
                final IState.Transient _state = State.of(spec).melt();
                final List<ITermVar> args = protoRule.params().stream().map(p -> _state.freshVar("t"))
                        .collect(ImmutableList.toImmutableList());
                final IState.Immutable state = _state.freeze();

                // loop over all rules in order
                final List<Diseq> unguard = Lists.newArrayList();
                for(Rule rule : predRules) {

                    // apply the rule to the arguments
                    final ApplyResult applyResult;
                    if((applyResult = apply(state, rule, args, null).orElse(null)) == null) {
                        continue;
                    }

                    // add disequalities
                    final IUniDisunifier.Transient _applyUnifier = applyResult.state().unifier().melt();
                    for(Diseq diseq : unguard) {
                        final Tuple3<Set<ITermVar>, ITerm, ITerm> _diseq = diseq.toTuple();
                        if(!_applyUnifier.disunify(_diseq._1(), _diseq._2(), _diseq._3()).isPresent()) {
                            log.warn("Rule seems overlapping with previous rule. This shouldn't really happen.");
                            continue;
                        }
                    }
                    final IUniDisunifier.Immutable applyUnifier = _applyUnifier.freeze();
                    final IState.Immutable applyState = applyResult.state().withUnifier(applyUnifier);

                    if(rule.label().isEmpty() || includeRule.test(name, rule.label())) {

                        // normalize the rule by solving it
                        final SolverResult solverResult;
                        try {
                            if((solverResult = Solver.solve(spec, applyState, applyResult.body(),
                                    new NullDebugContext())) == null) {
                                continue;
                            }
                        } catch(InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        if(solverResult.hasErrors()) {
                            continue;
                        }
                        final Map<Boolean, List<IConstraint>> unsolved =
                                solverResult.delays().keySet().stream().collect(Collectors.partitioningBy(
                                        c -> c instanceof CUser && includePredicate.test(((CUser) c).name())));
                        if(unsolved.get(true).isEmpty() && gen > 0) {
                            // ignore base rules after first generation
                            continue;
                        }
                        final Ref<Stream<Tuple2<IState.Immutable, IConstraint>>> expansions = new Ref<>(Stream.of(
                                ImmutableTuple2.of(solverResult.state(), Constraints.conjoin(unsolved.get(false)))));
                        unsolved.get(true).forEach(_c -> {
                            final CUser c = (CUser) _c;
                            expansions.set(expansions.get().flatMap(st_uc -> {
                                final IState.Immutable st = st_uc._1();
                                final IConstraint uc = st_uc._2();

                                final List<Tuple2<IState.Immutable, IConstraint>> sts = Lists.newArrayList();
                                for(Rule r : newRules.get(c.name())) {
                                    ApplyResult ar;
                                    if((ar = apply(st, r, c.args(), null).orElse(null)) == null) {
                                        continue;
                                    }
                                    final SolverResult sr;
                                    try {
                                        if((sr = Solver.solve(spec, ar.state(),
                                                Constraints.conjoin(Iterables2.from(ar.body(), uc)),
                                                new NullDebugContext())) == null) {
                                            continue;
                                        }
                                    } catch(InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if(sr.hasErrors()) {
                                        continue;
                                    }
                                    sts.add(ImmutableTuple2.of(sr.state(), sr.delayed()));
                                    // unguards are reflected in the fragments already,
                                    // so it should not be necessary to do anything with
                                    // the guard of these expansions
                                }
                                return sts.stream();
                            }));
                        });

                        // expand
                        expansions.get().forEach(st_uc -> {
                            final IState.Immutable st = st_uc._1();

                            // body without equalities
                            final List<IConstraint> cs = Lists.newArrayList();
                            cs.add(st_uc._2());
                            cs.addAll(StateUtil.asConstraint(st.scopeGraph()));
                            cs.addAll(StateUtil.asConstraint(st.termProperties()));
                            final IConstraint body = Constraints.conjoin(cs);
                            final java.util.Set<ITermVar> bodyVars = Constraints.freeVars(body);

                            // build params
                            final List<Pattern> params = args.stream().map(a -> {
                                final ITerm t = st.unifier().findRecursive(a); // findRecursive for most instantiated match pattern
                                return P.fromTerm(t, v -> !bodyVars.contains(v));
                            }).collect(Collectors.toList());
                            final java.util.Set<ITermVar> paramVars =
                                    Stream.concat(args.stream(), params.stream().flatMap(p -> p.getVars().stream()))
                                            .collect(ImmutableSet.toImmutableSet());

                            // unifier without match and unused vars
                            final IUniDisunifier.Transient _unifier = st.unifier().melt();
                            _unifier.removeAll(paramVars);
                            _unifier.retainAll(bodyVars);
                            final IUniDisunifier.Immutable unifier = _unifier.freeze();

                            // build body
                            final IConstraint eqs = Constraints.conjoin(StateUtil.asConstraint(unifier));
                            final java.util.Set<ITermVar> vs =
                                    Sets.difference(Sets.union(bodyVars, unifier.freeVarSet()), paramVars);
                            final IConstraint c = new CExists(vs, new CConj(body, eqs));

                            // build rule
                            final Rule r = Rule.of(name, params, c);
                            genRules.put(name, r);
                        });

                    }

                    // update unguard for next rules
                    final Optional<Diseq> guard = applyResult.guard();
                    if(!guard.isPresent()) {
                        break;
                    } else {
                        unguard.add(guard.get());
                    }

                }
            }
            newRules.putAll(genRules);
        }
        return ImmutableListMultimap.copyOf(newRules);
    }

    public static final void freeVars(Rule rule, Action1<ITermVar> onVar) {
        final java.util.Set<ITermVar> paramVars = rule.paramVars();
        Constraints.freeVars(rule.body(), v -> {
            if(!paramVars.contains(v)) {
                onVar.apply(v);
            }
        });
    }

    public static final void vars(Rule rule, Action1<ITermVar> onVar) {
        Constraints.vars(rule.body(), onVar);
    }

}