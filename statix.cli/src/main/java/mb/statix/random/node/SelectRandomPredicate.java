package mb.statix.random.node;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.metaborg.core.MetaborgException;
import org.metaborg.util.iterators.Iterables2;

import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CUser;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.solver.IConstraint;

public class SelectRandomPredicate extends SearchNode<SearchState, Tuple2<SearchState, CUser>> {

    public SelectRandomPredicate(Random rnd) {
        super(rnd);
    }

    private Set<CUser> freePredicates;
    private Set<CUser> pickedPredicates;
    private Set<IConstraint> otherConstraints;

    @Override protected void doInit() {
        freePredicates = new HashSet<>();
        pickedPredicates = new HashSet<>();
        otherConstraints = new HashSet<>();
        for(IConstraint c : input.constraints()) {
            if(c instanceof CUser) {
                freePredicates.add((CUser) c);
            } else {
                otherConstraints.add(c);
            }
        }
    }

    @Override protected Optional<Tuple2<SearchState, CUser>> doNext() throws MetaborgException {
        if(freePredicates.isEmpty()) {
            return Optional.empty();
        }
        final CUser predicate = pick(freePredicates);
        final SearchState newState =
                input.update(input.state(), Iterables2.fromConcat(freePredicates, pickedPredicates, otherConstraints));
        pickedPredicates.add(predicate);
        return Optional.of(ImmutableTuple2.of(newState, predicate));
    }

    @Override public String toString() {
        return "random-goal";
    }

}