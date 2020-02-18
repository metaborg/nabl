package mb.statix.spec;

import java.io.Serializable;
import java.util.Collection;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;


/**
 * An immutable set of rules.
 */
public final class UnorderedRuleSet implements RuleSet, Serializable {

    private static final long serialVersionUID = 1L;

    /** The rules, ordered from most specific o least specific guard. */
    private final ImmutableSetMultimap<String, Rule> rules;

    /**
     * Makes a new ruleset from the specified collection of rules.
     *
     * This function will ensure the rules are correctly ordered from most specific to least specific guard.
     *
     * @param rules
     *            the rules to put into the ruleset
     * @return the resulting ruleset
     */
    public static UnorderedRuleSet of(Collection<Rule> rules) {
        final ImmutableSetMultimap.Builder<String, Rule> builder = ImmutableSetMultimap.<String, Rule>builder();
        rules.forEach(rule -> builder.put(rule.name(), rule));
        return new UnorderedRuleSet(builder.build());
    }

    /**
     * Initializes a new instance of the {@link UnorderedRuleSet} class.
     *
     * @param rules
     *            the multimap of rule names to rules, ordered from most specific to least specific guard
     */
    public UnorderedRuleSet(SetMultimap<String, Rule> rules) {
        this.rules = ImmutableSetMultimap.copyOf(rules);
    }

    /**
     * Returns a map of rule names to a list of rules.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @return the map of rules
     */
    @Override public ImmutableSetMultimap<String, Rule> getRuleMap() {
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
    @Override public ImmutableSet<Rule> getRules(String name) {
        return this.rules.get(name);
    }

    @Override public UnorderedRuleSet getUnorderedRuleSet() {
        return this;
    }

}