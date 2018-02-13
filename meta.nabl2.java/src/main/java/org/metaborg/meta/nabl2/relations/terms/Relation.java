package org.metaborg.meta.nabl2.relations.terms;

import java.io.Serializable;
import java.util.Map;
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
import org.metaborg.meta.nabl2.util.Tuple2;
import org.metaborg.meta.nabl2.util.collections.HashTrieRelation2;
import org.metaborg.meta.nabl2.util.collections.IRelation2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;

public abstract class Relation<T> implements IRelation<T> {
    private static final ILogger logger = LoggerUtils.logger(Relation.class);

    protected Relation() {
    }


    @Override public Set.Immutable<T> smaller(T t) {
        Set.Transient<T> ts = Set.Transient.of();
        smaller(t, ts, Sets.newHashSet());
        if(getDescription().getSymmetry().equals(Symmetry.SYMMETRIC)) {
            larger(t, ts, Sets.newHashSet());
        }
        if(getDescription().getReflexivity().equals(Reflexivity.REFLEXIVE)) {
            ts.__insert(t);
        }
        return ts.freeze();
    }

    private void smaller(T current, Set.Transient<T> ts, java.util.Set<T> visited) {
        if(!visited.contains(current)) {
            visited.add(current);
            for(T next : entries().inverse().get(current)) {
                ts.__insert(next);
                if(getDescription().getTransitivity().equals(Transitivity.TRANSITIVE)) {
                    smaller(next, ts, visited);
                }
            }
        }
    }


    @Override public Set.Immutable<T> larger(T t) {
        Set.Transient<T> ts = Set.Transient.of();
        larger(t, ts, Sets.newHashSet());
        if(getDescription().getSymmetry().equals(Symmetry.SYMMETRIC)) {
            smaller(t, ts, Sets.newHashSet());
        }
        if(getDescription().getReflexivity().equals(Reflexivity.REFLEXIVE)) {
            ts.__insert(t);
        }
        return ts.freeze();
    }

    private void larger(T current, Set.Transient<T> ts, java.util.Set<T> visited) {
        if(!visited.contains(current)) {
            visited.add(current);
            for(T next : entries().get(current)) {
                ts.__insert(next);
                if(getDescription().getTransitivity().equals(Transitivity.TRANSITIVE)) {
                    larger(next, ts, visited);
                }
            }
        }
    }


    @Override public boolean contains(T t1, T t2) {
        if(t1.equals(t2)) {
            switch(getDescription().getReflexivity()) {
                case REFLEXIVE:
                    return true;
                case IRREFLEXIVE:
                    return false;
                case NON_REFLEXIVE:
                    break;
            }
        }
        boolean hit = contains(t1, t2, Sets.newHashSet());
        if(!hit && getDescription().getSymmetry().equals(Symmetry.SYMMETRIC)) {
            hit |= contains(t2, t1, Sets.newHashSet());
        }
        return hit;
    }

    private boolean contains(T current, T t, java.util.Set<T> visited) {
        if(!visited.contains(current)) {
            visited.add(current);
            return entries().get(current).stream().anyMatch(next -> {
                if(next.equals(t)) {
                    return true;
                } else if(getDescription().getTransitivity().equals(Transitivity.TRANSITIVE)) {
                    return contains(next, t, visited);
                } else {
                    return false;
                }
            });
        } else {
            return false;
        }
    }


    protected void canAddOrThrow(T t1, T t2) throws RelationException {
        if(getDescription().getReflexivity().equals(Reflexivity.IRREFLEXIVE) && t1.equals(t2)) {
            throw new ReflexivityException("Adding <" + t1 + "," + t2 + "> violates irreflexivity");
        }
        if(getDescription().getSymmetry().equals(Symmetry.ANTI_SYMMETRIC) && contains(t2, t1)) {
            throw new SymmetryException("Adding <" + t1 + "," + t2 + "> violates anti-symmetry");
        }
        if(getDescription().getTransitivity().equals(Transitivity.ANTI_TRANSITIVE)) {
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
    }


    @Override public Optional<T> leastUpperBound(T t1, T t2) {
        if(!getDescription().equals(RelationDescription.PARTIAL_ORDER)) {
            logger.warn("Lub must be called on partial-order, ignored.");
            return Optional.empty();
        }
        java.util.Set<T> bounds = Sets.newHashSet();
        bounds.addAll(larger(t1));
        bounds.retainAll(larger(t2));
        return bounds.stream().filter(l -> bounds.stream().allMatch(g -> contains(l, g))).findFirst();
    }

    @Override public Optional<T> greatestLowerBound(T t1, T t2) {
        if(!getDescription().equals(RelationDescription.PARTIAL_ORDER)) {
            logger.warn("Glb must be called on partial-order, ignored.");
            return Optional.empty();
        }
        java.util.Set<T> bounds = Sets.newHashSet();
        bounds.addAll(smaller(t1));
        bounds.retainAll(smaller(t2));
        return bounds.stream().filter(g -> bounds.stream().allMatch(l -> contains(l, g))).findFirst();
    }


    @Override public Stream<Tuple2<T, T>> stream() {
        return entries().stream();
    }


    public static class Immutable<T> extends Relation<T> implements IRelation.Immutable<T>, Serializable {
        private static final long serialVersionUID = 42L;

        protected final RelationDescription description;
        protected final IRelation2.Immutable<T, T> entries;

        public Immutable(RelationDescription description, IRelation2.Immutable<T, T> entries) {
            this.description = description;
            this.entries = entries;
        }

        @Override public RelationDescription getDescription() {
            return description;
        }

        @Override public IRelation2<T, T> entries() {
            return entries;
        }

        @Override public IRelation.Transient<T> melt() {
            return new Relation.Transient<>(description, entries.melt());
        }

        public static <T> Relation.Immutable<T> of(RelationDescription description) {
            return new Relation.Immutable<>(description, HashTrieRelation2.Immutable.of());
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + description.hashCode();
            result = prime * result + entries.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") final Relation.Immutable<T> other = (Relation.Immutable<T>) obj;
            if(!description.equals(other.description))
                return false;
            if(!entries.equals(other.entries))
                return false;
            return true;
        }
    
    }


    public static class Transient<T> extends Relation<T> implements IRelation.Transient<T> {

        protected final RelationDescription description;
        protected final IRelation2.Transient<T, T> entries;

        public Transient(RelationDescription description, IRelation2.Transient<T, T> entries) {
            this.description = description;
            this.entries = entries;
        }

        @Override public RelationDescription getDescription() {
            return description;
        }


        @Override public IRelation2<T, T> entries() {
            return entries;
        }

        @Override public boolean add(T t1, T t2) throws RelationException {
            canAddOrThrow(t1, t2);
            return entries.put(t1, t2);
        }

        @Override public IRelation.Immutable<T> freeze() {
            return new Relation.Immutable<>(description, entries.freeze());
        }

        public static <T> IRelation.Transient<T> of(RelationDescription description) {
            return new Relation.Transient<>(description, HashTrieRelation2.Transient.of());
        }

    }


    public static <T> IRelation.Transient<T> extend(IRelation.Transient<T> rel1, IRelation<T> rel2)
            throws RelationException {
        return new Extension<>(rel1, rel2);
    }

    public static class Extension<T> extends Relation<T> implements IRelation.Transient<T> {

        private final IRelation.Transient<T> rel1;
        private final IRelation<T> rel2;

        protected Extension(IRelation.Transient<T> rel1, IRelation<T> rel2) throws RelationException {
            this.rel1 = rel1;
            this.rel2 = rel2;
            check();
        }

        private void check() throws RelationException {
            if(!rel1.getDescription().equals(rel2.getDescription())) {
                throw new IllegalArgumentException("Relation descriptors must be equal.");
            }
            if(getDescription().getSymmetry().equals(Symmetry.ANTI_SYMMETRIC)) {
                for(Map.Entry<T, T> entry : rel1.entries().entrySet()) {
                    final T t1 = entry.getKey();
                    final T t2 = entry.getValue();
                    if(rel2.contains(t2, t1)) {
                        throw new SymmetryException("<" + t1 + "," + t2 + "> violates anti-symmetry");
                    }
                }
            }
            if(getDescription().getTransitivity().equals(Transitivity.ANTI_TRANSITIVE)) {
                for(Map.Entry<T, T> entry : rel1.entries().entrySet()) {
                    final T t1 = entry.getKey();
                    final T t2 = entry.getValue();
                    for(T t : rel1.smaller(t2)) {
                        if(rel2.contains(t1, t)) {
                            throw new TransitivityException("<" + t1 + "," + t2 + "> violates anti-transitivity");
                        }
                    }
                    for(T t : rel1.larger(t1)) {
                        if(rel2.contains(t, t2)) {
                            throw new TransitivityException("<" + t1 + "," + t2 + "> violates anti-transitivity");
                        }
                    }
                }
            }
        }

        public RelationDescription getDescription() {
            return rel1.getDescription();
        }

        @Override public IRelation2<T, T> entries() {
            return HashTrieRelation2.union(rel1.entries(), rel2.entries());
        }

        public boolean add(T t1, T t2) throws RelationException {
            canAddOrThrow(t1, t2);
            return rel1.add(t1, t2);
        }

        public IRelation.Immutable<T> freeze() {
            return rel1.freeze();
        }

    }

}
