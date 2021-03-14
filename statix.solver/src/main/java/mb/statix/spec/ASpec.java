package mb.statix.spec;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ASpec {

    @Value.Parameter public abstract RuleSet rules();

    @Value.Parameter public abstract Set<ITerm> edgeLabels();

    @Value.Parameter public abstract Set<ITerm> dataLabels();

    @Value.Lazy public Set<ITerm> allLabels() {
        return Sets.union(edgeLabels(), dataLabels());
    }

    @Value.Parameter public abstract SetMultimap<String, Tuple2<Integer, ITerm>> scopeExtensions();

    @Value.Default public boolean hasPrecomputedCriticalEdges() {
        return false;
    }

    public Spec precomputeCriticalEdges() {
        return Spec
                .of(rules().precomputeCriticalEdges(scopeExtensions()), edgeLabels(), dataLabels(), scopeExtensions())
                .withHasPrecomputedCriticalEdges(true);
    }

    public static Spec of() {
        return Spec.of(new RuleSet(ImmutableListMultimap.of()), ImmutableSet.of(), ImmutableSet.of(),
                ImmutableSetMultimap.of());
    }

}