package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.util.collections.HashTrieFunction;
import org.metaborg.meta.nabl2.util.collections.HashTrieRelation3;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IInverseFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.tuples.ImmutableScopeLabelOccurrence;

import io.usethesource.capsule.BinaryRelation;

public class CollectionConverter {

    // TODO: release Capsule and change input type to BinaryRelation.Immutable
    public static final <T, U> IFunction<T, U> relationToHashFunction(BinaryRelation<T, U> input) {
        final IFunction.Transient<T, U> output = HashTrieFunction.Transient.of();
        input.entryIterator().forEachRemaining(entry -> output.put(entry.getKey(), entry.getValue()));
        return output;
    }
    
    public static final <T, U> BinaryRelation.Immutable<T, U> hashFunctionToRelation(IInverseFunction<T, U> input) {
        return (BinaryRelation.Immutable<T, U>) hashFunctionToRelation(input.inverse()).inverse();
    }
    
    public static final <T, U> BinaryRelation.Immutable<T, U> hashFunctionToRelation(IFunction<T, U> input) {
        final BinaryRelation.Transient<T, U> output = BinaryRelation.Transient.of();
        input.keySet().forEach(key -> output.__insert(key, input.get(key).get()));
        return output.freeze();
    }

    public static final <T, U, V> IRelation3<T, U, V> liftHashFunctionToRelation(IInverseFunction<T, V> input,
            U intermediate) {
        return liftHashFunctionToRelation(input.inverse(), intermediate).inverse();
    }
    
    public static final <T, U, V> IRelation3<T, U, V> liftHashFunctionToRelation(IFunction<T, V> input,
            U intermediate) {
        final IRelation3.Transient<T, U, V> output = HashTrieRelation3.Transient.of();
        input.keySet().forEach(key -> output.put(key, intermediate, input.get(key).get()));
        return output;
    }
    
    public static final <T extends IScope, U extends ILabel, V extends IOccurrence> IRelation3.Immutable<T, U, V> union(IRelation3<T, U, V> one, IRelation3<T, U, V> two) {
        final IRelation3.Transient<T, U, V> result = HashTrieRelation3.Transient.of();

        one.stream(ImmutableScopeLabelOccurrence::of).iterator().forEachRemaining(tuple -> result.put(tuple.scope(), tuple.label(), tuple.occurrence()));
        two.stream(ImmutableScopeLabelOccurrence::of).iterator().forEachRemaining(tuple -> result.put(tuple.scope(), tuple.label(), tuple.occurrence()));

        return result.freeze();
    }
    
}
