package mb.statix.random.node;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.metaborg.core.MetaborgException;

import com.google.common.collect.Iterables;

import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CResolveQuery;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.solver.IConstraint;

public class SelectRandomQuery extends SearchNode<SearchState, Tuple2<SearchState, CResolveQuery>> {

    public SelectRandomQuery(Random rnd) {
        super(rnd);
    }

    private Set<CResolveQuery> predicates;
    private Set<IConstraint> otherConstraints;

    @Override protected void doInit() {
        predicates = new HashSet<>();
        otherConstraints = new HashSet<>();
        for(IConstraint c : input.constraints()) {
            if(c instanceof CResolveQuery) {
                predicates.add((CResolveQuery) c);
            } else {
                otherConstraints.add(c);
            }
        }
    }

    @Override protected Optional<Tuple2<SearchState, CResolveQuery>> doNext() throws MetaborgException {
        if(predicates.isEmpty()) {
            return Optional.empty();
        }
        final CResolveQuery predicate = pick(predicates);
        final SearchState newState = input.update(input.state(), Iterables.concat(predicates, otherConstraints));
        return Optional.of(ImmutableTuple2.of(newState, predicate));
    }

    @Override public String toString() {
        return "random-query";
    }

}