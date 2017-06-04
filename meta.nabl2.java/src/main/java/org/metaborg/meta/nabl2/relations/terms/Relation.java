package org.metaborg.meta.nabl2.relations.terms;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.ReflexivityException;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.RelationDescription.Reflexivity;
import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.relations.SymmetryException;
import org.metaborg.meta.nabl2.relations.TransitivityException;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;

public abstract class Relation<T> implements IRelation<T> {
    private static final ILogger logger = LoggerUtils.logger(Relation.class);

    protected final RelationDescription description;

    private final SetMultimap<T, T> smaller;
    private final SetMultimap<T, T> larger;

    public Relation(RelationDescription description, SetMultimap<T, T> smaller, SetMultimap<T, T> larger) {
        this.description = description;
        this.smaller = smaller;
        this.larger = larger;
    }

    @Override public RelationDescription getDescription() {
        return description;
    }

    @Override public Set.Immutable<T> smaller(T t) {
        Set.Transient<T> ts = Set.Transient.of();
        ts.__insertAll(smaller.get(t));
        if(description.getReflexivity().equals(Reflexivity.REFLEXIVE)) {
            ts.__insert(t);
        }
        return ts.freeze();
    }

    @Override public Set.Immutable<T> larger(T t) {
        Set.Transient<T> ts = Set.Transient.of();
        ts.__insertAll(larger.get(t));
        if(description.getReflexivity().equals(Reflexivity.REFLEXIVE)) {
            ts.__insert(t);
        }
        return ts.freeze();
    }

    @Override public boolean contains(T t1, T t2) {
        if(t1.equals(t2)) {
            switch(description.getReflexivity()) {
                case REFLEXIVE:
                    return true;
                case IRREFLEXIVE:
                    return false;
                case NON_REFLEXIVE:
                    break;
            }
        }
        return larger.get(t1).contains(t2);
    }

    @Override public Optional<T> leastUpperBound(T t1, T t2) {
        if(!description.equals(RelationDescription.PARTIAL_ORDER)) {
            logger.warn("Lub must be called on partial-order, ignored.");
            return Optional.empty();
        }
        java.util.Set<T> bounds = Sets.newHashSet();
        bounds.addAll(larger(t1));
        bounds.retainAll(larger(t2));
        return bounds.stream().filter(l -> bounds.stream().allMatch(g -> contains(l, g))).findFirst();
    }

    @Override public Optional<T> greatestLowerbound(T t1, T t2) {
        if(!description.equals(RelationDescription.PARTIAL_ORDER)) {
            logger.warn("Glb must be called on partial-order, ignored.");
            return Optional.empty();
        }
        java.util.Set<T> bounds = Sets.newHashSet();
        bounds.addAll(smaller(t1));
        bounds.retainAll(smaller(t2));
        return bounds.stream().filter(g -> bounds.stream().allMatch(l -> contains(l, g))).findFirst();
    }

    @Override public Stream<Tuple2<T, T>> stream() {
        return smaller.entrySet().stream().map(Tuple2::of);
    }

    public static class Immutable<T> extends Relation<T> implements IRelation.Immutable<T>, Serializable {
        private static final long serialVersionUID = 42L;

        private final SetMultimap.Immutable<T, T> smaller;
        private final SetMultimap.Immutable<T, T> larger;

        public Immutable(RelationDescription description, SetMultimap.Immutable<T, T> smaller,
                SetMultimap.Immutable<T, T> larger) {
            super(description, smaller, larger);
            this.smaller = smaller;
            this.larger = larger;
        }

        public Relation.Transient<T> melt() {
            return new Relation.Transient<>(description, smaller.asTransient(), larger.asTransient());
        }

        public static <T> Relation.Immutable<T> of(RelationDescription description) {
            return new Relation.Immutable<>(description, SetMultimap.Immutable.of(), SetMultimap.Immutable.of());
        }

    }

    public static class Transient<T> extends Relation<T> implements IRelation.Transient<T> {

        private final SetMultimap.Transient<T, T> smaller;
        private final SetMultimap.Transient<T, T> larger;

        public Transient(RelationDescription description, SetMultimap.Transient<T, T> smaller,
                SetMultimap.Transient<T, T> larger) {
            super(description, smaller, larger);
            this.smaller = smaller;
            this.larger = larger;
        }

        @Override public boolean add(T t1, T t2) throws RelationException {
            if(t1.equals(t2)) {
                switch(description.getReflexivity()) {
                    case REFLEXIVE:
                        return false;
                    case IRREFLEXIVE:
                        throw new ReflexivityException("Adding <" + t1 + "," + t2 + "> violates irreflexivity");
                    case NON_REFLEXIVE:
                        break;
                }
            }

            switch(description.getSymmetry()) {
                case SYMMETRIC:
                    extend(t1, t2, larger, smaller);
                    break;
                case ANTI_SYMMETRIC:
                    if(contains(t2, t1) && !t1.equals(t2)) {
                        throw new SymmetryException("Adding <" + t1 + "," + t2 + "> violates anti-symmetry");
                    }
                    break;
                case NON_SYMMETRIC:
                    break;
            }

            return extend(t1, t2, smaller, larger);
        }

        private boolean extend(T t1, T t2, SetMultimap.Transient<T, T> smaller, SetMultimap.Transient<T, T> larger)
                throws RelationException {
            if(larger.__insert(t1, t2)) {
                smaller.__insert(t2, t1);

                switch(description.getTransitivity()) {
                    case TRANSITIVE:
                        final Collection<T> largerTs = larger.get(t2);
                        final Collection<T> smallerTs = smaller.get(t1);
                        insertAll(larger, t1, largerTs);
                        for(T t : smallerTs) {
                            insertAll(larger, t, largerTs);
                            larger.__insert(t, t2);
                        }
                        insertAll(smaller, t2, smallerTs);
                        for(T t : largerTs) {
                            insertAll(smaller, t, smallerTs);
                            smaller.__insert(t, t1);
                        }
                        break;
                    case ANTI_TRANSITIVE:
                        for(T t : smaller.get(t2)) {
                            if(contains(t1, t)) {
                                throw new TransitivityException(
                                        "Adding <" + t1 + "," + t2 + "> violates anti-transitivity");
                            }
                        }
                        for(T t : larger.get(t1)) {
                            if(contains(t, t2)) {
                                throw new TransitivityException(
                                        "Adding <" + t1 + "," + t2 + "> violates anti-transitivity");
                            }
                        }
                        break;
                    case NON_TRANSITIVE:
                        break;
                }
                return true;
            } else {
                return false;
            }
        }

        private void insertAll(SetMultimap.Transient<T, T> map, T key, Collection<T> values) {
            values.stream().forEach(value -> map.__insert(key, value));
        }

        @Override public Relation.Immutable<T> freeze() {
            return new Relation.Immutable<>(description, smaller.freeze(), larger.freeze());
        }

        public static <T> Relation.Transient<T> of(RelationDescription description) {
            return new Relation.Transient<>(description, SetMultimap.Transient.of(), SetMultimap.Transient.of());
        }

    }

}