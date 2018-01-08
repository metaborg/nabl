package org.metaborg.meta.nabl2.relations.terms;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.relations.IVariance;
import org.metaborg.meta.nabl2.relations.IVariantMatcher;
import org.metaborg.meta.nabl2.relations.InstantiatedVariantsException;
import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.util.Optionals;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Multimap;

public class Relations<T> implements IRelations<T>, Serializable {

    private static final long serialVersionUID = 42L;

    private final Map<IRelationName, Relation<T>> relations;
    private final Multimap<IRelationName, IVariantMatcher<T>> variantMatchers;

    public Relations(Map<IRelationName, Relation<T>> relations,
            Multimap<IRelationName, IVariantMatcher<T>> variantMatchers) {
        this.relations = relations;
        this.variantMatchers = variantMatchers;
    }

    @Override public Iterable<IRelationName> getNames() {
        return relations.keySet();
    }

    public void add(IRelationName name, T t1, T t2) throws RelationException {
        if(!relations.containsKey(name)) {
            throw new NoSuchElementException("Relation " + name + " not defined.");
        }
        Relation<T> relation = relations.get(name);
        for(IVariantMatcher<T> matcher : variantMatchers.get(name)) {
            if(Optionals.lift(matcher.match(t1), matcher.match(t2), (a1, a2) -> true).isPresent()) {
                throw new InstantiatedVariantsException(
                        "Cannot add instantiated pair of variant constructors to the relation: " + t1 + " and " + t2);
            }
        }
        relation.add(t1, t2);
    }

    @Override public boolean contains(IRelationName name, T t1, T t2) {
        if(!relations.containsKey(name)) {
            throw new NoSuchElementException("Relation " + name + " not defined.");
        }
        Relation<T> relation = relations.get(name);
        for(IVariantMatcher<T> matcher : variantMatchers.get(name)) {
            Optional<Boolean> contains = Optionals.lift(matcher.match(t1), matcher.match(t2), (args1, args2) -> {
                return (args1.size() == args2.size())
                        && Iterables2.stream(Iterables2.zip(args1, args2, (arg1, arg2) -> {
                            T argt1 = arg1.getValue();
                            T argt2 = arg2.getValue();
                    // @formatter:off
                    return arg1.getVariance().match(IVariance.cases(
                        () -> argt1.equals(argt2),
                        r -> contains(nameOrDefault(r, name), argt1, argt2),
                        r -> contains(nameOrDefault(r, name), argt2, argt1)
                    ));
                    // @formatter:on
                        })).allMatch(c -> c);
            });
            if(contains.isPresent()) {
                return contains.get();
            }
        }
        return relation.contains(t1, t2);
    }

    @Override public Optional<T> leastUpperBound(IRelationName name, T t1, T t2) {
        if(!relations.containsKey(name)) {
            throw new NoSuchElementException("Relation " + name + " not defined.");
        }
        for(IVariantMatcher<T> matcher : variantMatchers.get(name)) {
            Optional<Optional<T>> contains = Optionals.lift(matcher.match(t1), matcher.match(t2), (args1, args2) -> {
                return Optionals.when(args1.size() == args2.size()).flatMap(eq -> {
                    return Optionals.sequence(Iterables2.zip(args1, args2, (arg1, arg2) -> {
                        T argt1 = arg1.getValue();
                        T argt2 = arg2.getValue();
                        return arg1.getVariance()
                                .match(IVariance.<Optional<T>>cases(
                            // @formatter:off
                            () -> argt1.equals(argt2) ? Optional.of(argt1) : Optional.empty(),
                            r -> leastUpperBound(nameOrDefault(r,name), argt1, argt2),
                            r -> greatestLowerBound(nameOrDefault(r,name), argt1, argt2)
                            // @formatter:on
                        ));
                    })).map(matcher::build);
                });
            });
            if(contains.isPresent()) {
                return contains.get();
            }
        }
        return relations.get(name).leastUpperBound(t1, t2);
    }

    @Override public Optional<T> greatestLowerBound(IRelationName name, T t1, T t2) {
        if(!relations.containsKey(name)) {
            throw new NoSuchElementException("Relation " + name + " not defined.");
        }
        for(IVariantMatcher<T> matcher : variantMatchers.get(name)) {
            Optional<Optional<T>> contains = Optionals.lift(matcher.match(t1), matcher.match(t2), (args1, args2) -> {
                return Optionals.when(args1.size() == args2.size()).flatMap(eq -> {
                    return Optionals.sequence(Iterables2.zip(args1, args2, (arg1, arg2) -> {
                        T argt1 = arg1.getValue();
                        T argt2 = arg2.getValue();
                        return arg1.getVariance()
                                .match(IVariance.<Optional<T>>cases(
                            // @formatter:off
                            () -> argt1.equals(argt2) ? Optional.of(argt1) : Optional.empty(),
                            r -> greatestLowerBound(nameOrDefault(r,name), argt1, argt2),
                            r -> leastUpperBound(nameOrDefault(r,name), argt1, argt2)
                            // @formatter:on
                        ));
                    })).map(matcher::build);
                });
            });
            if(contains.isPresent()) {
                return contains.get();
            }
        }
        return relations.get(name).greatestLowerbound(t1, t2);
    }

    private IRelationName nameOrDefault(IRelationName name, IRelationName defaultName) {
        return name.getName().isPresent() ? name : defaultName;
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + relations.hashCode();
        result = prime * result + variantMatchers.hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked") final Relations<T> other = (Relations<T>) obj;
        if(!relations.equals(other.relations))
            return false;
        if(!variantMatchers.equals(other.variantMatchers))
            return false;
        return true;
    }
    
}