package mb.statix.generator.util;

import static mb.statix.generator.util.StreamUtil.lazyFlatMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Set;

public class Subsets<E> {

    private final List<E> elementList;
    private final Set.Immutable<E> elementSet;

    private Subsets(Iterable<? extends E> elements) {
        this.elementList = ImList.Immutable.copyOf(elements);
        this.elementSet = CapsuleUtil.toSet(elements);
    }

    public Stream<Map.Entry<Set.Immutable<E>, Set.Immutable<E>>> enumerate(int size, Random rnd) {
        if(size < 0 || size > elementList.size()) {
            return Stream.empty();
        }
        return enumerate(size, 0, elementList.size() - size, rnd)
                .map(subset -> Tuple2.of(subset, elementSet.__removeAll(subset)));
    }

    private Stream<Set.Immutable<E>> enumerate(int n, int start, int end, Random rnd) {
        if(n == 0) {
            return Stream.of(CapsuleUtil.immutableSet());
        }
        final List<Integer> indices =
                IntStream.range(start, end + 1).boxed().collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(indices, rnd);
        return lazyFlatMap(indices.stream(), index -> {
            return enumerate(n - 1, index, end + 1, rnd).map(subset -> subset.__insert(elementList.get(index)));
        });
    }

    public static <E> Subsets<E> of(Iterable<? extends E> elements) {
        return new Subsets<>(elements);
    }

}