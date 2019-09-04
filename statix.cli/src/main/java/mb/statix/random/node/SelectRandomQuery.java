package mb.statix.random.node;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.metaborg.core.MetaborgException;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.ImmutableTuple3;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.statix.constraints.CResolveQuery;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.random.util.ElementSelectorSet;
import mb.statix.random.util.ElementSelectorSet.Entry;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;

public class SelectRandomQuery
        extends SearchNode<SearchState, Tuple2<SearchState, Tuple3<CResolveQuery, Scope, Boolean>>> {

    private static final ILogger log = LoggerUtils.logger(SelectRandomQuery.class);

    public SelectRandomQuery(Random rnd) {
        super(rnd);
    }

    private Iterator<Entry<Tuple3<CResolveQuery, Scope, Boolean>>> queries;
    private List<IConstraint> otherConstraints;

    @Override protected void doInit() {
        final Set.Transient<Tuple3<CResolveQuery, Scope, Boolean>> queries = Set.Transient.of();
        final ImmutableList.Builder<IConstraint> otherConstraints = ImmutableList.builder();
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
                    queries.__insert(ImmutableTuple3.of(q, scope.get(), isAlways.get()));
                } else {
                    otherConstraints.add(q);
                }
            } else {
                otherConstraints.add(c);
            }
        }
        this.queries = new ElementSelectorSet<>(queries.freeze()).iterator();
        this.otherConstraints = otherConstraints.build();
    }

    @Override protected Optional<Tuple2<SearchState, Tuple3<CResolveQuery, Scope, Boolean>>> doNext()
            throws MetaborgException, InterruptedException {
        if(!queries.hasNext()) {
            return Optional.empty();
        }
        final Entry<Tuple3<CResolveQuery, Scope, Boolean>> entry = queries.next();
        final Iterable<CResolveQuery> otherQueries = entry.getOthers().stream().map(Tuple3::_1)::iterator;
        final SearchState newState = input.update(input.state(), Iterables.concat(otherQueries, otherConstraints));
        return Optional.of(ImmutableTuple2.of(newState, entry.getFocus()));
    }

    @Override public String toString() {
        return "random-query";
    }

}