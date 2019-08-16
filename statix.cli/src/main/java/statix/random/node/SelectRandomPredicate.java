package statix.random.node;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.metaborg.core.MetaborgException;

import com.google.common.collect.Iterables;

import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CUser;
import mb.statix.solver.IConstraint;
import statix.random.SearchNode;
import statix.random.SearchState;

public class SelectRandomPredicate extends SearchNode<SearchState, Tuple2<SearchState, CUser>> {

    public SelectRandomPredicate(Random rnd) {
        super(rnd);
    }

    private Set<CUser> predicates;
    private Set<IConstraint> otherConstraints;

    @Override protected void doInit() {
        predicates = new HashSet<>();
        otherConstraints = new HashSet<>();
        for(IConstraint c : input.constraints()) {
            if(c instanceof CUser) {
                predicates.add((CUser) c);
            } else {
                otherConstraints.add(c);
            }
        }
    }

    @Override protected Optional<Tuple2<SearchState, CUser>> doNext() throws MetaborgException {
        if(predicates.isEmpty()) {
            return Optional.empty();
        }
        final CUser predicate = pick(predicates);
        final SearchState newState = input.update(input.state(), Iterables.concat(predicates, otherConstraints));
        return Optional.of(ImmutableTuple2.of(newState, predicate));
    }

    @Override public String toString() {
        return "random-goal";
    }

}