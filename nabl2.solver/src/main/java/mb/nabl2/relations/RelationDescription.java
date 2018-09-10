package mb.nabl2.relations;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable(prehash = false, builder = false)
@Serial.Version(value = 42L)
public abstract class RelationDescription {

    public static enum Reflexivity {

        /**
         * Relation that obeys the following reflexivity property: <code>xRy ==> yRx</code>
         */
        REFLEXIVE("refl"),

        /**
         * Relation that obeys the following reflexivity property: <code>xRy ==> ~yRx</code>
         */
        IRREFLEXIVE("irrefl"),

        /**
         * Relation with no reflexivity property.
         */
        NON_REFLEXIVE("");

        private final String name;

        private Reflexivity(String name) {
            this.name = name;
        }

        @Override public String toString() {
            return name;
        }

    }

    public static enum Symmetry {

        /**
         * Relation that obeys the following symmetry property: <code>xRy ==> yRx</code>
         */
        SYMMETRIC("sym"),

        /**
         * Relation that obeys the following symmetry property: <code>xRy, yRx ==> x = y</code>
         */
        ANTI_SYMMETRIC("anti-sym"),

        /**
         * Relation with no symmetry property.
         */
        NON_SYMMETRIC("");

        private final String name;

        private Symmetry(String name) {
            this.name = name;
        }

        @Override public String toString() {
            return name;
        }

    }

    public static enum Transitivity {

        /**
         * Relation that obeys the following transitivity property: <code>xRy, yRz ==> xRz</code>
         */
        TRANSITIVE("trans"),

        /**
         * Relation that obeys the following transitivity property: <code>xRy, yRz ==> ~xRz</code>
         */
        ANTI_TRANSITIVE("anti-trans"),

        /**
         * Relation with no transitivity property.
         */
        NON_TRANSITIVE("");

        private final String name;

        private Transitivity(String name) {
            this.name = name;
        }

        @Override public String toString() {
            return name;
        }

    }

    @Value.Parameter public abstract Reflexivity getReflexivity();

    @Value.Parameter public abstract Symmetry getSymmetry();

    @Value.Parameter public abstract Transitivity getTransitivity();

    public static final RelationDescription PARTIAL_ORDER =
            ImmutableRelationDescription.of(Reflexivity.REFLEXIVE, Symmetry.ANTI_SYMMETRIC, Transitivity.TRANSITIVE);

    public static final RelationDescription STRICT_PARTIAL_ORDER =
            ImmutableRelationDescription.of(Reflexivity.IRREFLEXIVE, Symmetry.ANTI_SYMMETRIC, Transitivity.TRANSITIVE);

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getReflexivity());
        if(sb.length() > 0) {
            sb.append(" ");
        }
        sb.append(getTransitivity());
        if(sb.length() > 0) {
            sb.append(" ");
        }
        sb.append(getSymmetry());
        return sb.toString();
    }

}