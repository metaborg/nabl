package mb.nabl2.relations;

import java.util.Optional;
import java.util.stream.Stream;

import org.metaborg.util.collection.IRelation2;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Set;

public interface IRelation<T> {

    RelationDescription getDescription();

    boolean isEmpty();

    Set.Immutable<T> smaller(T t);

    Set.Immutable<T> larger(T t);

    boolean contains(T t1, T t2);

    IRelation2<T, T> entries();

    Optional<T> leastUpperBound(T t1, T t2);

    Optional<T> greatestLowerBound(T t1, T t2);

    Stream<Tuple2<T, T>> stream();

    interface Immutable<T> extends IRelation<T> {

        Transient<T> melt();

    }

    interface Transient<T> extends IRelation<T> {

        boolean add(T t1, T t2) throws RelationException;

        default boolean addAll(IRelation<T> other) throws RelationException {
            boolean change = false;
            for(Tuple2<T, T> pair : (Iterable<Tuple2<T, T>>) other.stream()::iterator) {
                change |= add(pair._1(), pair._2());
            }
            return change;
        }

        Immutable<T> freeze();

    }

}