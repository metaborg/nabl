package mb.statix.spec;

import java.util.Collection;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import mb.nabl2.regexp.IAlphabet;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ASpec {

    @Value.Parameter public abstract ListMultimap<String, Rule> rules();

    public ListMultimap<String, Rule> rulesWithEquivalentPatterns() {
        final ImmutableListMultimap.Builder<String, Rule> overlappingRules = ImmutableListMultimap.builder();
        rules().asMap().forEach((name, rules) -> {
            overlappingRules.putAll(name, rulesWithEquivalentPatterns(rules));
        });
        return overlappingRules.build();
    }

    private Collection<Rule> rulesWithEquivalentPatterns(Collection<Rule> rules) {
        return rules.stream().filter(r1 -> rules.stream().anyMatch(
                r2 -> !r1.equals(r2) && ARule.leftRightPatternOrdering.compare(r1, r2).map(c -> c == 0).orElse(false)))
                .collect(ImmutableList.toImmutableList());
    }

    @Value.Parameter public abstract Set<ITerm> edgeLabels();

    @Value.Parameter public abstract Set<ITerm> relationLabels();

    @Value.Parameter public abstract ITerm noRelationLabel();

    @Value.Parameter public abstract IAlphabet<ITerm> labels();

    @Value.Parameter public abstract Multimap<String, Tuple2<Integer, ITerm>> scopeExtensions();

}