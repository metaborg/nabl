package statix.random;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Random;

import org.metaborg.core.MetaborgException;

import com.google.common.collect.ImmutableList;

import mb.statix.solver.IConstraint;
import mb.statix.solver.persistent.State;
import mb.statix.spec.Spec;
import statix.random.node.DropAstPredicates;
import statix.random.node.ExpandPredicate;
import statix.random.node.Infer;
import statix.random.node.SelectRandomPredicate;
import statix.random.node.Seq;

public class RandomTermGenerator {

    private static final int MAX_DEPTH = 4;

    private final int maxDepth;

    private final Random rnd = new Random(System.currentTimeMillis());
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
        final SearchNode<SearchState, SearchState> startNode = infer(rnd);
        startNode.init(state);
        stack.push(startNode);
    }

    public Optional<SearchState> next() throws MetaborgException {
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
            final SearchNode<SearchState, SearchState> expandNode = expand(rnd);
            expandNode.init(state.get());
            stack.push(expandNode);
        }
        return Optional.empty();
    }

    private static boolean done(SearchState state) {
        return state.constraints().isEmpty();
    }

    private static SearchNode<SearchState, SearchState> infer(Random rnd) {
        return new Seq<>(rnd, new Infer(rnd), new DropAstPredicates(rnd));
    }

    private static SearchNode<SearchState, SearchState> expand(Random rnd) {
        return new Seq<>(rnd, new Seq<>(rnd, new SelectRandomPredicate(rnd), new ExpandPredicate(rnd)), infer(rnd));
    }

}