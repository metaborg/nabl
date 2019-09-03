package mb.statix.random.node;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.core.MetaborgException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.solver.IConstraint;

public class DropPredicates extends SearchNode<SearchState, SearchState> {

    private final Set<Class<? extends IConstraint>> classes;

    @SafeVarargs public DropPredicates(Random rnd, Class<? extends IConstraint>... classes) {
        super(rnd);
        this.classes = ImmutableSet.copyOf(classes);
    }

    private List<IConstraint> constraints;
    private final AtomicBoolean fired = new AtomicBoolean();

    @Override protected void doInit() {
        // @formatter:off
        constraints = input.constraints().stream()
                .filter(c -> !classes.contains(c.getClass()))
                .collect(ImmutableList.toImmutableList());
        // @formatter:on
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
        return "drop-constraints";
    }

}