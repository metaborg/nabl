package mb.statix.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
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
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.Diseq;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.statix.constraints.CConj;
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
                final Diseq diseq = new Diseq(universalVars, guard);

                // construct result
                final IState.Immutable resultState = newState.freeze().withUnifier(newUnifier);
                applyResult = ApplyResult.of(resultState, diff.varSet(), Optional.of(diseq), newConstraint);
            }
            return Optional.of(applyResult);
        });
    }

    public static final Optional<Tuple2<Rule, ApplyResult>> applyOne(IState.Immutable state, Iterable<Rule> rules,
            List<? extends ITerm> args, @Nullable IConstraint cause) {
        return apply(state, rules, args, cause, true).stream().collect(MoreCollectors.toOptional());
    }

    public static final List<Tuple2<Rule, ApplyResult>> applyAll(IState.Immutable state, Iterable<Rule> rules,
            List<? extends ITerm> args, @Nullable IConstraint cause) {
        return apply(state, rules, args, cause, false);
    }

    private static final List<Tuple2<Rule, ApplyResult>> apply(IState.Immutable state, Iterable<Rule> rules,
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
                return ImmutableList.of();
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
        return results.build();

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

}