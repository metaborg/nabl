package mb.statix.random.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import com.google.common.collect.Lists;

import io.usethesource.capsule.Set;
import mb.nabl2.util.CapsuleUtil;

public class SetElementEnum<E> implements Iterable<SetElementEnum.Entry<E>> {

    private final Set.Immutable<E> elements;

    public SetElementEnum(Iterable<? extends E> elements) {
        this.elements = CapsuleUtil.toSet(elements);
    }

    @Override public Iterator<Entry<E>> iterator() {
        return new Iterator<Entry<E>>() {

            private final Iterator<E> it;
            {
                final LinkedList<E> es = Lists.newLinkedList(elements);
                Collections.shuffle(es);
                it = es.iterator();
            }

            @Override public boolean hasNext() {
                return it.hasNext();
            }

            @Override public Entry<E> next() {
                final E focus = it.next();
                final Set<E> others = elements.__remove(focus);
                return new Entry<E>() {

                    @Override public E getFocus() {
                        return focus;
                    }

                    @Override public Set<E> getOthers() {
                        return others;
                    }

                };
            }

        };
    }

    public interface Entry<E> {

        E getFocus();

        Set<E> getOthers();

    }

}