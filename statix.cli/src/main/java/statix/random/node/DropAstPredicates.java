package statix.random.node;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.core.MetaborgException;

import com.google.common.collect.ImmutableList;

import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.solver.IConstraint;
import statix.random.SearchNode;
import statix.random.SearchState;

public class DropAstPredicates extends SearchNode<SearchState, SearchState> {

    public DropAstPredicates(Random rnd) {
        super(rnd);
    }

    private List<IConstraint> constraints;
    private final AtomicBoolean fired = new AtomicBoolean();

    @Override protected void doInit() {
        constraints = input.constraints().stream().filter(c -> !(c instanceof CAstId || c instanceof CAstProperty))
                .collect(ImmutableList.toImmutableList());
        fired.set(false);
    }

    @Override protected Optional<SearchState> doNext() throws MetaborgException {
        if(fired.getAndSet(true)) {
            return Optional.empty();
        }
        final SearchState newState = input.update(input.state(), constraints);
        return Optional.of(newState);
    }

    @Override public String toString() {
        return "drop-ast-constraints";
    }

}