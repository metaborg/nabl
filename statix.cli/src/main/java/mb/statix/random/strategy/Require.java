package mb.statix.random.strategy;

import java.util.Iterator;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Streams;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class Require<I, O> extends SearchStrategy<I, O> {

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
        return SearchNodes.of(node, nodes::desc, Streams.stream(it));
    }

    @Override public String toString() {
        return "require(" + s + ")";
    }

}