package mb.nabl2.relations.impl;

import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.HashTrieRelation2;
import org.metaborg.util.collection.IRelation2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.tuple.Tuple2;

import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;
import mb.nabl2.relations.ARelationDescription.Reflexivity;
import mb.nabl2.relations.ARelationDescription.Symmetry;
import mb.nabl2.relations.ARelationDescription.Transitivity;
import mb.nabl2.relations.IRelation;
import mb.nabl2.relations.ReflexivityException;
import mb.nabl2.relations.RelationDescription;
import mb.nabl2.relations.RelationException;
import mb.nabl2.relations.SymmetryException;
import mb.nabl2.relations.TransitivityException;

public abstract class Relation<T> implements IRelation<T> {
    private static final ILogger logger = LoggerUtils.logger(Relation.class);

    protected Relation() {
    }

    @Override public boolean isEmpty() {
        return entries().isEmpty();
    }

    @Override public Set.Immutable<T> smaller(T t) {
        Set.Transient<T> ts = CapsuleUtil.transientSet();
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
        Set.Transient<T> ts = CapsuleUtil.transientSet();
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
        if(t1.equals(t2)) {
            if(getDescription().getReflexivity().equals(Reflexivity.IRREFLEXIVE)) {
                throw new ReflexivityException("Adding <" + t1 + "," + t2 + "> violates irreflexivity");
            }
        } else {
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

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getDescription()).append(" { ");
        stream().forEach(entry -> {
            sb.append(entry._1());
            sb.append(" < ");
            sb.append(entry._2());
            sb.append("; ");
        });
        sb.append("}");
        return sb.toString();

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

}