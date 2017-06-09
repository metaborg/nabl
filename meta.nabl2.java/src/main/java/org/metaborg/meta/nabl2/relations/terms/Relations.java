package org.metaborg.meta.nabl2.relations.terms;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.relations.IVariance;
import org.metaborg.meta.nabl2.relations.IVariantMatcher;
import org.metaborg.meta.nabl2.relations.InstantiatedVariantsException;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.Lists;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.SetMultimap;

public abstract class Relations<T> implements IRelations<T> {

    private final Map.Immutable<IRelationName, ? extends IRelation<T>> relations;
    protected final SetMultimap.Immutable<IRelationName, IVariantMatcher<T>> variantMatchers;

    private Relations(Map.Immutable<IRelationName, ? extends IRelation<T>> relations,
            SetMultimap.Immutable<IRelationName, IVariantMatcher<T>> variantMatchers) {
        this.relations = relations;
        this.variantMatchers = variantMatchers;
    }

    @Override public java.util.Set<IRelationName> getNames() {
        return relations.keySet();
    }

    @Override public boolean contains(IRelationName name, T t1, T t2) {
        if(!relations.containsKey(name)) {
            throw new NoSuchElementException("Relation " + name + " not defined.");
        }
        IRelation<T> relation = relations.get(name);
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
                    })).map(args -> matcher.build(Lists.newArrayList(args)));
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
                    })).map(args -> matcher.build(Lists.newArrayList(args)));
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

    public Stream<Tuple2<T, T>> stream(IRelationName name) {
        return relations.get(name).stream();
    }

    public static class Immutable<T> extends Relations<T> implements IRelations.Immutable<T>, Serializable {
        private static final long serialVersionUID = 42L;

        private final Map.Immutable<IRelationName, IRelation.Immutable<T>> relations;

        public Immutable(Map.Immutable<IRelationName, IRelation.Immutable<T>> relations,
                SetMultimap.Immutable<IRelationName, IVariantMatcher<T>> variantMatchers) {
            super(relations, variantMatchers);
            this.relations = relations;
        }

        @Override public Relations.Transient<T> melt() {
            final Map.Transient<IRelationName, IRelation.Transient<T>> relationsBuilder = Map.Transient.of();
            for(Entry<IRelationName, IRelation.Immutable<T>> entry : relations.entrySet()) {
                relationsBuilder.__put(entry.getKey(), entry.getValue().melt());
            }
            return new Relations.Transient<>(relationsBuilder.freeze(), variantMatchers);
        }

        public static <T> Relations.Immutable<T> of() {
            return new Relations.Immutable<>(Map.Immutable.of(), SetMultimap.Immutable.of());
        }

    }

    public static class Transient<T> extends Relations<T> implements IRelations.Transient<T> {

        private final Map.Immutable<IRelationName, IRelation.Transient<T>> relations;

        public Transient(Map.Immutable<IRelationName, IRelation.Transient<T>> relations,
                SetMultimap.Immutable<IRelationName, IVariantMatcher<T>> variantMatchers) {
            super(relations, variantMatchers);
            this.relations = relations;
        }

        @Override public boolean add(IRelationName name, T t1, T t2) throws RelationException {
            if(!relations.containsKey(name)) {
                throw new NoSuchElementException("Relation " + name + " not defined.");
            }
            IRelation.Transient<T> relation = relations.get(name);
            for(IVariantMatcher<T> matcher : variantMatchers.get(name)) {
                if(Optionals.lift(matcher.match(t1), matcher.match(t2), (a1, a2) -> true).isPresent()) {
                    throw new InstantiatedVariantsException(
                            "Cannot add instantiated pair of variant constructors to the relation: " + t1 + " and "
                                    + t2);
                }
            }
            return relation.add(t1, t2);
        }

        @Override public IRelations.Immutable<T> freeze() {
            Map.Transient<IRelationName, IRelation.Immutable<T>> relationsBuilder = Map.Transient.of();
            for(Entry<IRelationName, IRelation.Transient<T>> entry : relations.entrySet()) {
                relationsBuilder.__put(entry.getKey(), entry.getValue().freeze());
            }
            return new Relations.Immutable<>(relationsBuilder.freeze(), variantMatchers);
        }

        public static <T> Relations.Transient<T> of(Map<IRelationName, RelationDescription> relations,
                SetMultimap.Immutable<IRelationName, IVariantMatcher<T>> variantMatchers) {
            Map.Transient<IRelationName, IRelation.Transient<T>> relationsBuilder = Map.Transient.of();
            for(Entry<IRelationName, RelationDescription> entry : relations.entrySet()) {
                relationsBuilder.__put(entry.getKey(), Relation.Transient.of(entry.getValue()));
            }
            return new Relations.Transient<>(relationsBuilder.freeze(), variantMatchers);
        }

    }

}