package mb.statix.random;

import mb.statix.random.nodes.SearchElement;
import mb.statix.random.nodes.SearchNode;
import mb.statix.solver.IConstraint;

public interface SearchLogger {

    void init(long seed, SearchStrategy<?, ?> strategy, Iterable<IConstraint> constraint);

    void success(SearchNode<SearchState> n);

    void failure(SearchElement e);

    final SearchLogger NOOP = new SearchLogger() {

        @Override public void init(long seed, SearchStrategy<?, ?> strategy, Iterable<IConstraint> constraint) {
        }

        @Override public void success(SearchNode<SearchState> n) {
        }

        @Override public void failure(SearchElement e) {
        }

    };

}