package mb.statix.spec;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.tuple.Tuple3;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.substitution.FreshVars;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.Diseq;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.terms.unification.ud.PersistentUniDisunifier;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.StateUtil;
import mb.statix.spec.ApplyMode.Safety;

public final class RuleUtil {
    private RuleUtil() { /* This class cannot be instantiated. */ }

    /**
     * Apply the given list of rules to the given arguments. Returns the result of application if one rule can be
     * applied, empty of empty of no rules apply, or empty if more rules applied. Rules are expected to be in matching
     * order, with the first rule that can be applied is selected if the match is unconditional, or it is the only rule
     * that can be applied.
     *
     * @param state        the initial state
     * @param rules        an ordered list of rules to apply
     * @param args         the arguments to apply the rules to
     * @param cause        the cause of this rule application; or {@code null} if none
     * @param trackOrigins whether to track the syntactic origin of the constraints, if not already tracked
     * @return none if no rules apply, or the first application if multiple rules apply. The third component of the
     * tuple is true if this is the only match.
     */
    public static <E extends Throwable> Optional<Tuple3<Rule, ApplyResult, Boolean>> applyOrderedOne(
            IUniDisunifier.Immutable state,
            ImList.Immutable<Rule> rules,
            List<? extends ITerm> args,
            @Nullable IConstraint cause,
            ApplyMode<E> mode,
            Safety safety,
            boolean trackOrigins
    ) throws E {
        final List<Tuple2<Rule, ApplyResult>> results = applyOrdered(state, rules, args, cause, mode, safety, true, trackOrigins);
        if (results.size() == 0) {
            return Optional.empty();
        } else {
            final Tuple2<Rule, ApplyResult> result = results.get(0);
            return Optional.of(Tuple3.of(result._1(), result._2(), results.size() == 1));
        }
    }

    /**
     * Apply the given list of rules to the given arguments. Returns application results for rules can be applied. Rules
     * are expected to be in matching order, with rules being selected up to and including the first rule that can be
     * applied unconditional.
     *
     * @param state        the initial state
     * @param rules        an ordered list of rules to apply
     * @param args         the arguments to apply the rules to
     * @param cause        the cause of this rule application; or {@code null} if none
     * @param trackOrigins whether to track the syntactic origin of the constraints, if not already tracked
     * @return a list of apply results, up to and including the first unconditionally matching result.
     */
    public static <E extends Throwable> List<Tuple2<Rule, ApplyResult>> applyOrderedAll(
            IUniDisunifier.Immutable state,
            ImList.Immutable<Rule> rules,
            List<? extends ITerm> args,
            @Nullable IConstraint cause,
            ApplyMode<E> mode,
            Safety safety,
            boolean trackOrigins
    ) throws E {
        return applyOrdered(state, rules, args, cause, mode, safety, false, trackOrigins);
    }

    /**
     * Helper method to apply the given list of ordered rules to the given arguments. Returns a list of results for all
     * rules that could be applied. If onlyOne is true, returns at most two results.
     */
    private static <E extends Throwable> List<Tuple2<Rule, ApplyResult>> applyOrdered(
            IUniDisunifier.Immutable unifier,
            ImList.Immutable<Rule> rules,
            List<? extends ITerm> args,
            @Nullable IConstraint cause,
            ApplyMode<E> mode,
            Safety safety,
            boolean onlyOne,
            boolean trackOrigins
    ) throws E {
        final ImList.Mutable<Tuple2<Rule, ApplyResult>> results = ImList.Mutable.of();
        final AtomicBoolean foundOne = new AtomicBoolean(false);
        for (Rule rule : rules) {
            // apply rule
            final ApplyResult applyResult;
            if ((applyResult = apply(unifier, rule, args, cause, mode, safety, trackOrigins).orElse(null)) == null) {
                // this rule does not apply, continue to next rules
                continue;
            }
            results.add(Tuple2.of(rule, applyResult));
            if (onlyOne && foundOne.getAndSet(true)) {
                // we require exactly one, but found multiple
                break;
            }

            // stop or add guard to state for next rule
            final Tuple3<Set<ITermVar>, ITerm, ITerm> guard;
            if ((guard = applyResult.guard().map(Diseq::toTuple).orElse(null)) == null) {
                // next rules are unreachable after this unconditional match
                break;
            }
            final Optional<IUniDisunifier.Immutable> newUnifier =
                    unifier.disunify(guard._1(), guard._2(), guard._3()).map(IUniDisunifier.Result::unifier);
            if (!newUnifier.isPresent()) {
                // guards are equalities missing in the unifier, disunifying them should never fail
                throw new IllegalStateException("Unexpected incompatible guard.");
            }
            unifier = newUnifier.get();
        }
        return results.freeze();
    }

    // TODO: Remove this overload
    public static <E extends Throwable> Optional<ApplyResult> apply(
            IUniDisunifier.Immutable unifier,
            Rule rule,
            List<? extends ITerm> args,
            @Nullable IConstraint cause,
            ApplyMode<E> mode,
            Safety safety
    ) throws E {
        return apply(unifier, rule, args, cause, mode, safety, false);
    }

    /**
     * Apply the given rule to the given arguments. Returns the result of application, or nothing of the rule cannot be
     * applied. The result may contain equalities that need to be satisfied for the application to be valid.
     */
    public static <E extends Throwable> Optional<ApplyResult> apply(
            IUniDisunifier.Immutable unifier,
            Rule rule,
            List<? extends ITerm> args,
            @Nullable IConstraint cause,
            ApplyMode<E> mode,
            Safety safety,
            boolean trackOrigins
    ) throws E {
        return mode.apply(unifier, rule, args, cause, safety, trackOrigins);
    }

    /**
     * Apply the given rules to the given arguments. Returns the results of application.
     */
    public static <E extends Throwable> List<Tuple2<Rule, ApplyResult>> applyAll(
            IUniDisunifier.Immutable state,
            Collection<Rule> rules,
            List<? extends ITerm> args,
            @Nullable IConstraint cause,
            ApplyMode<E> mode,
            Safety safety,
            boolean trackOrigins
    ) throws E {
        final ImList.Mutable<Tuple2<Rule, ApplyResult>> results = ImList.Mutable.of();
        for (Rule rule : rules) {
            final ApplyResult result;
            if ((result = apply(state, rule, args, cause, mode, safety, trackOrigins).orElse(null)) != null) {
                results.add(Tuple2.of(rule, result));
            }
        }
        return results.freeze();
    }

    /**
     * Computes the order independent rules.
     *
     * @param rules the ordered set of rules for which to compute
     * @return the set of order independent rules
     */
    public static Set.Immutable<Rule> computeOrderIndependentRules(ImList.Immutable<Rule> rules) {
        final Set.Transient<Rule> newRules = CapsuleUtil.transientSet();
        final List<Tuple3<Set.Immutable<ITermVar>, ITerm, IUnifier.Immutable>> guards = new ArrayList<>();
        RULE:
        for (Rule rule : rules) {
            final Set.Immutable<ITermVar> ruleParamVars = rule.paramVars();
            final FreshVars fresh = new FreshVars(rule.freeVars(), ruleParamVars);

            final List<ITerm> paramTerms = new ArrayList<>();
            final IUniDisunifier.Transient _paramsUnifier = PersistentUniDisunifier.Immutable.of().melt();
            for (Pattern param : rule.params()) {
                final Tuple2<ITerm, List<Tuple2<ITermVar, ITerm>>> paramTerm =
                        param.asTerm(v -> v.orElseGet(() -> fresh.fresh("_")));
                paramTerms.add(paramTerm._1());
                try {
                    if (!_paramsUnifier.unify(paramTerm._2()).isPresent()) {
                        continue RULE; // skip, unmatchable pattern
                    }
                } catch (OccursException ex) {
                    continue RULE; // skip, unmatchable pattern
                }
            }
            final Set.Immutable<ITermVar> paramVars = fresh.fix().__insertAll(ruleParamVars);
            final ITerm paramsTerm = B.newTuple(paramTerms);
            final IUniDisunifier.Immutable paramsUnifier = _paramsUnifier.freeze();

            final IUniDisunifier.Transient _unifier = paramsUnifier.melt();
            GUARD:
            for (Tuple3<Set.Immutable<ITermVar>, ITerm, IUnifier.Immutable> guard : guards) {
                final IRenaming guardRen = fresh.fresh(guard._1());
                final Set.Immutable<ITermVar> guardVars = fresh.reset();
                final ITerm guardTerm = guardRen.apply(guard._2());
                IUnifier.Immutable guardUnifier = guard._3().rename(guardRen);
                try {
                    if ((guardUnifier =
                            guardUnifier.unify(paramsTerm, guardTerm).map(r -> r.unifier()).orElse(null)) == null) {
                        continue GUARD; // skip, guard already satisfied
                    }
                } catch (OccursException ex) {
                    continue GUARD; // skip, guard already satisfied
                }
                if (!_unifier.disunify(guardVars, guardUnifier).isPresent()) {
                    continue RULE; // skip, incompatible patterns & guards
                }
            }
            final IUniDisunifier.Immutable unifier = _unifier.freeze();

            final Tuple3<Set.Immutable<ITermVar>, ITerm, IUnifier.Immutable> guard =
                    Tuple3.of(paramVars, paramsTerm, paramsUnifier);
            guards.add(guard);

            final ImList.Immutable<Pattern> params = paramTerms.stream().map(P::fromTerm).collect(ImList.Immutable.toImmutableList());


            // we initialized FreshVars to make sure these do not capture any free variables,
            // or shadow any pattern variables. We can therefore use the original body without any
            // renaming
            final Set.Immutable<ITermVar> newBodyVars = paramVars.__removeAll(paramsTerm.getVars());
            final IConstraint body =
                    Constraints.exists(newBodyVars, Constraints.conjoin(StateUtil.asConstraint(unifier), rule.body()));

            final Rule newRule = Rule.builder()
                    .from(rule)
                    .params(params)
                    .body(body)
                    .bodyCriticalEdges(rule.bodyCriticalEdges())
                    .build();

            newRules.__insert(newRule);
        }
        return newRules.freeze();
    }

    /**
     * Inline rule into the i-th matching premise of the second rule. Inlining always succeeds (use simplify to solve
     * equalities in the resulting rule). The function returns empty if nothing was inlined because no i-th matching
     * premise existed.
     */
    public static Optional<Rule> inline(Rule rule, int ith, Rule into) {
        final FreshVars fresh = new FreshVars(vars(into));

        final AtomicInteger i = new AtomicInteger(0);
        final IConstraint newBody = Constraints.map(c -> {
            if (!(c instanceof CUser)) {
                return c;
            }
            final CUser constraint = (CUser)c;
            if (!constraint.name().equals(rule.name())) {
                return c;
            }
            if (i.getAndIncrement() != ith) {
                return c;
            }

            return applyToConstraint(fresh, rule, constraint.args());
        }, false).apply(into.body());

        if (i.get() <= ith) {
            // nothing was inlined
            return Optional.empty();
        }

        return Optional.of(into.withBody(newBody));
    }

    private static IConstraint applyToConstraint(FreshVars fresh, Rule rule, List<? extends ITerm> args) {
        final IRenaming swap = fresh.fresh(rule.paramVars());
        final Pattern rulePatterns = P.newTuple(rule.params()).eliminateWld(() -> fresh.fresh("_"));
        final Tuple2<ITerm, List<Tuple2<ITermVar, ITerm>>> p_eqs = rulePatterns.asTerm(v -> v.get());
        final ITerm p = swap.apply(p_eqs._1());
        final ITerm t = B.newTuple(args);
        final CEqual eq = new CEqual(t, p);
        final Set.Immutable<ITermVar> newVars = fresh.reset();
        final IConstraint newConstraint = Constraints.exists(newVars, new CConj(eq, rule.body().apply(swap)));
        return newConstraint;
    }

    /**
     * Transform rule such that constraints and have a single top-level existential.
     * <p>
     * Head patterns are preserved.
     * <p>
     * For example:
     *
     * <pre>
     *   { x :- {y} {y} x == Id(y) }  --->  { x :- {y y1} x == Id(y1) }
     * </pre>
     */
    public static Rule hoist(Rule rule) {
        final PreSolvedConstraint preSolvedBody = PreSolvedConstraint.of(rule.body()).cleanup();
        return rule.withBody(preSolvedBody.toConstraint());
    }

    /**
     * Transform rule such that head patterns are maximally instantiated based on the body. This implicitly applies
     * hoisting.
     * <p>
     * Head patterns are not preserved, but may only become more specific.
     */
    public static Rule instantiateHeadPatterns(Rule rule) {
        final Set.Immutable<ITermVar> paramVars = rule.paramVars();
        final FreshVars fresh = new FreshVars(rule.freeVars(), paramVars);

        final List<ITerm> paramTerms = new ArrayList<>();
        final IUniDisunifier.Transient _paramsUnifier = PersistentUniDisunifier.Immutable.of().melt();
        for (Pattern param : rule.params()) {
            final Tuple2<ITerm, List<Tuple2<ITermVar, ITerm>>> paramTerm =
                    param.asTerm(v -> v.orElseGet(() -> fresh.fresh("_")));
            paramTerms.add(paramTerm._1());
            try {
                if (!_paramsUnifier.unify(paramTerm._2()).isPresent()) {
                    return rule; // skip, unmatchable pattern
                }
            } catch (OccursException ex) {
                return rule; // skip, unmatchable pattern
            }
        }
        fresh.fix();
        final IUniDisunifier.Immutable paramsUnifier = _paramsUnifier.freeze();

        final PreSolvedConstraint body = PreSolvedConstraint.of(rule.body());
        final PreSolvedConstraint internedBody = body.intern(CapsuleUtil.immutableSet(), paramsUnifier);

        final Tuple2<ISubstitution.Immutable, PreSolvedConstraint> externResult = internedBody.extern(paramVars);
        final PreSolvedConstraint externedBody = externResult._2();
        final PreSolvedConstraint finalBody = externedBody.cleanup();

        final List<ITerm> newParamTerms = new ArrayList<>();
        final MultiSet.Transient<ITermVar> newParamVars = MultiSet.Transient.of();
        for (ITerm paramTerm : paramTerms) {
            final ITerm newParamTerm = externResult._1().apply(paramTerm);
            newParamTerms.add(newParamTerm);
            newParamTerm.visitVars(newParamVars::add);
        }

        final ImList.Immutable<Pattern> params = newParamTerms.stream()
                .map(t -> P.fromTerm(t, v -> !finalBody.freeVars().contains(v) && newParamVars.count(v) <= 1))
                .collect(ImList.Immutable.toImmutableList());

        return Rule.builder().from(rule).params(params).body(finalBody.toConstraint()).build();
    }


    /**
     * Close the rule by inlining free variables into the rule, taking their values from the given unifier.
     */
    public static Rule closeInUnifier(Rule rule, IUnifier.Immutable unifier, Safety safety) {
        ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of();
        for (ITermVar var : rule.freeVars()) {
            subst = subst.put(var, unifier.findRecursive(var));
        }
        Rule newRule;
        if (safety.equals(Safety.UNSAFE)) {
            newRule = rule.unsafeApply(subst);
        } else {
            newRule = rule.apply(subst);
        }
        return hoist(newRule);
    }


    /**
     * Make closed fragments from the given rules by inlining into the given rules. The predicates includePredicate and
     * includeRule determine which premises should be inlined. The fragments are closed only w.r.t. the included
     * predicates.
     */
    public static SetMultimap.Immutable<String, Rule> makeFragments(RuleSet rules, Predicate1<String> includePredicate,
            Predicate2<String, RuleName> includeRule, int generations) {
        final SetMultimap.Transient<String, Rule> fragments = SetMultimap.Transient.of();

        // 1. make all rules unordered, and keep included rules
        final SetMultimap.Transient<String, Rule> newRules = SetMultimap.Transient.of();
        for (String ruleName : rules.getRuleNames()) {
            if (includePredicate.test(ruleName)) {
                for (Rule r : rules.getOrderIndependentRules(ruleName)) {
                    if (includeRule.test(r.name(), r.label())) {
                        newRules.__insert(ruleName, r);
                    }
                }
            }
        }

        final PartialFunction1<IConstraint, CUser> expandable =
                c -> (c instanceof CUser && newRules.containsKey(((CUser)c).name())) ? Optional.of(((CUser)c))
                        : Optional.empty();

        // 2. find the included axioms and move to fragments
        for (Map.Entry<String, Rule> e : newRules.entrySet()) {
            if (Constraints.collectBase(expandable, false).apply(e.getValue().body()).isEmpty()) {
                fragments.__insert(e.getKey(), e.getValue());
            }
        }
        fragments.entrySet().forEach(e -> newRules.__remove(e.getKey(), e.getValue()));

        // 3. for each generation, inline fragments into rules
        for (int g = 0; g < generations; g++) {
            final SetMultimap.Transient<String, Rule> generation = SetMultimap.Transient.of();
            for (Map.Entry<String, Rule> e : newRules.entrySet()) {
                final String name = e.getKey();
                final Rule r = e.getValue();
                final FreshVars fresh = new FreshVars(vars(r));
                final List<IConstraint> cs = Constraints.flatMap(c -> {
                    final Optional<CUser> u = expandable.apply(c);
                    if (u.isPresent()) {
                        return fragments.get(u.get().name()).stream()
                                .map(f -> applyToConstraint(fresh, f, u.get().args()));
                    } else {
                        return Stream.of(c);
                    }
                }, false).apply(r.body()).collect(Collectors.toList());
                for(IConstraint c : cs) {
                    final Rule f = r.withLabel(RuleName.empty()).withBody(new CExists(CapsuleUtil.immutableSet(), c));
                    generation.__insert(name, hoist(f));
                }
            }
            CapsuleUtil.putAll(fragments, generation);
        }

        return fragments.freeze();
    }


    public static Set.Immutable<ITermVar> vars(Rule rule) {
        final Set.Transient<ITermVar> vars = CapsuleUtil.transientSet();
        vars(rule, vars::__insert);
        return vars.freeze();
    }

    public static void vars(Rule rule, Action1<ITermVar> onVar) {
        rule.paramVars().forEach(onVar::apply);
        Constraints.vars(rule.body(), onVar);
    }

}
