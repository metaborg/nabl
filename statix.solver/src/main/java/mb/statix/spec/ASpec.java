package mb.statix.spec;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ASpec {

    @Value.Parameter public abstract RuleSet rules();

    @Value.Parameter public abstract Set.Immutable<ITerm> edgeLabels();

    @Value.Parameter public abstract Set.Immutable<ITerm> dataLabels();

    @Value.Lazy public Set.Immutable<ITerm> allLabels() {
        return Set.Immutable.union(edgeLabels(), dataLabels());
    }

    // TODO: Should this be a capsule SetMultimap? or can there be duplicate key-value pairs?
    @Value.Parameter public abstract MultiSetMap.Immutable<String, Tuple2<Integer, ITerm>> scopeExtensions();

    @Value.Default public boolean hasPrecomputedCriticalEdges() {
        return false;
    }

    public Spec precomputeCriticalEdges() {
        return Spec
                .of(rules().precomputeCriticalEdges(scopeExtensions()), edgeLabels(), dataLabels(), scopeExtensions())
                .withHasPrecomputedCriticalEdges(true);
    }

    public static Spec of() {
        return Spec.of(new RuleSet(CapsuleUtil.immutableMap()), CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet(),
                MultiSetMap.Immutable.of());
    }

}