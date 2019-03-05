package mb.nabl2.util.collections;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Queues;

/**
 * A deque with fast membership tests.
 */
public class IndexedDeque<E> implements Deque<E> {

    private final Deque<E> deque;
    private final Multiset<E> index;

    public IndexedDeque() {
        this(Queues.newArrayDeque());
    }

    public IndexedDeque(Deque<E> deque) {
        this.deque = deque;
        this.index = HashMultiset.create();
    }

    @Override
    public void addFirst(E e) {
        deque.addFirst(e);
        index.add(e);
    }

    @Override
    public boolean isEmpty() {
        return deque.isEmpty();
    }

    @Override
    public void addLast(E e) {
        deque.addLast(e);
        index.add(e);
    }

    @Override
    public Object[] toArray() {
        return deque.toArray();
    }

    @Override
    public boolean offerFirst(E e) {
        if(deque.offerFirst(e)) {
            index.add(e);
            return true;
        }
        return false;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return deque.toArray(a);
    }

    @Override
    public boolean offerLast(E e) {
        if(deque.offerLast(e)) {
            index.add(e);
            return true;
        }
        return false;
    }

    @Override
    public E removeFirst() {
        final E elem = deque.removeFirst();
        index.remove(elem);
        return elem;
    }

    @Override
    public E removeLast() {
        final E elem = deque.removeLast();
        index.remove(elem);
        return elem;
    }

    @Override
    public E pollFirst() {
        final E elem = deque.pollFirst();
        if(elem != null) {
            index.remove(elem);
        }
        return elem;
    }

    @Override
    public E pollLast() {
        final E elem = deque.pollLast();
        if(elem != null) {
            index.remove(elem);
        }
        return elem;
    }

    @Override
    public E getFirst() {
        return deque.getFirst();
    }

    @Override
    public E getLast() {
        return deque.getLast();
    }

    @Override
    public E peekFirst() {
        return deque.peekFirst();
    }

    @Override
    public E peekLast() {
        return deque.peekLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        if(deque.removeFirstOccurrence(o)) {
            index.remove(o);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        if(deque.removeLastOccurrence(o)) {
            index.remove(o);
            return true;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return index.containsAll(c);
    }

    @Override
    public boolean add(E e) {
        if(deque.add(e)) {
            index.add(e);
            return true;
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if(deque.addAll(c)) {
            index.addAll(c);
            return true;
        }
        return false;
    }

    @Override
    public boolean offer(E e) {
        if(deque.offer(e)) {
            index.add(e);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if(deque.removeAll(c)) {
            index.removeAll(c);
            return true;
        }
        return false;
    }

    @Override
    public E remove() {
        final E elem = deque.remove();
        index.remove(elem);
        return elem;
    }

    @Override
    public E poll() {
        final E elem = deque.poll();
        if(elem != null) {
            index.remove(elem);
        }
        return elem;
    }

    @Override
    public E element() {
        return deque.element();
    }

    @Override
    public E peek() {
        return deque.peek();
    }

    @Override
    public void push(E e) {
        deque.push(e);
        index.add(e);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if(deque.retainAll(c)) {
            index.retainAll(c);
            return true;
        }
        return false;
    }

    @Override
    public E pop() {
        final E elem = deque.pop();
        index.remove(elem);
        return elem;
    }

    @Override
    public boolean remove(Object o) {
        if(deque.remove(o)) {
            index.remove(o);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        deque.clear();
        index.clear();
    }

    @Override public boolean equals(Object o) {
        return deque.equals(o);
    }

    @Override
    public boolean contains(Object o) {
        return index.contains(o);
    }

    @Override
    public int size() {
        return deque.size();
    }

    @Override
    public Iterator<E> iterator() {
        final Iterator<E> it = deque.iterator();
        return new Iterator<E>() {

            private E current = null;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public E next() {
                return(current = it.next());
            }

            @Override
            public void remove() {
                it.remove();
                index.remove(current);
            }

        };
    }

    @Override
    public Iterator<E> descendingIterator() {
        final Iterator<E> it = deque.descendingIterator();
        return new Iterator<E>() {

            private E current = null;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public E next() {
                return(current = it.next());
            }

            @Override
            public void remove() {
                it.remove();
                index.remove(current);
            }

        };
    }

    @Override public int hashCode() {
        return deque.hashCode();
    }

    @Override public String toString() {
        return deque.toString();
    }

}