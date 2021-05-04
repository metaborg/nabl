package mb.nabl2.util.collections;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.tuple.Tuple3;

public interface IProperties<I, K, V> {

    Set<I> getIndices();

    Set<K> getDefinedKeys(I index);

    Optional<V> getValue(I index, K key);

    boolean contains(I index);

    boolean contains(I index, K key);

    public Stream<Tuple3<I, K, V>> stream();

    interface Immutable<I, K, V> extends IProperties<I, K, V> {

        IProperties.Transient<I, K, V> melt();

    }

    interface Transient<I, K, V> extends IProperties<I, K, V> {

        Optional<V> putValue(I index, K key, V value);

        default boolean putAll(IProperties<I, K, V> other) {
            return other.stream().map(ikv -> ikv.apply(this::putValue).isPresent()).reduce(false, Boolean::logicalOr);
        }

        boolean mapValues(Function1<V, V> mapper);

        IProperties.Immutable<I, K, V> freeze();

    }

}