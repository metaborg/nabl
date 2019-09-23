package mb.statix.random;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.metaborg.core.MetaborgException;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.ImmutableList;

public interface SearchNodes<O> {

    Optional<SearchNode<O>> next() throws MetaborgException, InterruptedException;

    default <X> SearchNodes<X> map(Function1<SearchNode<O>, SearchNode<X>> map) {
        final SearchNodes<O> outer = this;
        return new SearchNodes<X>() {
            @Override public Optional<SearchNode<X>> next() throws MetaborgException, InterruptedException {
                final SearchNode<O> next;
                if((next = outer.next().orElse(null)) == null) {
                    return Optional.empty();
                }
                return Optional.of(map.apply(next));
            }
        };
    }

    default SearchNodes<O> filter(Predicate1<O> filter) {
        final SearchNodes<O> outer = this;
        return new SearchNodes<O>() {
            @Override public Optional<SearchNode<O>> next() throws MetaborgException, InterruptedException {
                final SearchNode<O> next;
                if((next = outer.next().orElse(null)) == null) {
                    return Optional.empty();
                }
                if(filter.test(next.output())) {
                    return Optional.of(next);
                } else {
                    return next();
                }
            }
        };
    }

    default <X> SearchNodes<X> flatMap(Function1<SearchNode<O>, SearchNodes<X>> flatMap) {
        final SearchNodes<O> outer = this;
        return new SearchNodes<X>() {
            private SearchNodes<X> nexts = null;

            @Override public Optional<SearchNode<X>> next() throws MetaborgException, InterruptedException {
                if(nexts == null) {
                    final SearchNode<O> next;
                    if((next = outer.next().orElse(null)) == null) {
                        return Optional.empty();
                    }
                    nexts = flatMap.apply(next);
                }
                final SearchNode<X> next;
                if((next = nexts.next().orElse(null)) == null) {
                    nexts = null;
                    return next();
                } else {
                    return Optional.of(next);
                }
            }
        };
    }

    default SearchNodes<O> limit(int n) {
        final SearchNodes<O> outer = this;
        final AtomicInteger limit = new AtomicInteger(n);
        return new SearchNodes<O>() {
            @Override public Optional<SearchNode<O>> next() throws MetaborgException, InterruptedException {
                if(limit.getAndDecrement() <= 0) {
                    return Optional.empty();
                } else {
                    return outer.next();
                }
            }
        };
    }

    @SafeVarargs static <X> SearchNodes<X> of(SearchNode<X>... nodes) {
        final Iterator<SearchNode<X>> it = ImmutableList.copyOf(nodes).iterator();
        return new SearchNodes<X>() {
            @Override public Optional<SearchNode<X>> next() {
                if(it.hasNext()) {
                    return Optional.of(it.next());
                } else {
                    return Optional.empty();
                }
            }
        };
    }

    static <X> SearchNodes<X> of(Iterator<SearchNode<X>> nodes) {
        return new SearchNodes<X>() {
            @Override public Optional<SearchNode<X>> next() {
                if(nodes.hasNext()) {
                    return Optional.of(nodes.next());
                } else {
                    return Optional.empty();
                }
            }
        };
    }

    static <X> SearchNodes<X> of(Stream<SearchNode<X>> nodes) {
        return of(nodes.iterator());
    }

    static <X> SearchNodes<X> of(Iterable<SearchNode<X>> nodes) {
        return of(nodes.iterator());
    }

}