package mb.statix.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.unification.Diseq;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.IUnifier.Immutable.Result;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CInequal;
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
                final Result<IUnifier.Immutable> unifyResult;
                try {
                    if((unifyResult = state.unifier().unify(matchResult.equalities()).orElse(null)) == null) {
                        return Optional.empty();
                    }
                } catch(OccursException e) {
                    return Optional.empty();
                }
                final IUnifier.Immutable newUnifier = unifyResult.unifier();
                final IUnifier.Immutable diff = unifyResult.result();

                // construct guard
                final Map<ITermVar, ITerm> guard = diff.retainAll(constrainedVars).unifier().equalityMap();
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

    public static final ListMultimap<String, Rule> makeFragments(Spec spec, java.util.Set<String> predicates,
            int generations) {
        final Ref<ListMultimap<String, Rule>> newRules = new Ref<>(ImmutableListMultimap.of());
        for(int gen = 0; gen < generations; gen++) {
            final Multimap<String, Rule> genRules = ArrayListMultimap.create();
            for(String name : predicates) {
                for(Rule rule : spec.rules().get(name)) {
                    final IState.Transient state = State.of(spec).melt();
                    final List<ITermVar> args = rule.params().stream().map(p -> state.freshVar("t"))
                            .collect(ImmutableList.toImmutableList());
                    final ApplyResult applyResult;
                    if((applyResult = apply(state.freeze(), rule, args, null).orElse(null)) == null) {
                        continue;
                    }
                    final SolverResult solverResult;
                    try {
                        if((solverResult = Solver.solve(applyResult.state(), applyResult.body(),
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
                                    c -> c instanceof CUser && predicates.contains(((CUser) c).name())));
                    if(unsolved.get(true).isEmpty() && gen > 0) {
                        // ignore base rules after first generation
                        continue;
                    }
                    final Ref<Stream<Tuple2<IState.Immutable, IConstraint>>> expansions = new Ref<>(Stream
                            .of(ImmutableTuple2.of(solverResult.state(), Constraints.conjoin(unsolved.get(false)))));
                    unsolved.get(true).forEach(_c -> {
                        final CUser c = (CUser) _c;
                        expansions.set(expansions.get().flatMap(st_uc -> {
                            final IState.Immutable st = st_uc._1();
                            final IConstraint uc = st_uc._2();

                            final List<Tuple2<IState.Immutable, IConstraint>> sts = Lists.newArrayList();
                            final Iterator<Rule> it = newRules.get().get(c.name()).iterator();
                            final List<IConstraint> unguards = Lists.newArrayList();
                            while(it.hasNext()) {
                                final Rule r = it.next();
                                ApplyResult ar;
                                if((ar = apply(st, r, c.args(), null).orElse(null)) == null) {
                                    continue;
                                }
                                final SolverResult sr;
                                try {
                                    final IConstraint unguard = Constraints.conjoin(unguards);
                                    if((sr = Solver.solve(ar.state(),
                                            Constraints.conjoin(Iterables2.from(ar.body(), uc, unguard)),
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
                                if(!ar.guard().isPresent()) {
                                    break;
                                }
                                ar.guard().get().toTuple().apply((us, left, right) -> {
                                    unguards.add(new CInequal(us, left, right));
                                    return null;
                                });
                            }
                            return sts.stream();
                        }));
                    });
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
                        final IUnifier.Transient _unifier = st.unifier().melt();
                        _unifier.removeAll(paramVars);
                        _unifier.retainAll(bodyVars);
                        final IUnifier.Immutable unifier = _unifier.freeze();

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
            }
            // rebuild to get ordered map again, the Builder does not allow use unfortunately
            final ImmutableListMultimap.Builder<String, Rule> newNewRules = ImmutableListMultimap
                    .<String, Rule>builder().orderValuesBy(Rule.leftRightPatternOrdering.asComparator());
            newNewRules.putAll(newRules.get());
            newNewRules.putAll(genRules);
            newRules.set(newNewRules.build());
        }
        return newRules.get();
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