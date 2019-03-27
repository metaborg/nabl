package mb.statix.spec;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import mb.nabl2.regexp.IAlphabet;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ASpec {

    @Value.Parameter public abstract ListMultimap<String, IRule> rules();

    public ListMultimap<String, IRule> overlappingRules() {
        final ImmutableListMultimap.Builder<String, IRule> overlappingRules = ImmutableListMultimap.builder();
        rules().asMap().forEach((name, rules) -> {
            overlappingRules.putAll(name, overlappingRules(rules));
        });
        return overlappingRules.build();
    }

    private Collection<IRule> overlappingRules(Collection<IRule> rules) {
        return rules.stream()
                .filter(r1 -> rules.stream()
                        .anyMatch(r2 -> !r1.equals(r2) && ARule.leftRightPatternOrdering.compare(r1, r2) == 0))
                .collect(Collectors.toList());
    }

    @Value.Parameter public abstract IAlphabet<ITerm> labels();

    @Value.Parameter public abstract ITerm endOfPath();

    @Value.Parameter public abstract Map<ITerm, Type> relations();

    @Value.Parameter public abstract Multimap<String, Tuple2<Integer, ITerm>> scopeExtensions();

}