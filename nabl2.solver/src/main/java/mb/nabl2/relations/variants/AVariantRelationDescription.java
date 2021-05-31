package mb.nabl2.relations.variants;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.scopegraph.relations.RelationDescription;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AVariantRelationDescription<T> {

    @Value.Parameter public abstract RelationDescription relationDescription();

    @Value.Parameter public abstract List<IVariantMatcher<T>> variantMatchers();

}