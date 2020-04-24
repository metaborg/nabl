package mb.statix.generator;

import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.solver.IConstraint;

public interface SearchLogger<I, O> {

    void init(long seed, SearchStrategy<I, O> strategy, Iterable<IConstraint> constraint);

    void success(SearchNode<O> n);

    void failure(SearchNodes<?> nodes);

    @SuppressWarnings("rawtypes")
    SearchLogger<?, ?> NOOP = new SearchLogger() {
        @Override
        public void success(SearchNode n) {

        }

        @Override
        public void failure(SearchNodes nodes) {

        }

        @Override
        public void init(long seed, SearchStrategy strategy, Iterable constraint) {

        }

    };

}