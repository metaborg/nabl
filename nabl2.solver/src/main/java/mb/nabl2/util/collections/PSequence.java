package mb.nabl2.util.collections;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import com.google.common.collect.ImmutableList;

public class PSequence<E> implements Iterable<E>, Serializable {

    private static final long serialVersionUID = 1L;

    private final ImmutableList<E> elems;

    private PSequence(ImmutableList<E> elems) {
        this.elems = elems;
    }

    public int size() {
        return elems.size();
    }

    public boolean isEmpty() {
        return elems.isEmpty();
    }

    public Iterator<E> iterator() {
        return elems.iterator();
    }

    public PSequence<E> init() {
        return new PSequence<>(elems.subList(0, elems.size() - 1));
    }

    public PSequence<E> tail() {
        return new PSequence<>(elems.subList(1, elems.size()));
    }

    public PSequence<E> prepend(E elem) {
        return new PSequence<>(ImmutableList.<E>builder().add(elem).addAll(this.elems).build());
    }

    public PSequence<E> prependAll(Collection<E> elems) {
        return new PSequence<>(ImmutableList.<E>builder().addAll(elems).addAll(this.elems).build());
    }

    public PSequence<E> append(E elem) {
        return new PSequence<>(ImmutableList.<E>builder().addAll(this.elems).add(elem).build());
    }

    public PSequence<E> appendAll(Iterable<E> elems) {
        return new PSequence<>(ImmutableList.<E>builder().addAll(this.elems).addAll(elems).build());
    }

    public static <E> PSequence<E> of() {
        return new PSequence<>(ImmutableList.of());
    }

    @SafeVarargs public static <E> PSequence<E> of(E... elems) {
        return of(Arrays.asList(elems));
    }

    public static <E> PSequence<E> of(Iterable<E> elems) {
        return new PSequence<>(ImmutableList.copyOf(elems));
    }

}