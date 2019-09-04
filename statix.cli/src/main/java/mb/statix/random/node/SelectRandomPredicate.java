package mb.statix.random.node;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.metaborg.core.MetaborgException;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CUser;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.random.util.ElementSelectorSet;
import mb.statix.random.util.ElementSelectorSet.Entry;
import mb.statix.solver.IConstraint;

public class SelectRandomPredicate extends SearchNode<SearchState, Tuple2<SearchState, CUser>> {

    private static final ILogger log = LoggerUtils.logger(SelectRandomPredicate.class);

    public SelectRandomPredicate(Random rnd) {
        super(rnd);
    }

    private Iterator<Entry<CUser>> predicates;
    private List<IConstraint> otherConstraints;

    @Override protected void doInit() {
        this.predicates = new ElementSelectorSet<>(
                input.constraints().stream().filter(c -> c instanceof CUser).map(c -> (CUser) c)::iterator).iterator();
        otherConstraints = input.constraints().stream().filter(c -> !(c instanceof CUser))
                .collect(ImmutableList.toImmutableList());
    }

    @Override protected Optional<Tuple2<SearchState, CUser>> doNext() throws MetaborgException, InterruptedException {
        if(!predicates.hasNext()) {
            return Optional.empty();
        }
        final Entry<CUser> entry = predicates.next();
        final SearchState newState =
                input.update(input.state(), Iterables2.fromConcat(entry.getOthers(), otherConstraints));
        return Optional.of(ImmutableTuple2.of(newState, entry.getFocus()));
    }

    @Override public String toString() {
        return "random-goal";
    }

}