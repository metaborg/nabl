package org.metaborg.meta.nabl2.relations.terms;

import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.ReflexivityException;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.RelationDescription.Reflexivity;
import org.metaborg.meta.nabl2.relations.RelationDescription.Symmetry;
import org.metaborg.meta.nabl2.relations.RelationDescription.Transitivity;
import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.relations.SymmetryException;
import org.metaborg.meta.nabl2.relations.TransitivityException;
import org.metaborg.meta.nabl2.util.collections.HashTrieRelation2;
import org.metaborg.meta.nabl2.util.collections.IRelation2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;

public abstract class Relation<T> implements IRelation<T> {
    private static final ILogger logger = LoggerUtils.logger(Relation.class);

    protected final RelationDescription description;

    private final IRelation2<T, T> pairs;

    public Relation(RelationDescription description, IRelation2<T, T> pairs) {
        this.description = description;
        this.pairs = pairs;
    }

    @Override public RelationDescription getDescription() {
        return description;
    }

    @Override public Set.Immutable<T> smaller(T t) {
        Set.Transient<T> ts = Set.Transient.of();
        smaller(t, ts, Sets.newHashSet());
        if(description.getSymmetry().equals(Symmetry.SYMMETRIC)) {
            larger(t, ts, Sets.newHashSet());
        }
        if(description.getReflexivity().equals(Reflexivity.REFLEXIVE)) {
            ts.__insert(t);
        }
        return ts.freeze();
    }

    private void smaller(T current, Set.Transient<T> ts, java.util.Set<T> visited) {
        if(!visited.contains(current)) {
            visited.add(current);
            for(T next : pairs.inverse().get(current)) {
                ts.__insert(next);
                if(description.getTransitivity().equals(Transitivity.TRANSITIVE)) {
                    smaller(next, ts, visited);
                }
            }
        }
    }


    @Override public Set.Immutable<T> larger(T t) {
        Set.Transient<T> ts = Set.Transient.of();
        larger(t, ts, Sets.newHashSet());
        if(description.getSymmetry().equals(Symmetry.SYMMETRIC)) {
            smaller(t, ts, Sets.newHashSet());
        }
        if(description.getReflexivity().equals(Reflexivity.REFLEXIVE)) {
            ts.__insert(t);
        }
        return ts.freeze();
    }

    private void larger(T current, Set.Transient<T> ts, java.util.Set<T> visited) {
        if(!visited.contains(current)) {
            visited.add(current);
            for(T next : pairs.get(current)) {
                ts.__insert(next);
                if(description.getTransitivity().equals(Transitivity.TRANSITIVE)) {
                    larger(next, ts, visited);
                }
            }
        }
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
        boolean hit = contains(t1, t2, Sets.newHashSet());
        if(!hit && description.getSymmetry().equals(Symmetry.SYMMETRIC)) {
            hit |= contains(t2, t1, Sets.newHashSet());
        }
        return hit;
    }

    private boolean contains(T current, T t, java.util.Set<T> visited) {
        if(!visited.contains(current)) {
            visited.add(current);
            return pairs.get(current).stream().anyMatch(next -> {
                if(next.equals(t)) {
                    return true;
                } else if(description.getTransitivity().equals(Transitivity.TRANSITIVE)) {
                    return contains(next, t, visited);
                } else {
                    return false;
                }
            });
        } else {
            return false;
        }
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
        return pairs.entrySet().stream().map(Tuple2::of);
    }


    public static class Immutable<T> extends Relation<T> implements IRelation.Immutable<T>, Serializable {
        private static final long serialVersionUID = 42L;

        private final IRelation2.Immutable<T, T> pairs;

        public Immutable(RelationDescription description, IRelation2.Immutable<T, T> pairs) {
            super(description, pairs);
            this.pairs = pairs;
        }

        public Relation.Transient<T> melt() {
            return new Relation.Transient<>(description, pairs.melt());
        }

        public static <T> Relation.Immutable<T> of(RelationDescription description) {
            return new Relation.Immutable<>(description, HashTrieRelation2.Immutable.of());
        }

    }

    public static class Transient<T> extends Relation<T> implements IRelation.Transient<T> {

        private final IRelation2.Transient<T, T> pairs;

        public Transient(RelationDescription description, IRelation2.Transient<T, T> pairs) {
            super(description, pairs);
            this.pairs = pairs;
        }

        @Override public boolean add(T t1, T t2) throws RelationException {
            if(description.getReflexivity().equals(Reflexivity.IRREFLEXIVE) && t1.equals(t2)) {
                throw new ReflexivityException("Adding <" + t1 + "," + t2 + "> violates irreflexivity");
            }
            if(description.getSymmetry().equals(Symmetry.ANTI_SYMMETRIC) && contains(t2, t1)) {
                throw new SymmetryException("Adding <" + t1 + "," + t2 + "> violates anti-symmetry");
            }
            if(description.getTransitivity().equals(Transitivity.ANTI_TRANSITIVE)) {
                for(T t : smaller(t2)) {
                    if(contains(t1, t)) {
                        throw new TransitivityException("Adding <" + t1 + "," + t2 + "> violates anti-transitivity");
                    }
                }
                for(T t : larger(t1)) {
                    if(contains(t, t2)) {
                        throw new TransitivityException("Adding <" + t1 + "," + t2 + "> violates anti-transitivity");
                    }
                }

            }
            return pairs.put(t1, t2);
        }


        @Override public Relation.Immutable<T> freeze() {
            return new Relation.Immutable<>(description, pairs.freeze());
        }

        public static <T> Relation.Transient<T> of(RelationDescription description) {
            return new Relation.Transient<>(description, HashTrieRelation2.Transient.of());
        }

    }

}