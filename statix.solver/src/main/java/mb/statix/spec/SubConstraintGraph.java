package mb.statix.spec;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;

import java.io.Serializable;

class SubConstraintGraph implements Serializable {

    private static final long serialVersionUID = 42L;

    private final SetMultimap.Immutable<RuleName, String> subConstraints;

    private final SetMultimap.Immutable<String, RuleName> invokingRules;

    public SubConstraintGraph(SetMultimap.Immutable<RuleName, String> subConstraints,
            SetMultimap.Immutable<String, RuleName> invokingRules) {
        this.subConstraints = subConstraints;
        this.invokingRules = invokingRules;
    }

    public Set.Immutable<String> subConstraints(RuleName rule) {
        return subConstraints.get(rule);
    }

    public Set.Immutable<RuleName> invokingRules(String rule) {
        return invokingRules.get(rule);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static SubConstraintGraph of(Map.Immutable<String, ImList.Immutable<Rule>> rules) {
        final Builder builder = newBuilder();
        for(ImList.Immutable<Rule> ruleList : rules.values()) {
            ruleList.forEach(builder::addRule);
        }
        return builder.build();
    }

    public static class Builder {

        private final SetMultimap.Transient<RuleName, String> subConstraints = SetMultimap.Transient.of();

        private final SetMultimap.Transient<String, RuleName> invokingRules = SetMultimap.Transient.of();

        public Builder addRule(Rule rule) {
            if(!rule.label().isEmpty()) {
                addCalls(rule.label(), rule.body());
            }
            return this;
        }

        private void addCalls(RuleName label, IConstraint constraint) {
            // @formatter:off
            constraint.match(Constraints.<Unit>cases()
                .conj(cc -> {
                    Constraints.disjoin(cc, c -> addCalls(label, c));
                    return Unit.unit;
                })
                .exists(exists -> {
                    addCalls(label, exists.constraint());
                    return Unit.unit;
                })
                .user(user -> {
                    subConstraints.__insert(label, user.name());
                    invokingRules.__insert(user.name(), label);
                    return Unit.unit;
                })
                .otherwise(c -> Unit.unit)
            );
            // @formatter:on
        }

        public SubConstraintGraph build() {
            return new SubConstraintGraph(subConstraints.freeze(), invokingRules.freeze());
        }

    }

}
