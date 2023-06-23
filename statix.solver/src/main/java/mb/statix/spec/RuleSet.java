package mb.statix.spec;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import io.usethesource.capsule.SetMultimap;
import mb.nabl2.terms.ITerm;
import mb.statix.solver.completeness.CompletenessUtil;


/**
 * An immutable set of rules.
 */
public final class RuleSet implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The rules, ordered from most specific to least specific guard. */
    private final Map.Immutable<String, SortedSet<Rule>> rules;
    /**
     * The independent rules. If a rule name is not in this map, an independent version of its rules has not yet been
     * created.
     */
    private final HashMap<String, Set.Immutable<Rule>> independentRules = new HashMap<>();

    /**
     * Makes a new ruleset from the specified collection of rules.
     *
     * This function will ensure the rules are correctly ordered from most specific to least specific guard.
     *
     * @param rules
     *            the rules to put into the ruleset
     * @return the resulting ruleset
     */
    public static RuleSet of(Collection<Rule> rules) {
        return new RuleSet(buildRuleSet(rules.stream()));
    }

    /**
     * Initializes a new instance of the {@link RuleSet} class.
     *
     * @param rules
     *            the multimap of rule names to rules, ordered from most specific to least specific guard
     */
    public RuleSet(Map.Immutable<String, SortedSet<Rule>> rules) {
        this.rules = rules;
    }

    /**
     * Returns a map of rule names to a list of rules.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @return the map of rules
     */
    public Map.Immutable<String, SortedSet<Rule>> getRuleMap() {
        return this.rules;
    }

    /**
     * Gets the names of all the rules in the ruleset.
     *
     * @return the set of rule names
     */
    public java.util.Set<String> getRuleNames() {
        return this.rules.keySet();
    }

    /**
     * Gets all rules in the ruleset.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @return all rules
     */
    public List<Rule> getAllRules() {
        return this.rules.values().stream().flatMap(SortedSet::stream).collect(Collectors.toList());
    }

    /**
     * Gets the rules with the specified name in the ruleset.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @param name
     *            the name of the rules to find
     * @return the rules with the specified name
     */
    public SortedSet<Rule> getRules(String name) {
        return this.rules.get(name);
    }

    /**
     * Gets a map of lists of rules, where the match order is reflected in (dis)equality constraints in the rule bodies.
     * The resulting rules can be applied independent of the other rules in the set.
     *
     * Note that compared to using applyAll, mismatches may only be discovered when the body of the returned rules is
     * evaluated, instead of during the matching process already.
     *
     * @return a map of a list of rules that are mutually independent
     */
    public java.util.Map<String, java.util.Set<Rule>> getAllOrderIndependentRules() {
        final HashMap<String, java.util.Set<Rule>> independentRules = new HashMap<>();
        this.rules.keySet().forEach(name -> {
            final Immutable<Rule> orderIndependentRules = getOrderIndependentRules(name);
            if(!orderIndependentRules.isEmpty()) {
                independentRules.put(name, orderIndependentRules);
            }
        });
        return independentRules;
    }

    /**
     * Gets a list of rules with the specified name, where the match order is reflected in (dis)equality constraints in
     * the rule bodies. The resulting rules can be applied independent of the other rules in the set.
     *
     * Note that compared to using applyAll, mismatches may only be discovered when the body of the returned rules is
     * evaluated, instead of during the matching process already.
     *
     * @param name
     *            the name of the rules to find
     * @return a list of rules that are order independent
     */
    public Set.Immutable<Rule> getOrderIndependentRules(String name) {
        return this.independentRules.computeIfAbsent(name, n -> RuleUtil.computeOrderIndependentRules(getRules(name)));
    }

    /**
     * Gets a multimap from names to rules that have equivalent patterns.
     *
     * @return the map from names to equivalent rules
     */
    public java.util.Map<String, java.util.Set<Rule>> getAllEquivalentRules() {
        final HashMap<String, java.util.Set<Rule>> overlappingRules = new HashMap<>();
        this.rules.keySet().forEach(name -> {
            final java.util.Set<Rule> equivalentRules = getEquivalentRules(name);
            if(!equivalentRules.isEmpty()) {
                overlappingRules.computeIfAbsent(name, k -> new HashSet<>())
                    .addAll(equivalentRules);
            }
        });
        return overlappingRules;
    }

    /**
     * Gets a set of rules with equivalent patterns.
     *
     * @param name
     *            the name of the rules to find
     * @return a set of rules with equivalent patterns
     */
    public java.util.Set<Rule> getEquivalentRules(String name) {
        SortedSet<Rule> rules = getRules(name);
        return rules.stream().filter(a -> rules.stream().anyMatch(
                b -> !a.equals(b) && ARule.LeftRightOrder.compare(a, b).map(c -> c == 0).orElse(false)))
                .collect(Collectors.toSet());
    }

    public RuleSet precomputeCriticalEdges(SetMultimap.Immutable<String, Tuple2<Integer, ITerm>> scopeExtensions) {
        return new RuleSet(buildRuleSet(
            rules.keySet().stream().flatMap(name -> rules.get(name).stream()).map(
                (Rule rule) -> CompletenessUtil.precomputeCriticalEdges(rule, scopeExtensions))));
    }

    private static Map.Immutable<String, SortedSet<Rule>> buildRuleSet(Stream<Rule> rules) {
        final HashMap<String, SortedSet<Rule>> builder = new HashMap<>();
        rules.forEach(rule -> {
            final SortedSet<Rule> value = builder.computeIfAbsent(rule.name(),
                k -> new TreeSet<>(Rule.leftRightOrderWithConsistentEquality.asComparator()));
            value.add(rule);
        });
        builder.replaceAll((key, value) -> Collections.unmodifiableSortedSet(value));
        return CapsuleUtil.toMap(builder);
    }

}
