package org.metaborg.meta.nabl2.util.collections;

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

    public void addFirst(E e) {
        deque.addFirst(e);
        index.add(e);
    }

    public boolean isEmpty() {
        return deque.isEmpty();
    }

    public void addLast(E e) {
        deque.addLast(e);
        index.add(e);
    }

    public Object[] toArray() {
        return deque.toArray();
    }

    public boolean offerFirst(E e) {
        if(deque.offerFirst(e)) {
            index.add(e);
            return true;
        }
        return false;
    }

    public <T> T[] toArray(T[] a) {
        return deque.toArray(a);
    }

    public boolean offerLast(E e) {
        if(deque.offerLast(e)) {
            index.add(e);
            return true;
        }
        return false;
    }

    public E removeFirst() {
        final E elem = deque.removeFirst();
        index.remove(elem);
        return elem;
    }

    public E removeLast() {
        final E elem = deque.removeLast();
        index.remove(elem);
        return elem;
    }

    public E pollFirst() {
        final E elem = deque.pollFirst();
        if(elem != null) {
            index.remove(elem);
        }
        return elem;
    }

    public E pollLast() {
        final E elem = deque.pollLast();
        if(elem != null) {
            index.remove(elem);
        }
        return elem;
    }

    public E getFirst() {
        return deque.getFirst();
    }

    public E getLast() {
        return deque.getLast();
    }

    public E peekFirst() {
        return deque.peekFirst();
    }

    public E peekLast() {
        return deque.peekLast();
    }

    public boolean removeFirstOccurrence(Object o) {
        if(deque.removeFirstOccurrence(o)) {
            index.remove(o);
            return true;
        }
        return false;
    }

    public boolean removeLastOccurrence(Object o) {
        if(deque.removeLastOccurrence(o)) {
            index.remove(o);
            return true;
        }
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        return index.containsAll(c);
    }

    public boolean add(E e) {
        if(deque.add(e)) {
            index.add(e);
            return true;
        }
        return false;
    }

    public boolean addAll(Collection<? extends E> c) {
        if(deque.addAll(c)) {
            index.addAll(c);
            return true;
        }
        return false;
    }

    public boolean offer(E e) {
        if(deque.offer(e)) {
            index.add(e);
            return true;
        }
        return false;
    }

    public boolean removeAll(Collection<?> c) {
        if(deque.removeAll(c)) {
            index.removeAll(c);
            return true;
        }
        return false;
    }

    public E remove() {
        final E elem = deque.remove();
        index.remove(elem);
        return elem;
    }

    public E poll() {
        final E elem = deque.poll();
        if(elem != null) {
            index.remove(elem);
        }
        return elem;
    }

    public E element() {
        return deque.element();
    }

    public E peek() {
        return deque.peek();
    }

    public void push(E e) {
        deque.push(e);
        index.add(e);
    }

    public boolean retainAll(Collection<?> c) {
        if(deque.retainAll(c)) {
            index.retainAll(c);
            return true;
        }
        return false;
    }

    public E pop() {
        final E elem = deque.pop();
        index.remove(elem);
        return elem;
    }

    public boolean remove(Object o) {
        if(deque.remove(o)) {
            index.remove(o);
            return true;
        }
        return false;
    }

    public void clear() {
        deque.clear();
        index.clear();
    }

    @Override public boolean equals(Object o) {
        return deque.equals(o);
    }

    public boolean contains(Object o) {
        return index.contains(o);
    }

    public int size() {
        return deque.size();
    }

    public Iterator<E> iterator() {
        final Iterator<E> it = deque.iterator();
        return new Iterator<E>() {

            private E current = null;

            public boolean hasNext() {
                return it.hasNext();
            }

            public E next() {
                return(current = it.next());
            }

            public void remove() {
                it.remove();
                index.remove(current);
            }

        };
    }

    public Iterator<E> descendingIterator() {
        final Iterator<E> it = deque.descendingIterator();
        return new Iterator<E>() {

            private E current = null;

            public boolean hasNext() {
                return it.hasNext();
            }

            public E next() {
                return(current = it.next());
            }

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