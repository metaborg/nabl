package mb.statix.random.node;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.metaborg.core.MetaborgException;

import com.google.common.collect.Iterables;

import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.ImmutableTuple3;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.statix.constraints.CResolveQuery;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;

public class SelectRandomQuery
        extends SearchNode<SearchState, Tuple2<SearchState, Tuple3<CResolveQuery, Scope, Boolean>>> {

    public SelectRandomQuery(Random rnd) {
        super(rnd);
    }

    private Set<Tuple3<CResolveQuery, Scope, Boolean>> freeQueries;
    private Set<CResolveQuery> pickedQueries;
    private Set<IConstraint> otherConstraints;

    @Override protected void doInit() {
        freeQueries = new HashSet<>();
        pickedQueries = new HashSet<>();
        otherConstraints = new HashSet<>();
        for(IConstraint c : input.constraints()) {
            if(c instanceof CResolveQuery) {
                final CResolveQuery q = (CResolveQuery) c;
                final Optional<Boolean> isAlways;
                try {
                    isAlways = q.min().getDataEquiv().isAlways(input.state().spec());
                } catch(InterruptedException e) {
                    throw new RuntimeException(e); // FIXME
                }
                final Optional<Scope> scope = Scope.matcher().match(q.scopeTerm(), input.state().unifier());
                if(scope.isPresent() && isAlways.isPresent()) {
                    freeQueries.add(ImmutableTuple3.of(q, scope.get(), isAlways.get()));
                } else {
                    otherConstraints.add(q);
                }
            } else {
                otherConstraints.add(c);
            }
        }
    }

    @Override protected Optional<Tuple2<SearchState, Tuple3<CResolveQuery, Scope, Boolean>>> doNext()
            throws MetaborgException {
        if(freeQueries.isEmpty()) {
            return Optional.empty();
        }
        final Tuple3<CResolveQuery, Scope, Boolean> predicate = pick(freeQueries);
        final Iterable<CResolveQuery> freeQueries = this.freeQueries.stream().map(Tuple3::_1)::iterator;
        final SearchState newState =
                input.update(input.state(), Iterables.concat(freeQueries, pickedQueries, otherConstraints));
        pickedQueries.add(predicate._1());
        return Optional.of(ImmutableTuple2.of(newState, predicate));
    }

    @Override public String toString() {
        return "random-query";
    }

}