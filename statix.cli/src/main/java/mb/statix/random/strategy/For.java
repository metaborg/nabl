package mb.statix.random.strategy;

import java.util.Optional;

import org.metaborg.core.MetaborgException;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchNodes;
import mb.statix.random.SearchStrategy;

final class For<I, O> extends SearchStrategy<I, O> {
    private final SearchStrategy<I, O> s;
    private final int n;

    For(int n, SearchStrategy<I, O> s) {
        this.s = s;
        this.n = n;
    }

    @Override public SearchNodes<O> doApply(SearchContext ctx, I input, SearchNode<?> parent) {
        return new SearchNodes<O>() {

            private int count = n;
            private SearchNodes<O> nodes = null;

            @Override public Optional<SearchNode<O>> next() throws MetaborgException, InterruptedException {
                if(count <= 0) {
                    return Optional.empty();
                }
                if(nodes == null) {
                    count--;
                    nodes = s.apply(ctx, input, parent);
                }
                final Optional<SearchNode<O>> next = nodes.next();
                if(!next.isPresent()) {
                    nodes = null;
                    return next();
                }
                return next;
            }

        };

    }

    @Override public String toString() {
        return "for(" + n + ", " + s.toString() + ")";
    }

}