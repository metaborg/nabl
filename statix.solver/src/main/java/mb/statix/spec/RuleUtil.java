package mb.statix.spec;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.substitution.FreshVars;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.u.PersistentUnifier;
import mb.nabl2.terms.unification.ud.Diseq;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.terms.unification.ud.PersistentUniDisunifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.StateUtil;

public class RuleUtil {

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
    public static Optional<Optional<Tuple2<Rule, ApplyResult>>> applyOrderedOne(IState.Immutable state,
            List<Rule> rules, List<? extends ITerm> args, @Nullable IConstraint cause) {
        return applyOrdered(state, rules, args, cause, true)
                .map(rs -> rs.stream().collect(MoreCollectors.toOptional()));
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
    public static List<Tuple2<Rule, ApplyResult>> applyOrderedAll(IState.Immutable state, List<Rule> rules,
            List<? extends ITerm> args, @Nullable IConstraint cause) {
        return applyOrdered(state, rules, args, cause, false).get();
    }

    /**
     * Helper method to apply the given list of ordered rules to the given arguments. Returns a list of results for all
     * rules that could be applied, or empty if onlyOne is true, and multiple matches were found.
     */
    private static Optional<List<Tuple2<Rule, ApplyResult>>> applyOrdered(IState.Immutable state, List<Rule> rules,
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
    public static Optional<ApplyResult> apply(IState.Immutable state, Rule rule, List<? extends ITerm> args,
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
            final IConstraint newConstraint = rule.body().substitute(matchResult.substitution()).withCause(cause);

            final ApplyResult applyResult;
            if(constrainedVars.isEmpty()) {
                applyResult = ApplyResult.of(newState.freeze(), PersistentUnifier.Immutable.of(), Optional.empty(),
                        newConstraint);
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
                applyResult = ApplyResult.of(resultState, diff, Optional.of(diseq), newConstraint);
            }
            return Optional.of(applyResult);
        });
    }

    /**
     * Apply the given rules to the given arguments. Returns the results of application.
     */
    public static List<Tuple2<Rule, ApplyResult>> applyAll(IState.Immutable state, Collection<Rule> rules,
            List<? extends ITerm> args, @Nullable IConstraint cause) {
        return rules.stream().flatMap(
                rule -> Streams.stream(apply(state, rule, args, cause)).map(result -> ImmutableTuple2.of(rule, result)))
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * Computes the order independent rules.
     *
     * @param rules
     *            the ordered set of rules for which to compute
     * @return the set of order independent rules
     */
    public static ImmutableSet<Rule> computeOrderIndependentRules(List<Rule> rules) {
        final List<Pattern> guards = Lists.newArrayList();

        return rules.stream().flatMap(r -> {
            final IUniDisunifier.Transient diseqs = PersistentUniDisunifier.Immutable.of().melt();

            // Eliminate wildcards in the patterns
            final FreshVars fresh = new FreshVars(r.varSet());
            final List<Pattern> paramPatterns = r.params().stream().map(p -> p.eliminateWld(() -> fresh.fresh("_")))
                    .collect(ImmutableList.toImmutableList());
            fresh.fix();
            final Pattern paramsPattern = P.newTuple(paramPatterns);

            // Create term for params and add implied equalities
            final Tuple2<ITerm, List<Tuple2<ITermVar, ITerm>>> p_eqs = paramsPattern.asTerm(Optional::get);
            try {
                if(!diseqs.unify(p_eqs._2()).isPresent()) {
                    return Stream.empty();
                }
            } catch(OccursException e) {
                return Stream.empty();
            }

            // Add disunifications for all patterns from previous rules
            final boolean guardsOk = guards.stream().allMatch(g -> {
                final IRenaming.Immutable swap = fresh.fresh(g.getVars());
                final Pattern g1 = g.eliminateWld(() -> fresh.fresh("_"));
                final Tuple2<ITerm, List<Tuple2<ITermVar, ITerm>>> t_eqs = g1.rename(swap).asTerm(Optional::get);
                // Add internal equalities from the guard pattern, which are also reasons why the guard wouldn't match
                final List<ITermVar> leftEqs =
                        t_eqs._2().stream().map(Tuple2::_1).collect(ImmutableList.toImmutableList());
                final List<ITerm> rightEqs =
                        t_eqs._2().stream().map(Tuple2::_2).collect(ImmutableList.toImmutableList());
                final ITerm left = B.newTuple(p_eqs._1(), B.newTuple(leftEqs));
                final ITerm right = B.newTuple(t_eqs._1(), B.newTuple(rightEqs));
                final java.util.Set<ITermVar> universals = fresh.reset();
                return diseqs.disunify(universals, left, right).isPresent();
            });
            if(!guardsOk)
                return Stream.empty();

            // Add params as guard for next rule
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
    public static Optional<Rule> inline(Rule rule, int ith, Rule into) {
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

            return applyToConstraint(rule, constraint.args());
        }, false).apply(into.body());

        if(i.get() <= ith) {
            // nothing was inlined
            return Optional.empty();
        }

        return Optional.of(into.withLabel("").withBody(newBody));
    }

    private static IConstraint applyToConstraint(Rule rule, List<? extends ITerm> args) {
        final java.util.Set<ITermVar> argVars =
                args.stream().flatMap(t -> t.getVars().stream()).collect(ImmutableSet.toImmutableSet());
        final FreshVars fresh = new FreshVars(argVars);
        final IRenaming.Immutable localRenaming = fresh.fresh(rule.paramVars());
        final Pattern rulePatterns = P.newTuple(rule.params()).eliminateWld(() -> fresh.fresh("_"));
        final Tuple2<ITerm, List<Tuple2<ITermVar, ITerm>>> p_eqs = rulePatterns.asTerm(v -> v.get());
        final ITerm p = localRenaming.apply(p_eqs._1());
        final ITerm t = B.newTuple(args);
        final CEqual eq = new CEqual(t, p);
        final Set.Immutable<ITermVar> newVars = fresh.reset();
        final IConstraint newBody = rule.body().substitute(localRenaming);
        final IConstraint newConstraint = Constraints.exists(newVars, new CConj(eq, newBody));
        return newConstraint;
    }

    /**
     * Simplify the rule by hoisting existentials to the top and solving (dis)equalities. Returns empty if the
     * (dis)equalities are inconsistent, otherwise return the simplified rule.
     */
    public static Optional<Rule> simplify(Rule rule) {
        final List<IConstraint> constraints = Lists.newArrayList();
        final IUniDisunifier.Transient unifier = PersistentUniDisunifier.Immutable.of().melt();
        final java.util.Set<ITermVar> paramVars = rule.paramVars();
        final FreshVars fresh = new FreshVars(paramVars);

        final List<Tuple2<ITerm, List<Tuple2<ITermVar, ITerm>>>> paramsAndEqs = rule.params().stream()
                .map(p -> p.asTerm(v -> v.orElse(fresh.fresh("_")))).collect(ImmutableList.toImmutableList());
        final List<ITerm> params = paramsAndEqs.stream().map(Tuple2::_1).collect(ImmutableList.toImmutableList());
        final List<IConstraint> eqs = paramsAndEqs.stream().map(Tuple2::_2)
                .map(es -> Constraints.conjoin(
                        es.stream().map(e -> new CEqual(e._1(), e._2())).collect(ImmutableList.toImmutableList())))
                .collect(ImmutableList.toImmutableList());
        final Set.Immutable<ITermVar> wildcards = fresh.reset();

        final Deque<IConstraint> worklist = Lists.newLinkedList();
        worklist.addAll(eqs);
        worklist.add(rule.body());
        while(!worklist.isEmpty()) {
            final IConstraint constraint = worklist.removeLast();
            // @formatter:off
            final boolean okay = constraint.match(Constraints.<Boolean>cases(
                c -> { constraints.add(c); return true; },
                conj -> { Constraints.disjoin(conj).forEach(worklist::addLast); return true; },
                equal -> { try { return unifier.unify(equal.term1(), equal.term2()).isPresent(); } catch(OccursException e) { return false; } },
                exists -> {
                    final IRenaming.Immutable renaming = fresh.fresh(exists.vars());
                    worklist.addLast(exists.constraint().substitute(renaming));
                    return true;
                },
                c -> { return false; },
                inequal -> { return unifier.disunify(inequal.universals(), inequal.term1(), inequal.term2()).isPresent(); },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; },
                c -> { return true; },
                c -> { constraints.add(c); return true; },
                c -> { constraints.add(c); return true; }
            ));
            // @formatter:on
            if(!okay) {
                return Optional.empty();
            }
        }
        final Set.Immutable<ITermVar> newBodyVars = fresh.reset();

        final List<Pattern> newParams =
                params.stream().map(p -> P.fromTerm(unifier.findRecursive(p), wildcards::contains))
                        .collect(ImmutableList.toImmutableList());
        final java.util.Set<ITermVar> newParamVars =
                newParams.stream().flatMap(p -> p.getVars().stream()).collect(ImmutableSet.toImmutableSet());

        final java.util.Set<ITermVar> elimParamVars = Sets.difference(paramVars, newParamVars).immutableCopy();
        final java.util.Set<ITermVar> introParamVars = Sets.difference(newParamVars, paramVars).immutableCopy();
        final java.util.Set<ITermVar> eVars =
                Sets.union(elimParamVars, Sets.difference(newBodyVars, introParamVars).immutableCopy());

        final IConstraint newBody = Constraints.exists(eVars,
                Constraints.conjoin(Iterables.concat(StateUtil.asConstraint(unifier), constraints)));

        Rule newRule = Rule.builder().from(rule).params(newParams).body(newBody).build();

        if(!Sets.symmetricDifference(rule.freeVars(), newRule.freeVars()).isEmpty()) {
            throw new AssertionError("Free variables changed when simplifying " + rule + " to " + newRule);
        }

        return Optional.of(newRule);
    }

    /**
     * Make closed fragments from the given rules by inlining into the given rules. The predicates includePredicate and
     * includeRule determine which premises should be inlined. The fragments are closed only w.r.t. the included
     * predicates.
     */
    // FIXME This generates many duplicate fragments, because a fragment from generation n is used for inlining in
    //       all generations n' > n. This would not happen if we only inline with fragments from the previous
    //       generation n'-1. However, this would not generate many fragments based on rules with multiple premises,
    //       where we would want to inline fragments from different generations.
    public static UnorderedRuleSet makeFragments(RuleSet rules, Predicate1<String> includePredicate,
            Predicate2<String, String> includeRule, int generations, BinaryOperator<java.util.Set<Rule>> combine) {
        final SetMultimap<String, Rule> fragments = HashMultimap.create();

        // 1. make all rules unordered, and keep included rules
        final SetMultimap<String, Rule> newRules = HashMultimap.create();

        for(Entry<String, Collection<Rule>> entry : rules.getUnorderedRuleSet().getRuleMap().asMap().entrySet()) {
            String ruleName = entry.getKey();
            if(includePredicate.test(ruleName)) {
                for(Rule r : entry.getValue()) {
                    if(includeRule.test(r.name(), r.label())) {
                        newRules.put(ruleName, r);
                    }
                }
            }
        }

        final PartialFunction1<IConstraint, CUser> expandable =
                c -> (c instanceof CUser && newRules.containsKey(((CUser) c).name())) ? Optional.of(((CUser) c))
                        : Optional.empty();

        // 2. find the included axioms and move to fragments
        for(Map.Entry<String, Rule> e : newRules.entries()) {
            if(Constraints.collectBase(expandable, false).apply(e.getValue().body()).isEmpty()) {
                fragments.put(e.getKey(), e.getValue());
            }
        }
        fragments.forEach(newRules::remove);

        // 3. for each generation, inline fragments into rules
        for(int g = 0; g < generations; g++) {
            final SetMultimap<String, Rule> generation = HashMultimap.create();
            for(Map.Entry<String, Rule> e : newRules.entries()) {
                final String name = e.getKey();
                final Rule r = e.getValue();
                final List<IConstraint> cs = Constraints.flatMap(c -> {
                    final Optional<CUser> u = expandable.apply(c);
                    if(u.isPresent()) {
                        return fragments.get(u.get().name()).stream().map(f -> applyToConstraint(f, u.get().args()));
                    } else {
                        return Stream.of(c);
                    }
                }, false).apply(r.body()).collect(Collectors.toList());
                for(IConstraint c : cs) {
                    final Rule f = r.withLabel("").withBody(c);
                    final Optional<Rule> sf = simplify(f);
                    if(sf.isPresent()) {
                        generation.put(name, sf.get());
                    }
                }
            }

            for(String name : generation.keySet()) {
                final java.util.Set<Rule> oldFragments = fragments.removeAll(name);
                final java.util.Set<Rule> newFragments = combine.apply(oldFragments, generation.get(name));
                fragments.putAll(name, newFragments);
            }
        }

        return new UnorderedRuleSet(fragments);
    }

    public static void vars(Rule rule, Action1<ITermVar> onVar) {
        Constraints.vars(rule.body(), onVar);
    }

}