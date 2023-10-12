package mb.statix.generator.strategy;

import java.util.Iterator;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;

public final class Require<I extends SearchState, O extends SearchState> extends SearchStrategy<I, O> {

    private final static ILogger logger = LoggerUtils.logger(Require.class);

    private final SearchStrategy<I, O> s;

    Require(SearchStrategy<I, O> s) {
        this.s = s;
    }

    @Override protected SearchNodes<O> doApply(SearchContext ctx, SearchNode<I> node) {
        final SearchNodes<O> nodes = s.apply(ctx, node);
        final Iterator<SearchNode<O>> it = nodes.nodes().iterator();
        if(!it.hasNext()) {
            logger.error("required node failed: {}", nodes.desc());
            return SearchNodes.failure(node, "require[no results]");
        }
        return SearchNodes.of(node, nodes::desc, nodes.nodes());
    }

    @Override public String toString() {
        return "require(" + s + ")";
    }

}