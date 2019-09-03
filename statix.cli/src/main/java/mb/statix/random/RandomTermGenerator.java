package mb.statix.random;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Random;

import org.metaborg.core.MetaborgException;

import com.google.common.collect.ImmutableList;

import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.random.node.SearchNodes;
import mb.statix.solver.IConstraint;
import mb.statix.solver.persistent.State;
import mb.statix.spec.Spec;

public class RandomTermGenerator {

    private static final int MAX_DEPTH = 4;

    private final int maxDepth;

    private final SearchNodes N = new SearchNodes(new Random(System.currentTimeMillis()));
    private final Deque<SearchNode<SearchState, SearchState>> stack = new LinkedList<>();

    public RandomTermGenerator(Spec spec, IConstraint constraint) {
        this(spec, constraint, MAX_DEPTH);
    }

    public RandomTermGenerator(Spec spec, IConstraint constraint, int maxDepth) {
        this.maxDepth = maxDepth;
        initStack(spec, constraint);
    }

    private void initStack(Spec spec, IConstraint constraint) {
        final SearchState state = SearchState.of(State.of(spec), ImmutableList.of(constraint));
        final SearchNode<SearchState, SearchState> startNode = infer();
        startNode.init(state);
        stack.push(startNode);
    }

    public Optional<SearchState> next() throws MetaborgException, InterruptedException {
        while(!stack.isEmpty()) {
            Optional<SearchState> state = stack.peek().next();
            if(!state.isPresent()) {
                stack.pop();
                continue;
            }
            if(done(state.get())) {
                return state;
            }
            final int depth = stack.size() - 1;
            if(depth >= maxDepth) {
                continue;
            }
            final SearchNode<SearchState, SearchState> searchNode = search();
            searchNode.init(state.get());
            stack.push(searchNode);
        }
        return Optional.empty();
    }

    private static boolean done(SearchState state) {
        return state.constraints().isEmpty();
    }

    private SearchNode<SearchState, SearchState> infer() {
        return N.seq(N.infer(), dropAst());
    }

    private SearchNode<SearchState, SearchState> dropAst() {
        return N.drop(CAstId.class, CAstProperty.class);
    }

    private SearchNode<SearchState, SearchState> search() {
        // @formatter:off
        return N.seq(
            N.alt(
                N.seq(N.selectPredicate(), N.expandPredicate()), 
                N.seq(N.selectQuery(), N.resolveQuery())
            ),
            infer()
        );
        // @formatter:on
    }

}