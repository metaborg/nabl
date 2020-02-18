package mb.statix.spec;

import java.io.Serializable;
import java.util.Collection;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ListMultimap;


/**
 * An immutable set of rules.
 */
public final class OrderedRuleSet implements RuleSet, Serializable {

    private static final long serialVersionUID = 1L;

    /** The rules, ordered from most specific o least specific guard. */
    private final ImmutableListMultimap<String, Rule> rules;

    /**
     * Makes a new ruleset from the specified collection of rules.
     *
     * This function will ensure the rules are correctly ordered from most specific to least specific guard.
     *
     * @param rules
     *            the rules to put into the ruleset
     * @return the resulting ruleset
     */
    public static OrderedRuleSet of(Collection<Rule> rules) {
        final ImmutableListMultimap.Builder<String, Rule> builder = ImmutableListMultimap.<String, Rule>builder()
                .orderValuesBy(Rule.leftRightPatternOrdering.asComparator());
        rules.forEach(rule -> builder.put(rule.name(), rule));
        return new OrderedRuleSet(builder.build());
    }

    /**
     * Initializes a new instance of the {@link OrderedRuleSet} class.
     *
     * @param rules
     *            the multimap of rule names to rules, ordered from most specific to least specific guard
     */
    public OrderedRuleSet(ListMultimap<String, Rule> rules) {
        this.rules = ImmutableListMultimap.copyOf(rules);
    }

    /**
     * Returns a map of rule names to a list of rules.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @return the map of rules
     */
    @Override public ImmutableListMultimap<String, Rule> getRuleMap() {
        return this.rules;
    }

    /**
     * Gets the names of all the rules in the ruleset.
     *
     * @return the set of rule names
     */
    @Override public ImmutableSet<String> getRuleNames() {
        return this.rules.keySet();
    }

    /**
     * Gets all rules in the ruleset.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @return all rules
     */
    @Override public ImmutableCollection<Rule> getAllRules() {
        return this.rules.values();
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
    @Override public ImmutableList<Rule> getRules(String name) {
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
    @Override public UnorderedRuleSet getUnorderedRuleSet() {
        final ImmutableSetMultimap.Builder<String, Rule> independentRules = ImmutableSetMultimap.builder();
        this.rules.keySet().forEach(
                name -> independentRules.putAll(name, RuleUtil.computeOrderIndependentRules(this.rules.get(name))));
        return new UnorderedRuleSet(independentRules.build());
    }

}