package mb.nabl2.relations.variants;

import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;

import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.Lists;

import io.usethesource.capsule.Set;
import mb.nabl2.relations.IRelation;
import mb.nabl2.relations.RelationDescription;
import mb.nabl2.relations.RelationException;
import mb.nabl2.relations.impl.Relation;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.IRelation2;

public abstract class VariantRelation<T> implements IVariantRelation<T> {

    protected VariantRelation() {
    }

    protected abstract IRelation<T> baseRelation();

    @Override public boolean isEmpty() {
        return baseRelation().isEmpty();
    }

    @Override public Set.Immutable<T> smaller(T t) {
        return baseRelation().smaller(t);
    }

    @Override public Set.Immutable<T> larger(T t) {
        return baseRelation().larger(t);
    }

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
        return baseRelation().contains(t1, t2);
    }

    protected void canAddOrThrow(T t1, T t2) throws RelationException {
        for(IVariantMatcher<T> matcher : getVariantMatchers()) {
            if(Optionals.lift(matcher.match(t1), matcher.match(t2), (a1, a2) -> true).isPresent()) {
                throw new InstantiatedVariantsException(
                        "Cannot add instantiated pair of variant constructors to the relation: " + t1 + " and " + t2);
            }
        }
    }

    @Override public Optional<T> leastUpperBound(T t1, T t2) {
        for(IVariantMatcher<T> matcher : getVariantMatchers()) {
            Optional<Optional<T>> contains = Optionals.lift(matcher.match(t1), matcher.match(t2), (args1, args2) -> {
                return Optionals.when(args1.size() == args2.size()).flatMap(eq -> {
                    return Optionals.sequence(Iterables2.zip(args1, args2, (arg1, arg2) -> {
                        T argt1 = arg1.getValue();
                        T argt2 = arg2.getValue();
                        return arg1.getVariance().match(IVariance.<Optional<T>>cases(
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
        return baseRelation().leastUpperBound(t1, t2);
    }

    @Override public Optional<T> greatestLowerBound(T t1, T t2) {
        for(IVariantMatcher<T> matcher : getVariantMatchers()) {
            Optional<Optional<T>> contains = Optionals.lift(matcher.match(t1), matcher.match(t2), (args1, args2) -> {
                return Optionals.when(args1.size() == args2.size()).flatMap(eq -> {
                    return Optionals.sequence(Iterables2.zip(args1, args2, (arg1, arg2) -> {
                        T argt1 = arg1.getValue();
                        T argt2 = arg2.getValue();
                        return arg1.getVariance().match(IVariance.<Optional<T>>cases(
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
        return baseRelation().greatestLowerBound(t1, t2);
    }

    @Override public Stream<Tuple2<T, T>> stream() {
        return entries().stream();
    }

    public static class Immutable<T> extends VariantRelation<T> implements IVariantRelation.Immutable<T>, Serializable {
        private static final long serialVersionUID = 42L;

        private final VariantRelationDescription<T> description;
        private final IRelation.Immutable<T> baseRelation;

        protected Immutable(VariantRelationDescription<T> description, IRelation.Immutable<T> baseRelation) {
            this.description = description;
            this.baseRelation = baseRelation;
        }

        @Override protected IRelation.Immutable<T> baseRelation() {
            return baseRelation;
        }

        @Override public RelationDescription getDescription() {
            return description.relationDescription();
        }

        @Override public Iterable<IVariantMatcher<T>> getVariantMatchers() {
            return description.variantMatchers();
        }

        @Override public IRelation2<T, T> entries() {
            return baseRelation.entries();
        }

        @Override public IVariantRelation.Transient<T> melt() {
            return new VariantRelation.Transient<>(description, baseRelation.melt());
        }

        public static <T> IVariantRelation.Immutable<T> of(VariantRelationDescription<T> description) {
            return new VariantRelation.Immutable<>(description,
                    Relation.Immutable.of(description.relationDescription()));
        }

    }


    public static class Transient<T> extends VariantRelation<T> implements IVariantRelation.Transient<T> {

        private final VariantRelationDescription<T> description;
        private final IRelation.Transient<T> baseRelation;

        protected Transient(VariantRelationDescription<T> description, IRelation.Transient<T> baseRelation) {
            this.description = description;
            this.baseRelation = baseRelation;
        }

        @Override protected IRelation<T> baseRelation() {
            return baseRelation;
        }

        @Override public RelationDescription getDescription() {
            return description.relationDescription();
        }

        @Override public Iterable<IVariantMatcher<T>> getVariantMatchers() {
            return description.variantMatchers();
        }

        @Override public IRelation2<T, T> entries() {
            return baseRelation.entries();
        }

        @Override public boolean add(T t1, T t2) throws RelationException {
            canAddOrThrow(t1, t2);
            return baseRelation.add(t1, t2);
        }

        @Override public IVariantRelation.Immutable<T> freeze() {
            return new VariantRelation.Immutable<>(description, baseRelation.freeze());
        }

        public static <T> IVariantRelation.Transient<T> of(VariantRelationDescription<T> description) {
            return new VariantRelation.Transient<>(description,
                    Relation.Transient.of(description.relationDescription()));
        }

    }

}