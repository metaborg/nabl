package mb.statix.spec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
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

    private static final ILogger logger = LoggerUtils.logger(RuleSet.class);

    private static final long serialVersionUID = 1L;

    /** The rules, ordered from most specific to least specific guard. */
    private final Map.Immutable<String, ImList.Immutable<Rule>> rules;

    /** The rules, indexed by name. */
    private final Map.Immutable<RuleName, Rule> rulesByLabel;

    /** Graph of rules using other predicates as sub-constraints. */
    private final SubConstraintGraph subConstraintGraph;

    /**
     * The independent rules. If a rule name is not in this map, an independent version of its rules has not yet been
     * created.
     */
    private final HashMap<String, Set.Immutable<Rule>> independentRules = new HashMap<>();

    /**
     * Sub-constraints. If a rule name is not in this map, an independent version of its rules has not yet been created.
     */
    private final HashMap<RuleName, Map.Immutable<String, ImList.Immutable<Rule>>> subConstraints = new HashMap<>();

    /**
     * Invoking rules. If a rule name is not in this map, an independent version of its rules has not yet been created.
     */
    private final HashMap<String, Collection<Rule>> invokingRules = new HashMap<>();

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
    public RuleSet(Map.Immutable<String, ImList.Immutable<Rule>> rules) {
        this(rules, buildLabelMap(rules));
    }

    private RuleSet(Map.Immutable<String, ImList.Immutable<Rule>> rules, Map.Immutable<RuleName, Rule> rulesByLabel) {
        this(rules, rulesByLabel, SubConstraintGraph.of(rules));
    }

    private RuleSet(Map.Immutable<String, ImList.Immutable<Rule>> rules, SubConstraintGraph subConstraintGraph) {
        this(rules, buildLabelMap(rules), subConstraintGraph);
    }

    private RuleSet(Map.Immutable<String, ImList.Immutable<Rule>> rules, Map.Immutable<RuleName, Rule> rulesByLabel,
            SubConstraintGraph subConstraintGraph) {
        this.rules = rules;
        this.rulesByLabel = rulesByLabel;
        this.subConstraintGraph = subConstraintGraph;
    }

    /**
     * Returns a map of rule names to a list of rules.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @return the map of rules
     */
    public Map.Immutable<String, ImList.Immutable<Rule>> getRuleMap() {
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
        return this.rules.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    /**
     * Gets the rules with the specified name in the ruleset.
     *
     * The rules are returned in order from most specific to least specific guard.
     *
     * @param name
     *            the (constraint) name of the rules to find
     * @return the rules with the specified name
     */
    public ImList.Immutable<Rule> getRules(String name) {
        return this.rules.get(name);
    }

    /**
     *
     * @param name
     *            the (rule) name of the rules to find
     * @return the rule with the specified name, or <code>null</code> if it does not exist.
     */
    public @Nullable Rule getRule(RuleName name) {
        return rulesByLabel.get(name);
    }

    /**
     * Gets the subset of rules that may be a sub-set of the constraints initiated by {@code rule}.
     * @param rule
     *            the name of the rule to get all sub-constraints for.
     * @return the subset of rules that may be a sub-set of {@code rule}
     */
    public Map.Immutable<String, ImList.Immutable<Rule>> subConstraints(RuleName rule) {
        return subConstraints.computeIfAbsent(rule, _rule -> {
            final Set.Immutable<String> subConstraintNames = subConstraintGraph.subConstraints(_rule);
            final Map.Transient<String, ImList.Immutable<Rule>> _rules = rules.asTransient();
            CapsuleUtil.filter(_rules, subConstraintNames::contains);
            return _rules.freeze();
        });
    }


    /**
     * Gets the subset of rules that may use a rule for predicate {@code constraint} as sub-constraint.
     * @param constraint
     *                  the name of the constraints to get all causing rules for
     * @return the subset of rules that may be a cause of {@code constraint}
     */
    public Collection<Rule> invokingRules(String constraint) {
        return invokingRules.computeIfAbsent(constraint, _constraint -> {
            final Set.Immutable<RuleName> superRuleNames = subConstraintGraph.invokingRules(_constraint);
            final Map.Transient<RuleName, Rule> _rules = rulesByLabel.asTransient();
            CapsuleUtil.filter(_rules, superRuleNames::contains);
            return _rules.freeze().values();
        });
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
                overlappingRules.computeIfAbsent(name, k -> new HashSet<>()).addAll(equivalentRules);
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
        ImList.Immutable<Rule> rules = getRules(name);
        return rules.stream()
                .filter(a -> rules.stream().anyMatch(
                        b -> !a.equals(b) && ARule.LeftRightOrder.compare(a, b).map(c -> c == 0).orElse(false)))
                .collect(Collectors.toSet());
    }

    public RuleSet precomputeCriticalEdges(SetMultimap.Immutable<String, Tuple2<Integer, ITerm>> scopeExtensions) {
        return new RuleSet(
                buildRuleSet(rules.keySet().stream().flatMap(name -> rules.get(name).stream())
                        .map((Rule rule) -> CompletenessUtil.precomputeCriticalEdges(rule, scopeExtensions))),
                subConstraintGraph);
    }

    private static Map.Immutable<String, ImList.Immutable<Rule>> buildRuleSet(Stream<Rule> rules) {
        final HashMap<String, List<Rule>> builder = new HashMap<>();
        rules.forEach(rule -> {
            final List<Rule> value = builder.computeIfAbsent(rule.name(), k -> new ArrayList<>());
            value.add(rule);
        });
        return CapsuleUtil.toMap(builder,
                value -> ImList.Immutable.sortedCopyOf(value, ARule.LeftRightOrder.asComparator()));
    }

    private static Map.Immutable<RuleName, Rule> buildLabelMap(Map.Immutable<String, ImList.Immutable<Rule>> rules) {
        final Map.Transient<RuleName, Rule> rulesByLabel = CapsuleUtil.transientMap();
        for(ImList.Immutable<Rule> ruleList : rules.values()) {
            for(Rule rule : ruleList) {
                if(!rule.label().isEmpty()) {
                    final Rule old;
                    if((old = rulesByLabel.__put(rule.label(), rule)) != null) {
                        logger.warn("Duplicate rule name {}. ", rule.label());
                        logger.debug("* rule 1: {}", old);
                        logger.debug("* rule 2: {}", rule);
                    }
                } else {
                    logger.warn("Rule without name for constraint {}/{}: {}.", rule.name(), rule.params().size(), rule);
                }
            }
        }
        return rulesByLabel.freeze();
    }

}
