package org.metaborg.meta.nabl2.relations.variants;

import java.io.Serializable;
import java.util.Optional;

import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.relations.terms.Relation;
import org.metaborg.meta.nabl2.util.collections.HashTrieRelation2;
import org.metaborg.meta.nabl2.util.collections.IRelation2;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.Lists;

public abstract class VariantRelation<T> extends Relation<T> implements IVariantRelation<T> {

    @Override public boolean contains(T t1, T t2) {
        for(IVariantMatcher<T> matcher : getVariantMatchers()) {
            Optional<Boolean> contains = Optionals.lift(matcher.match(t1), matcher.match(t2), (args1, args2) -> {
                return (args1.size() == args2.size())
                        && Iterables2.stream(Iterables2.zip(args1, args2, (arg1, arg2) -> {
                            T argt1 = arg1.getValue();
                            T argt2 = arg2.getValue();
                    // @formatter:off
                    return arg1.getVariance().match(IVariance.cases(
                        () -> argt1.equals(argt2),
                        r -> contains(argt1, argt2),
                        r -> contains(argt2, argt1)
                    ));
                    // @formatter:on
                        })).allMatch(c -> c);
            });
            if(contains.isPresent()) {
                return contains.get();
            }
        }
        return super.contains(t1, t2);
    }

    @Override public Optional<T> leastUpperBound(T t1, T t2) {
        for(IVariantMatcher<T> matcher : getVariantMatchers()) {
            Optional<Optional<T>> contains = Optionals.lift(matcher.match(t1), matcher.match(t2), (args1, args2) -> {
                return Optionals.when(args1.size() == args2.size()).flatMap(eq -> {
                    return Optionals.sequence(Iterables2.zip(args1, args2, (arg1, arg2) -> {
                        T argt1 = arg1.getValue();
                        T argt2 = arg2.getValue();
                        return arg1.getVariance()
                                .match(IVariance.<Optional<T>>cases(
                            // @formatter:off
                            () -> argt1.equals(argt2) ? Optional.of(argt1) : Optional.empty(),
                            r -> leastUpperBound(argt1, argt2),
                            r -> greatestLowerBound(argt1, argt2)
                            // @formatter:on
                        ));
                    })).map(args -> matcher.build(Lists.newArrayList(args)));
                });
            });
            if(contains.isPresent()) {
                return contains.get();
            }
        }
        return super.leastUpperBound(t1, t2);
    }

    @Override public Optional<T> greatestLowerBound(T t1, T t2) {
        for(IVariantMatcher<T> matcher : getVariantMatchers()) {
            Optional<Optional<T>> contains = Optionals.lift(matcher.match(t1), matcher.match(t2), (args1, args2) -> {
                return Optionals.when(args1.size() == args2.size()).flatMap(eq -> {
                    return Optionals.sequence(Iterables2.zip(args1, args2, (arg1, arg2) -> {
                        T argt1 = arg1.getValue();
                        T argt2 = arg2.getValue();
                        return arg1.getVariance()
                                .match(IVariance.<Optional<T>>cases(
                            // @formatter:off
                            () -> argt1.equals(argt2) ? Optional.of(argt1) : Optional.empty(),
                            r -> greatestLowerBound(argt1, argt2),
                            r -> leastUpperBound(argt1, argt2)
                            // @formatter:on
                        ));
                    })).map(args -> matcher.build(Lists.newArrayList(args)));
                });
            });
            if(contains.isPresent()) {
                return contains.get();
            }
        }
        return super.greatestLowerBound(t1, t2);
    }

    @Override protected void canAddOrThrow(T t1, T t2) throws RelationException {
        for(IVariantMatcher<T> matcher : getVariantMatchers()) {
            if(Optionals.lift(matcher.match(t1), matcher.match(t2), (a1, a2) -> true).isPresent()) {
                throw new InstantiatedVariantsException(
                        "Cannot add instantiated pair of variant constructors to the relation: " + t1 + " and " + t2);
            }
        }
        super.canAddOrThrow(t1, t2);
    }

    public static class Immutable<T> extends Relation.Immutable<T>
            implements IVariantRelation.Immutable<T>, Serializable {
        private static final long serialVersionUID = 42L;

        private final VariantRelationDescription<T> description;

        protected Immutable(VariantRelationDescription<T> description, IRelation2.Immutable<T, T> entries) {
            super(description.relationDescription(), entries);
            this.description = description;
        }

        @Override public Iterable<IVariantMatcher<T>> getVariantMatchers() {
            return description.variantMatchers();
        }

        @Override public IVariantRelation.Transient<T> melt() {
            return new VariantRelation.Transient<>(description, entries.melt());
        }

        public static <T> IVariantRelation.Immutable<T> of(VariantRelationDescription<T> description) {
            return new VariantRelation.Immutable<>(description, HashTrieRelation2.Immutable.of());
        }

    }


    public static class Transient<T> extends Relation.Transient<T> implements IVariantRelation.Transient<T> {

        private final VariantRelationDescription<T> description;

        protected Transient(VariantRelationDescription<T> description, IRelation2.Transient<T, T> entries) {
            super(description.relationDescription(), entries);
            this.description = description;
        }

        @Override public Iterable<IVariantMatcher<T>> getVariantMatchers() {
            return description.variantMatchers();
        }

        @Override public boolean add(T t1, T t2) throws RelationException {
            canAddOrThrow(t1, t2);
            return super.add(t1, t2);
        }

        @Override public IVariantRelation.Immutable<T> freeze() {
            return new VariantRelation.Immutable<>(description, entries.freeze());
        }

        public static <T> IVariantRelation.Transient<T> of(VariantRelationDescription<T> description) {
            return new VariantRelation.Transient<>(description, HashTrieRelation2.Transient.of());
        }

    }


    public static <T> IVariantRelation.Transient<T> extend(IVariantRelation.Transient<T> rel1, IVariantRelation<T> rel2)
            throws RelationException {
        return new Extension<>(rel1, rel2);
    }

    private static class Extension<T> extends Relation.Extension<T> implements IVariantRelation.Transient<T> {

        private final IVariantRelation.Transient<T> rel1;
        private final IVariantRelation<T> rel2;

        protected Extension(IVariantRelation.Transient<T> rel1, IVariantRelation<T> rel2) throws RelationException {
            super(rel1, rel2);
            this.rel1 = rel1;
            this.rel2 = rel2;
        }

        @Override public Iterable<IVariantMatcher<T>> getVariantMatchers() {
            return rel1.getVariantMatchers();
        }

        @Override public boolean add(T t1, T t2) throws RelationException {
            canAddOrThrow(t1, t2);
            return rel1.add(t1, t2);
        }

        @Override public IVariantRelation.Immutable<T> freeze() {
            return rel1.freeze();
        }

    }

}