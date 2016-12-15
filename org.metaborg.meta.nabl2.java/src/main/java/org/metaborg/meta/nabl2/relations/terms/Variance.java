package org.metaborg.meta.nabl2.relations.terms;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.relations.IVariance;

public class Variance {

    @Value.Immutable
    @Serial.Version(value = 42L)
    public static abstract class Invariant implements IVariance {

        @Override public <T> T match(Cases<T> cases) {
            return cases.caseInvariant();
        }

    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    public static abstract class Covariant implements IVariance {

        @Value.Parameter public abstract RelationName getRelation();

        @Override public <T> T match(Cases<T> cases) {
            return cases.caseCovariant(getRelation());
        }

    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    public static abstract class Contravariant implements IVariance {

        @Value.Parameter public abstract RelationName getRelation();

        @Override public <T> T match(Cases<T> cases) {
            return cases.caseContravariant(getRelation());
        }

    }

}