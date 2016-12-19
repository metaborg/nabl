package org.metaborg.meta.nabl2.relations;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class RelationDescription {

    public static enum Reflexivity {

        /**
         * Relation that obeys the following reflexivity property:
         * <code>xRy ==> yRx</code>
         */
        REFLEXIVE,

        /**
         * Relation that obeys the following reflexivity property:
         * <code>xRy ==> ~yRx</code>
         */
        IRREFLEXIVE,

        /**
         * Relation with no reflexivity property.
         */
        NON_REFLEXIVE

    }

    public static enum Symmetry {

        /**
         * Relation that obeys the following symmetry property:
         * <code>xRy ==> yRx</code>
         */
        SYMMETRIC,

        /**
         * Relation that obeys the following symmetry property:
         * <code>xRy, yRx ==> x = y</code>
         */
        ANTI_SYMMETRIC,

        /**
         * Relation with no symmetry property.
         */
        NON_SYMMETRIC

    }

    public static enum Transitivity {

        /**
         * Relation that obeys the following transitivity property:
         * <code>xRy, yRz ==> xRz</code>
         */
        TRANSITIVE,

        /**
         * Relation that obeys the following transitivity property:
         * <code>xRy, yRz ==> ~xRz</code>
         */
        ANTI_TRANSITIVE,

        /**
         * Relation with no transitivity property.
         */
        NON_TRANSITIVE
    }

    @Value.Parameter public abstract Reflexivity getReflexivity();

    @Value.Parameter public abstract Symmetry getSymmetry();

    @Value.Parameter public abstract Transitivity getTransitivity();

    public static final RelationDescription PARTIAL_ORDER = ImmutableRelationDescription.of(Reflexivity.REFLEXIVE,
            Symmetry.ANTI_SYMMETRIC, Transitivity.TRANSITIVE);

    public static final RelationDescription STRICT_PARTIAL_ORDER = ImmutableRelationDescription.of(
            Reflexivity.IRREFLEXIVE, Symmetry.ANTI_SYMMETRIC, Transitivity.TRANSITIVE);

}