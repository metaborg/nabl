package mb.statix.generator.strategy;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;


/**
 * Repeatedly applies a strategy until it fails.
 */
public final class Fix2 extends SearchStrategy<SearchState, SearchState> {

    private final SearchStrategy<SearchState, SearchState> search;

    public Fix2(SearchStrategy<SearchState, SearchState> search) {
        this.search = search;
    }

    @Override protected SearchNodes<SearchState> doApply(SearchContext ctx, SearchNode<SearchState> node) {
        final List<SearchNode<SearchState>> results = new ArrayList<>();
        final Deque<SearchNode<SearchState>> stack = new LinkedList<>();
        stack.push(node);
        while (!stack.isEmpty()) {
            final SearchNode<SearchState> nextNode = stack.pop();
            SearchNodes<SearchState> newNodes = search.apply(ctx, nextNode);
            List<SearchNode<SearchState>> newNodesList = newNodes.nodes().collect(Collectors.toList());
            if (newNodesList.isEmpty()) {
                // The strategy failed on nextNode, so it is one of the results
                results.add(nextNode);
            } else {
                // Continue
                for (SearchNode<SearchState> newNode : newNodesList) {
                    stack.push(newNode);
                }
            }
        }
        return SearchNodes.of(node, () -> "fix2(???)", results);
    }

    @Override public String toString() {
        return "fix2(" + search + ")";
    }

}