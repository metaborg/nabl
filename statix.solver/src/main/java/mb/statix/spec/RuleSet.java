package mb.statix.spec;

import com.google.common.collect.*;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.terms.unification.ud.PersistentUniDisunifier;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.StateUtil;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.functions.Predicate2;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;


/**
 * An immutable set of rules.
 */
public final class RuleSet {

    /** The rules, ordered from most specific o least specific guard. */
    private final ImmutableListMultimap<String, Rule> rules;
    /** The independent rules. If a rule name is not in this map,
     *  an independent version of its rules has not yet been created. */
    private final Map<String, ImmutableList<Rule>> independentRules = new HashMap<>();

    /**
     * Makes a new ruleset from the specified collection of rules.
     *
     * This function will ensure the rules are correctly ordered from most specific to least specific guard.
     *
     * @param rules the rules to put into the ruleset
     * @return the resulting ruleset
     */
    public static RuleSet of(Collection<Rule> rules) {
        final ImmutableListMultimap.Builder<String, Rule> builder = ImmutableListMultimap.<String, Rule>builder()
                .orderValuesBy(Rule.leftRightPatternOrdering.asComparator());
        rules.forEach(rule -> builder.put(rule.name(), rule));
        return new RuleSet(builder.build());
    }

    /**
     * Initializes a new instance of the {@link RuleSet} class.
     *
     * @param rules the multimap of rule names to rules, ordered from most specific to least specific guard
     */
    public RuleSet(ListMultimap<String, Rule> rules) {
        this.rules = ImmutableListMultimap.copyOf(rules);
    }

    /**
     * Returns a map of rule names to a list of rules.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @return the map of rules
     */
    public ImmutableListMultimap<String, Rule> getRuleMap() { return this.rules; }

    /**
     * Gets the names of all the rules in the ruleset.
     *
     * @return the set of rule names
     */
    public ImmutableSet<String> getRuleNames() {
        return this.rules.keySet();
    }

    /**
     * Gets all rules in the ruleset.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @return all rules
     */
    public ImmutableCollection<Rule> getAllRules() {
        return this.rules.values();
    }

    /**
     * Gets the rules with the specified name in the ruleset.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @param name the name of the rules to find
     * @return the rules with the specified name
     */
    public ImmutableList<Rule> getRules(String name) {
        return this.rules.get(name);
    }

    /**
     * Gets a map of lists of rules, where the match order is reflected in (dis)equality constraints
     * in the rule bodies. The resulting rules can be applied independent of the other rules in the set.
     *
     * Note that compared to using applyAll, mismatches may only be discovered when the body of the returned rules is
     * evaluated, instead of during the matching process already.
     *
     * @return a map of a list of rules that are mutually independent
     */
    public ImmutableListMultimap<String, Rule> getAllIndependentRules() {
        final ImmutableListMultimap.Builder<String, Rule> independentRules = ImmutableListMultimap.builder();
        this.rules.keySet().forEach(name -> independentRules.putAll(name, getIndependentRules(name)));
        return independentRules.build();
    }

    /**
     * Gets a list of rules with the specified name, where the match order is reflected in (dis)equality constraints
     * in the rule bodies. The resulting rules can be applied independent of the other rules in the set.
     *
     * Note that compared to using applyAll, mismatches may only be discovered when the body of the returned rules is
     * evaluated, instead of during the matching process already.
     *
     * @param name the name of the rules to find
     * @return a list of rules that are mutually independent
     */
    public ImmutableList<Rule> getIndependentRules(String name) {
        return this.independentRules.computeIfAbsent(name, n -> computeIndependentRules(getRules(name)));
    }

    /**
     * Computes the independent rules.
     *
     * @param rules the set of rules for which to compute
     * @return the set of independent rules
     */
    private ImmutableList<Rule> computeIndependentRules(ImmutableList<Rule> rules) {
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
                if (!diseqs.unify(p_eqs._2()).isPresent()) {
                    return Stream.empty();
                }
            } catch (OccursException e) {
                return Stream.empty();
            }

            // Add disunifications for all patterns from previous rules
            final boolean guardsOk = guards.stream().allMatch(g -> {
                final IRenaming swap = fresh.fresh(g.getVars());
                final Pattern g1 = g.eliminateWld(() -> fresh.fresh("_"));
                final Tuple2<ITerm, List<Tuple2<ITermVar, ITerm>>> t_eqs = g1.apply(swap).asTerm(v -> v.get());
                // Add internal equalities from the guard pattern, which are also reasons why the guard wouldn't match
                final List<ITermVar> leftEqs =
                        t_eqs._2().stream().map(Tuple2::_1).collect(ImmutableList.toImmutableList());
                final List<ITerm> rightEqs =
                        t_eqs._2().stream().map(Tuple2::_2).collect(ImmutableList.toImmutableList());
                final ITerm left = B.newTuple(p_eqs._1(), B.newTuple(leftEqs));
                final ITerm right = B.newTuple(t_eqs._1(), B.newTuple(rightEqs));
                final Set<ITermVar> universals = fresh.reset();
                return diseqs.disunify(universals, left, right).isPresent();
            });
            if (!guardsOk) return Stream.empty();

            // Add params as guard for next rule
            guards.add(paramsPattern);

            final IConstraint body = Constraints.conjoin(StateUtil.asInequalities(diseqs), r.body());
            return Stream.of(r.withParams(paramPatterns).withBody(body));
        }).collect(ImmutableList.toImmutableList());
    }

    /**
     * Gets a multimap from names to rules that have equivalent patterns.
     *
     * @return the map from names to equivalent rules
     */
    public ListMultimap<String, Rule> getAllEquivalentRules() {
        final ImmutableListMultimap.Builder<String, Rule> overlappingRules = ImmutableListMultimap.builder();
        this.rules.keySet().forEach(name -> overlappingRules.putAll(name, getEquivalentRules(name)));
        return overlappingRules.build();
    }

    /**
     * Gets a set of rules with equivalent patterns.
     *
     * @param name the name of the rules to find
     * @return a set of rules with equivalent patterns
     */
    public ImmutableSet<Rule> getEquivalentRules(String name) {
        ImmutableList<Rule> rules = getRules(name);
        return rules.stream().filter(a -> rules.stream().anyMatch(b -> !a.equals(b) && ARule.leftRightPatternOrdering.compare(a, b).map(c -> c == 0).orElse(false)))
                .collect(ImmutableSet.toImmutableSet());
    }

}
