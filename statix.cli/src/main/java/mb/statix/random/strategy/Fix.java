package mb.statix.random.strategy;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

import org.metaborg.core.MetaborgException;
import org.metaborg.util.functions.Predicate1;

import mb.statix.constraints.CUser;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchNodes;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;

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
        final Deque<SearchNodes<SearchState>> stack = new LinkedList<>();
        final SearchNodes<SearchState> initNodes = infer.apply(ctx, input, parent);
        stack.push(initNodes);
        return new SearchNodes<SearchState>() {

            boolean fresh = false;

            @Override public Optional<SearchNode<SearchState>> next() throws MetaborgException, InterruptedException {
                while(!stack.isEmpty()) {
                    final SearchNodes<SearchState> nodes = stack.peek();
                    final SearchNode<SearchState> node;
                    try {
                        if((node = nodes.next().orElse(null)) == null) {
                            stack.pop();
                            if(fresh) {
                                ctx.progress('.');
                            }
                            continue;
                        }
                    } finally {
                        fresh = false;
                    }
                    if(node.output().constraints().stream()
                            .allMatch(c -> (c instanceof CUser && done.test((CUser) c)))) {
                        ctx.progress('+');
                        return Optional.of(node);
                    }
                    final SearchNodes<SearchState> nextNodes =
                            SearchStrategies.seq(search, infer).apply(ctx, node.output(), node);
                    stack.push(nextNodes);
                    fresh = true;
                }
                ctx.progress('\n');
                return Optional.empty();
            }

        };
    }

    @Override public String toString() {
        return "fix(" + search + ", " + infer + ")";
    }

}