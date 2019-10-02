package mb.statix.generator;

import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.solver.IConstraint;

public interface SearchLogger {

    void init(long seed, SearchStrategy<?, ?> strategy, Iterable<IConstraint> constraint);

    void success(SearchNode<SearchState> n);

    void failure(SearchNodes<?> nodes);

    final SearchLogger NOOP = new SearchLogger() {

        @Override public void init(long seed, SearchStrategy<?, ?> strategy, Iterable<IConstraint> constraint) {
        }

        @Override public void success(SearchNode<SearchState> n) {
        }

        @Override public void failure(SearchNodes<?> nodes) {
        }

    };

}