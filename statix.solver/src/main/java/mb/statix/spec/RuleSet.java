package mb.statix.spec;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import jakarta.annotation.Nullable;
import mb.nabl2.terms.ITerm;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.tuple.Tuple2;

import java.util.Collection;
import java.util.List;

public interface RuleSet {

    /**
     * Returns a map of rule names to a list of rules.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @return the map of rules
     */
    Map.Immutable<String, ImList.Immutable<Rule>> getRuleMap();

    /**
     * Gets the names of all the rules in the ruleset.
     *
     * @return the set of rule names
     */
    java.util.Set<String> getRuleNames();

    /**
     * Gets all rules in the ruleset.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @return all rules
     */
    List<Rule> getAllRules();

    /**
     * Gets the rules with the specified name in the ruleset.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @param name
     *            the (constraint) name of the rules to find
     * @return the rules with the specified name
     */
    ImList.Immutable<Rule> getRules(String name);

    /**
     *
     * @param name
     *            the (rule) name of the rules to find
     * @return the rule with the specified name, or <code>null</code> if it does not exist.
     */
    @Nullable
    Rule getRule(RuleName name);

    /**
     * Gets the subset of rules that may be a sub-set of the constraints initiated by {@code rule}.
     * @param rule
     *            the name of the rule to get all sub-constraints for.
     * @return the subset of rules that may be a sub-set of {@code rule}
     */
    Map.Immutable<String, ImList.Immutable<Rule>> subConstraints(RuleName rule);

    /**
     * Gets the subset of rules that may use a rule for predicate {@code constraint} as sub-constraint.
     * @param constraint
     *                  the name of the constraints to get all causing rules for
     * @return the subset of rules that may be a cause of {@code constraint}
     */
    Collection<Rule> invokingRules(String constraint);

    /**
     * Gets a map of lists of rules, where the match order is reflected in (dis)equality constraints in the rule bodies.
     * The resulting rules can be applied independent of the other rules in the set.
     *
     * Note that compared to using applyAll, mismatches may only be discovered when the body of the returned rules is
     * evaluated, instead of during the matching process already.
     *
     * @return a map of a list of rules that are mutually independent
     */
    java.util.Map<String, java.util.Set<Rule>> getAllOrderIndependentRules();

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
    Set.Immutable<Rule> getOrderIndependentRules(String name);

    /**
     * Gets a list of rules with the specified name, where the match order is reflected in (dis)equality constraints in
     * the rule bodies. The resulting rules can be applied independent of the other rules in the set.
     *
     * Note that compared to using applyAll, mismatches may only be discovered when the body of the returned rules is
     * evaluated, instead of during the matching process already.
     *
     * @param label
     *            the label of the rule to find
     * @return the rule, including constraints implied byt specificity ordering
     */
    Rule getOrderIndependentRule(RuleName label);

    /**
     * Gets a multimap from names to rules that have equivalent patterns.
     *
     * @return the map from names to equivalent rules
     */
    java.util.Map<String, java.util.Set<Rule>> getAllEquivalentRules();

    /**
     * Gets a set of rules with equivalent patterns.
     *
     * @param name
     *            the name of the rules to find
     * @return a set of rules with equivalent patterns
     */
    java.util.Set<Rule> getEquivalentRules(String name);

    RuleSet precomputeCriticalEdges(SetMultimap.Immutable<String, Tuple2<Integer, ITerm>> scopeExtensions);
}
