package mb.statix.random.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Set;
import mb.nabl2.util.ImmutableTuple2;

public class WeightedDrawSet<E> {

    private final List<E> elementList;
    private final Set.Immutable<E> elementSet;

    private WeightedDrawSet(Map<? extends E, Integer> elements) {
        final ImmutableList.Builder<E> elementList = ImmutableList.builder();
        final Set.Transient<E> elementSet = Set.Transient.of();
        elements.forEach((e, w) -> {
            elementSet.__insert(e);
            IntStream.range(0, w).forEach(i -> {
                elementList.add(e);
            });
        });
        this.elementList = elementList.build();
        this.elementSet = elementSet.freeze();
    }

    public Stream<Map.Entry<E, Set.Immutable<E>>> draw(Random rnd) {
        if(elementList.isEmpty()) {
            return Stream.empty();
        }
        return rnd.ints(0, elementList.size()).mapToObj(idx -> {
            final E e = elementList.get(idx);
            final Set.Immutable<E> es = elementSet.__remove(e);
            return ImmutableTuple2.of(e, es);
        });
    }

    public Stream<Map.Entry<E, Set.Immutable<E>>> enumerate(Random rnd) {
        final List<E> elementList = Lists.newArrayList(this.elementList);
        return Streams.stream(new Iterator<Map.Entry<E, Set.Immutable<E>>>() {

            @Override public boolean hasNext() {
                return !elementList.isEmpty();
            }

            @Override public Map.Entry<E, Set.Immutable<E>> next() {
                final int idx = rnd.nextInt(elementList.size());
                final E e = elementList.get(idx);
                elementList.removeIf(ee -> ee.equals(e));
                final Set.Immutable<E> es = elementSet.__remove(e);
                return ImmutableTuple2.of(e, es);
            }

        });
    }

    public static <E> WeightedDrawSet<E> of(Map<? extends E, Integer> elements) {
        return new WeightedDrawSet<>(elements);
    }

    public static <E> WeightedDrawSet<E> of(Iterable<? extends E> elements) {
        return new WeightedDrawSet<>(Iterables2.stream(elements).collect(Collectors.toMap(e -> e, e -> 1)));
    }

}