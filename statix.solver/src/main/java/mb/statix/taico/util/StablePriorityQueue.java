package mb.statix.taico.util;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

public class StablePriorityQueue<E> extends AbstractQueue<E> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private PriorityQueue<Entry<E>> queue;
    private final Comparator<? super E> comparator;
    protected volatile int entryCounter;
    
    public StablePriorityQueue() {
        this((Comparator<? super E>) null);
    }

    /**
     * Creates a {@code StablePriorityQueue} with the specified initial
     * capacity that orders its elements according to their
     * {@linkplain Comparable natural ordering}.
     *
     * @param initialCapacity the initial capacity for this priority queue
     * @throws IllegalArgumentException if {@code initialCapacity} is less
     *         than 1
     */
    public StablePriorityQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    /**
     * Creates a {@code StablePriorityQueue} with the default initial capacity and
     * whose elements are ordered according to the specified comparator.
     *
     * @param  comparator the comparator that will be used to order this
     *         priority queue.  If {@code null}, the {@linkplain Comparable
     *         natural ordering} of the elements will be used.
     * @since 1.8
     */
    public StablePriorityQueue(Comparator<? super E> comparator) {
        this.queue = new PriorityQueue<>();
        this.comparator = comparator;
    }

    /**
     * Creates a {@code StablePriorityQueue} with the specified initial capacity
     * that orders its elements according to the specified comparator.
     *
     * @param  initialCapacity the initial capacity for this priority queue
     * @param  comparator the comparator that will be used to order this
     *         priority queue.  If {@code null}, the {@linkplain Comparable
     *         natural ordering} of the elements will be used.
     * @throws IllegalArgumentException if {@code initialCapacity} is
     *         less than 1
     */
    public StablePriorityQueue(int initialCapacity,
                         Comparator<? super E> comparator) {
        this.queue = new PriorityQueue<>(initialCapacity);
        this.comparator = comparator;
    }
    
    /**
     * Creates a {@code StablePriorityQueue} and adds all the given elements to it.
     * 
     * @param elements
     *      the elements to add to the queue
     */
    public StablePriorityQueue(Collection<? extends E> elements) {
        this(elements.size(), null);
        addAll(elements);
    }
    
    @Override
    public int size() {
        return queue.size();
    }
    
    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    @Override
    public E peek() {
        Entry<E> entry = queue.peek();
        return entry == null ? null : entry.value;
    }
    
    @Override
    public E poll() {
        Entry<E> entry = queue.poll();
        return entry == null ? null : entry.value;
    }
    
    @Override
    public E element() {
        return queue.element().value;
    }
    
    @Override
    public boolean offer(E e) {
        return queue.offer(new Entry<>(e));
    }
    
    @Override
    public E remove() {
        return queue.remove().value;
    }
    
    @Override
    public boolean add(E e) {
        return queue.add(new Entry<>(e));
    }
    
    @Override
    public boolean remove(Object o) {
        return queue.remove(new Entry<>(o, -1));
    }
    
    @Override
    public void clear() {
        queue.clear();
    }
    
    @Override
    public boolean contains(Object o) {
        return queue.contains(new Entry<>(o, -1));
    }
    
    /**
     * @see PriorityQueue#comparator()
     */
    public Comparator<? super E> comparator() {
        return comparator;
    }
    
    @Override
    public Iterator<E> iterator() {
        return new SPQIterator(queue.iterator());
    }
    
    private class SPQIterator implements Iterator<E> {
        private final Iterator<Entry<E>> it;
        private SPQIterator(Iterator<Entry<E>> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public E next() {
            return it.next().value;
        }
        
        @Override
        public void remove() {
            it.remove();
        }
    }
    
    /**
     * Class to represent an entry in this priority queue.
     * 
     * @param <T>
     *      the type parameter is used to avoid typing conflicts with contains and remove
     */
    private class Entry<T> implements Comparable<Entry<T>> {
        private final int order;
        private final T value;
        
        public Entry(T value) {
            this(value, entryCounter++);
        }
        
        public Entry(T value, int order) {
            this.value = value;
            this.order = order;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public int compareTo(Entry<T> o) {
            final int c;
            if (comparator == null) {
                c = ((Comparable<? super T>) value).compareTo(o.value);
            } else {
                c = comparator.compare((E) value, (E) o.value);
            }
            return c != 0 ? c : Integer.compare(order, o.order);
        }
        
        @Override
        public String toString() {
            return value.toString();
        }
        
        @Override
        public int hashCode() {
            return value.hashCode();
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof StablePriorityQueue.Entry)) return false;
            return value.equals(((Entry<?>) obj).value);
        }
    }
}
