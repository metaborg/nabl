package mb.nabl2.util.collections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ConsList<E> implements Iterable<E> {

    private final E head;
    private final ConsList<E> tail;

    private ConsList(E head, ConsList<E> tail) {
        this.head = head;
        this.tail = tail;
    }

    @Override public Iterator<E> iterator() {
        return new Iterator<E>() {

            private ConsList<E> current = ConsList.this;

            @Override public boolean hasNext() {
                return !current.isNil();
            }

            @Override public E next() {
                if(current.isNil()) {
                    throw new NoSuchElementException();
                }
                final E next = current.head;
                current = current.tail;
                return next;
            }
        };
    }

    public ConsList<E> prepend(E head) {
        return new ConsList<>(head, this);
    }

    public ConsList<E> prepend(ConsList<E> init) {
        return init.append(this);
    }

    public ConsList<E> append(ConsList<E> tail) {
        ConsList<E> list = tail;
        for(E head : this) {
            list = tail.prepend(head);
        }
        return list;
    }

    public ConsList<E> tail() {
        return isNil() ? this : tail;
    }

    private boolean isNil() {
        return head == null;
    }

    public static <E> ConsList<E> nil() {
        return new ConsList<>(null, null);
    }

    public static <E> ConsList<E> of(E e) {
        return new ConsList<>(e, nil());
    }

    @SafeVarargs public static <E> ConsList<E> of(E... es) {
        return of(Arrays.asList(es));
    }

    public static <E> ConsList<E> of(Iterable<E> es) {
        if(es instanceof ConsList) {
            return (ConsList<E>) es;
        }
        ConsList<E> list = nil();
        for(E e : es) {
            list = list.prepend(e);
        }
        return list;
    }

}