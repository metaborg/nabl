package mb.statix.random.strategy;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Stream;

import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.Streams;

import mb.statix.constraints.CUser;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.random.util.StreamUtil;

public class Fix extends SearchStrategy<SearchState, SearchState> {

    private final SearchStrategy<SearchState, SearchState> search;
    private final SearchStrategy<SearchState, SearchState> infer;
    private final Predicate1<CUser> done;

    public Fix(SearchStrategy<SearchState, SearchState> search, SearchStrategy<SearchState, SearchState> infer,
            Predicate1<CUser> done) {
        this.search = search;
        this.infer = infer;
        this.done = done;
    }

    @Override protected SearchNodes<SearchState> doApply(SearchContext ctx, SearchState input, SearchNode<?> parent) {
        final Deque<Iterator<SearchNode<SearchState>>> stack = new LinkedList<>();
        final Action1<SearchNodes<SearchState>> push = ns -> {
            if(!ns.success()) {
                ctx.failure(ns);
            } else {
                stack.push(ns.nodes().iterator());
            }
        };
        final SearchNodes<SearchState> initNodes = infer.apply(ctx, input, parent);
        push.apply(initNodes);
        final Stream<SearchNode<SearchState>> fixNodes = StreamUtil.generate(() -> {
            while(!stack.isEmpty()) {
                final Iterator<SearchNode<SearchState>> nodes = stack.peek();
                if(!nodes.hasNext()) {
                    stack.pop();
                    continue;
                }
                final SearchNode<SearchState> node = nodes.next();
                if(Streams.stream(node.output().constraintsAndDelays())
                        .allMatch(c -> (c instanceof CUser && done.test((CUser) c)))) {
                    return Optional.of(node);
                }
                final SearchNodes<SearchState> nextNodes =
                        SearchStrategies.seq(search, infer).apply(ctx, node.output(), node);
                push.apply(nextNodes);
            }
            return Optional.empty();
        });
        return SearchNodes.of(parent, fixNodes);
    }

    @Override public String toString() {
        return "fix(" + search + ", " + infer + ")";
    }

}