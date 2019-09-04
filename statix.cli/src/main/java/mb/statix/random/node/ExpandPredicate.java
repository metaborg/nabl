package mb.statix.random.node;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.metaborg.core.MetaborgException;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CUser;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.spec.Rule;

public class ExpandPredicate extends SearchNode<Tuple2<SearchState, CUser>, SearchState> {

    private static final ILogger log = LoggerUtils.logger(ExpandPredicate.class);

    public ExpandPredicate(Random rnd) {
        super(rnd);
    }

    private CUser predicate;
    private Set<Rule> rules;

    @Override protected void doInit() {
        this.predicate = input._2();
        this.rules = new HashSet<>(input._1().state().spec().rules().get(predicate.name()));
    }

    @Override protected Optional<SearchState> doNext() throws MetaborgException, InterruptedException {
        if(rules.isEmpty()) {
            return Optional.empty();
        }
        final Rule rule = pick(rules);
        final SearchState searchState = input._1();
        return apply(searchState.state(), rule, predicate.args(), predicate).map(stateAndConstraint -> {
            return searchState.update(stateAndConstraint._1(),
                    Iterables2.cons(stateAndConstraint._2(), searchState.constraints()));
        });
    }

    @Override public String toString() {
        return "expand-goal-rule";
    }

}