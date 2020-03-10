package mb.statix.spec;

import com.google.common.collect.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * An immutable set of rules.
 */
public final class RuleSet {

    /** The rules, ordered from most specific o least specific guard. */
    private final ImmutableListMultimap<String, Rule> rules;
    /** The independent rules. If a rule name is not in this map,
     *  an independent version of its rules has not yet been created. */
    private final Map<String, ImmutableSet<Rule>> independentRules = new HashMap<>();

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
    public ImmutableListMultimap<String, Rule> getAllOrderIndependentRules() {
        final ImmutableListMultimap.Builder<String, Rule> independentRules = ImmutableListMultimap.builder();
        this.rules.keySet().forEach(name -> independentRules.putAll(name, getOrderIndependentRules(name)));
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
     * @return a list of rules that are order independent
     */
    public ImmutableSet<Rule> getOrderIndependentRules(String name) {
        return this.independentRules.computeIfAbsent(name, n -> RuleUtil.computeOrderIndependentRules(getRules(name)));
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
