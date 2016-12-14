package org.metaborg.meta.nabl2.relations;

import java.util.Collection;
import java.util.Collections;

import org.metaborg.meta.nabl2.relations.RelationDescription.Reflexivity;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class Relation<T> implements IRelation<T> {

    private final RelationDescription description;
    private final Multimap<T,T> smaller;
    private final Multimap<T,T> larger;

    public Relation(RelationDescription description) {
        this.description = description;
        this.smaller = HashMultimap.create();
        this.larger = HashMultimap.create();
    }

    @Override public RelationDescription getDescription() {
        return description;
    }

    public void add(T t1, T t2) throws RelationException {
        if (t1.equals(t2)) {
            switch (description.getReflexivity()) {
            case REFLEXIVE:
                return;
            case IRREFLEXIVE:
                throw new ReflexivityException();
            case NON_REFLEXIVE:
                break;
            }
        }

        extend(t1, t2, smaller, larger);

        switch (description.getSymmetry()) {
        case SYMMETRIC:
            extend(t1, t2, larger, smaller);
            break;
        case ANTI_SYMMETRIC:
            if (contains(t2, t1) && !t1.equals(t2)) {
                throw new SymmetryException();
            }
            break;
        case NON_SYMMETRIC:
            break;
        }
    }

    private void extend(T t1, T t2, Multimap<T,T> smaller, Multimap<T,T> larger) throws RelationException {
        larger.put(t1, t2);
        smaller.put(t2, t1);

        switch (description.getTransitivity()) {
        case TRANSITIVE:
            final Collection<T> largerTs = larger.get(t2);
            final Collection<T> smallerTs = smaller.get(t1);
            larger.putAll(t1, largerTs);
            for (T t : smallerTs) {
                larger.putAll(t, largerTs);
                larger.put(t, t2);
            }
            smaller.putAll(t2, smallerTs);
            for (T t : largerTs) {
                smaller.putAll(t, smallerTs);
                smaller.put(t, t1);
            }
            break;
        case ANTI_TRANSITIVE:
            for (T t : smaller.get(t2)) {
                if (contains(t1, t)) {
                    throw new TransitivityException();
                }
            }
            for (T t : larger.get(t1)) {
                if (contains(t, t2)) {
                    throw new TransitivityException();
                }
            }
            break;
        case NON_TRANSITIVE:
            break;
        }
    }

    @Override public Collection<T> smaller(T t) {
        Collection<T> ts = smaller.get(t);
        if (description.getReflexivity().equals(Reflexivity.REFLEXIVE)) {
            ts.add(t);
        }
        return Collections.unmodifiableCollection(ts);
    }

    @Override public Collection<T> larger(T t) {
        Collection<T> ts = larger.get(t);
        if (description.getReflexivity().equals(Reflexivity.REFLEXIVE)) {
            ts.add(t);
        }
        return Collections.unmodifiableCollection(ts);
    }

    @Override public boolean contains(T t1, T t2) {
        if (t1.equals(t2)) {
            switch (description.getReflexivity()) {
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

}