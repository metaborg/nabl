package mb.statix.spec;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

import mb.nabl2.regexp.IAlphabet;
import mb.nabl2.regexp.impl.FiniteAlphabet;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ASpec {

    @Value.Parameter public abstract RuleSet rules();

    @Value.Parameter public abstract Set<ITerm> edgeLabels();

    @Value.Parameter public abstract Set<ITerm> relationLabels();

    @Value.Parameter public abstract ITerm noRelationLabel();

    @Value.Parameter public abstract IAlphabet<ITerm> labels();

    @Value.Parameter public abstract SetMultimap<String, Tuple2<Integer, ITerm>> scopeExtensions();

    public static Spec of() {
        return Spec.of(new RuleSet(ImmutableListMultimap.of()), ImmutableSet.of(), ImmutableSet.of(), B.EMPTY_TUPLE,
                new FiniteAlphabet<>(), ImmutableSetMultimap.of());
    }

}