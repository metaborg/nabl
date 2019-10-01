package mb.statix.random.strategy;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final SearchStrategy<SearchState, SearchState> searchAndInfer;
    private final Predicate1<CUser> done;
    private final int maxConsecutiveFailures;

    public Fix(SearchStrategy<SearchState, SearchState> search, SearchStrategy<SearchState, SearchState> infer,
            Predicate1<CUser> done, int maxConsecutiveFailures) {
        this.search = search;
        this.infer = infer;
        this.searchAndInfer = SearchStrategies.seq(search).$(infer).$();
        this.done = done;
        this.maxConsecutiveFailures = maxConsecutiveFailures;
    }

    @Override protected SearchNodes<SearchState> doApply(SearchContext ctx, SearchNode<SearchState> node) {
        final AtomicInteger failureCount = new AtomicInteger();
        final Deque<Iterator<SearchNode<SearchState>>> stack = new LinkedList<>();
        final Action1<SearchNodes<SearchState>> push = ns -> {
            Iterator<SearchNode<SearchState>> it = ns.nodes().iterator();
            if(it.hasNext()) {
                stack.push(it);
                failureCount.set(0);
            } else {
                ctx.failure(ns);
                failureCount.incrementAndGet();
            }
        };
        final SearchNodes<SearchState> initNodes = infer.apply(ctx, node);
        push.apply(initNodes);
        final Stream<SearchNode<SearchState>> fixNodes = StreamUtil.generate(() -> {
            while(!stack.isEmpty()) {
                final Iterator<SearchNode<SearchState>> nodes = stack.peek();
                if(!nodes.hasNext()) {
                    stack.pop();
                    continue;
                }
                final SearchNode<SearchState> next = nodes.next();
                if(Streams.stream(next.output().constraintsAndDelays())
                        .allMatch(c -> (c instanceof CUser && done.test((CUser) c)))) {
                    return Optional.of(next);
                }
                final SearchNodes<SearchState> nextNodes = searchAndInfer.apply(ctx, next);
                push.apply(nextNodes);
                if(maxConsecutiveFailures >= 0 && failureCount.get() >= maxConsecutiveFailures) {
                    // we're done here
                    stack.clear();
                }
            }
            return Optional.empty();
        });
        return SearchNodes.of(node, () -> "fix(???)", fixNodes);
    }

    @Override public String toString() {
        return "fix(" + search + ", " + infer + ")";
    }

}