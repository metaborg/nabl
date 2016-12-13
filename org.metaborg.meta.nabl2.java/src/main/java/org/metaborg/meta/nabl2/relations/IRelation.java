package org.metaborg.meta.nabl2.relations;

import org.metaborg.util.iterators.Iterables2;

public interface IRelation<T> {

    public enum Symmetry {

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

    public enum Reflexivity {

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

    public enum Transitivity {

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

    Iterable<T> smaller(T t);

    Iterable<T> larger(T t);

    boolean contains(T t1, T t2);

    static <T> IRelation<T> empty() {
        return new IRelation<T>() {

            @Override public Iterable<T> smaller(T t) {
                return Iterables2.empty();
            }

            @Override public Iterable<T> larger(T t) {
                return Iterables2.empty();
            }

            @Override public boolean contains(T t1, T t2) {
                return false;
            }

        };
    }

}