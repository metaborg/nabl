package mb.statix.spec;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;


public interface RuleSet {

    /**
     * Returns a map of rule names to a list of rules.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @return the map of rules
     */
    Multimap<String, Rule> getRuleMap();

    /**
     * Gets the names of all the rules in the ruleset.
     *
     * @return the set of rule names
     */
    Set<String> getRuleNames();

    /**
     * Gets all rules in the ruleset.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @return all rules
     */
    Collection<Rule> getAllRules();

    /**
     * Gets the rules with the specified name in the ruleset.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @param name
     *            the name of the rules to find
     * @return the rules with the specified name
     */
    Collection<Rule> getRules(String name);

    /**
     * Return a rule set where rules are independent of matching order.
     */
    UnorderedRuleSet getUnorderedRuleSet();

    /**
     * Gets a multimap from names to rules that have equivalent patterns.
     *
     * @return the map from names to equivalent rules
     */
    default SetMultimap<String, Rule> getAllEquivalentRules() {
        final ImmutableSetMultimap.Builder<String, Rule> overlappingRules = ImmutableSetMultimap.builder();
        getRuleNames().forEach(name -> overlappingRules.putAll(name, getEquivalentRules(name)));
        return overlappingRules.build();
    }

    /**
     * Gets a set of rules with equivalent patterns.
     *
     * @param name
     *            the name of the rules to find
     * @return a set of rules with equivalent patterns
     */
    default Set<Rule> getEquivalentRules(String name) {
        Collection<Rule> rules = getRules(name);
        return rules.stream().filter(a -> rules.stream().anyMatch(
                b -> !a.equals(b) && Rule.leftRightPatternOrdering.compare(a, b).map(c -> c == 0).orElse(false)))
                .collect(ImmutableSet.toImmutableSet());
    }

}